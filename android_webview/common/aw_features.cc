// Copyright 2018 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "android_webview/common/aw_features.h"

namespace android_webview {
namespace features {

// Alphabetical:

// Enable brotli compression support in WebView.
const base::Feature kWebViewBrotliSupport{"WebViewBrotliSupport",
                                          base::FEATURE_DISABLED_BY_DEFAULT};

// Use the SafeBrowsingApiHandler which uses the connectionless GMS APIs. This
// Feature is checked and used in downstream internal code.
const base::Feature kWebViewConnectionlessSafeBrowsing{
    "WebViewConnectionlessSafeBrowsing", base::FEATURE_DISABLED_BY_DEFAULT};

// Restricts WebView child processes to use only LITTLE cores on big.LITTLE
// architectures.
const base::Feature kWebViewCpuAffinityRestrictToLittleCores{
    "WebViewCpuAffinityRestrictToLittleCores",
    base::FEATURE_DISABLED_BY_DEFAULT};

// Enable display cutout support for Android P and above.
const base::Feature kWebViewDisplayCutout{"WebViewDisplayCutout",
                                          base::FEATURE_DISABLED_BY_DEFAULT};

// Only allow extra headers added via loadUrl() to be sent to the original
// domain (eTLD+1); strip them from the request if a cross-domain redirect
// occurs. kWebViewExtraHeadersSameOriginOnly is stricter; when that's enabled,
// this feature has no effect.
const base::Feature kWebViewExtraHeadersSameDomainOnly{
    "WebViewExtraHeadersSameDomainOnly", base::FEATURE_DISABLED_BY_DEFAULT};

// Only allow extra headers added via loadUrl() to be sent to the original
// origin; strip them from the request if a cross-origin redirect occurs.
// When this is enabled, kWebViewExtraHeadersSameDomainOnly has no effect.
const base::Feature kWebViewExtraHeadersSameOriginOnly{
    "WebViewExtraHeadersSameOriginOnly", base::FEATURE_DISABLED_BY_DEFAULT};

// A Feature used for WebView variations tests. Not used in production.
const base::Feature kWebViewTestFeature{"WebViewTestFeature",
                                        base::FEATURE_DISABLED_BY_DEFAULT};

// Enable raster in wide color gamut for apps that use webview in a wide color
// gamut activity.
const base::Feature kWebViewWideColorGamutSupport{
    "WebViewWideColorGamutSupport", base::FEATURE_ENABLED_BY_DEFAULT};

}  // namespace features
}  // namespace android_webview
