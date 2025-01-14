// Copyright 2019 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "android_webview/browser/network_service/aw_url_loader_throttle.h"

#include "android_webview/browser/aw_resource_context.h"
#include "android_webview/common/aw_features.h"
#include "base/feature_list.h"
#include "base/metrics/histogram_macros.h"
#include "base/strings/string_util.h"
#include "net/base/registry_controlled_domains/registry_controlled_domain.h"
#include "net/http/http_request_headers.h"
#include "net/url_request/redirect_info.h"
#include "services/network/public/cpp/resource_request.h"

namespace android_webview {

namespace {

// These values are logged to UMA. Entries should not be renumbered and
// numeric values should never be reused. Please keep in sync with
// "WebViewExtraHeadersRedirect" in src/tools/metrics/histograms/enums.xml.
enum class ExtraHeadersRedirect {
  kSameOrigin = 0,
  kSameDomain = 1,
  kCrossDomain = 2,
  kMaxValue = kCrossDomain
};

void RecordExtraHeadersRedirectUMA(ExtraHeadersRedirect value) {
  UMA_HISTOGRAM_ENUMERATION("Android.WebView.ExtraHeadersRedirect", value);
}

}  // namespace

AwURLLoaderThrottle::AwURLLoaderThrottle(AwResourceContext* aw_resource_context)
    : aw_resource_context_(aw_resource_context) {}

AwURLLoaderThrottle::~AwURLLoaderThrottle() = default;

void AwURLLoaderThrottle::WillStartRequest(network::ResourceRequest* request,
                                           bool* defer) {
  AddExtraHeadersIfNeeded(request->url, &request->headers);
  if (!added_headers_.empty()) {
    original_origin_ = url::Origin::Create(request->url);
  }
}

void AwURLLoaderThrottle::WillRedirectRequest(
    net::RedirectInfo* redirect_info,
    const network::mojom::URLResponseHead& response_head,
    bool* defer,
    std::vector<std::string>* to_be_removed_request_headers,
    net::HttpRequestHeaders* modified_request_headers,
    net::HttpRequestHeaders* modified_cors_exempt_request_headers) {
  bool same_origin_only = base::FeatureList::IsEnabled(
      features::kWebViewExtraHeadersSameOriginOnly);
  bool same_domain_only = base::FeatureList::IsEnabled(
      features::kWebViewExtraHeadersSameDomainOnly);

  if (!added_headers_.empty()) {
    bool is_same_origin =
        original_origin_.CanBeDerivedFrom(redirect_info->new_url);
    bool is_same_domain = net::registry_controlled_domains::SameDomainOrHost(
        redirect_info->new_url, original_origin_,
        net::registry_controlled_domains::INCLUDE_PRIVATE_REGISTRIES);

    if (is_same_origin) {
      RecordExtraHeadersRedirectUMA(ExtraHeadersRedirect::kSameOrigin);
    } else if (is_same_domain) {
      RecordExtraHeadersRedirectUMA(ExtraHeadersRedirect::kSameDomain);
    } else {
      RecordExtraHeadersRedirectUMA(ExtraHeadersRedirect::kCrossDomain);
    }

    if ((same_origin_only && !is_same_origin) ||
        (same_domain_only && !is_same_domain)) {
      // The headers we added must be removed.
      to_be_removed_request_headers->insert(
          to_be_removed_request_headers->end(),
          std::make_move_iterator(added_headers_.begin()),
          std::make_move_iterator(added_headers_.end()));
      added_headers_.clear();
    }
  }

  if (!same_origin_only && !same_domain_only) {
    // The original behaviour added more headers if the redirect target had
    // previously been loaded with extra headers; this is weird/surprising, so
    // it's skipped when either feature is enabled.
    AddExtraHeadersIfNeeded(redirect_info->new_url, modified_request_headers);
  }
}

void AwURLLoaderThrottle::AddExtraHeadersIfNeeded(
    const GURL& url,
    net::HttpRequestHeaders* headers) {
  std::string extra_headers = aw_resource_context_->GetExtraHeaders(url);
  if (extra_headers.empty())
    return;

  net::HttpRequestHeaders temp_headers;
  temp_headers.AddHeadersFromString(extra_headers);
  for (net::HttpRequestHeaders::Iterator it(temp_headers); it.GetNext();) {
    if (headers->HasHeader(it.name()))
      continue;

    headers->SetHeader(it.name(), it.value());
    added_headers_.push_back(it.name());
  }
}

}  // namespace android_webview
