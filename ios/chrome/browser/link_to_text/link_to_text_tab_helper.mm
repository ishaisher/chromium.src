// Copyright 2020 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#import "ios/chrome/browser/link_to_text/link_to_text_tab_helper.h"

#import "base/bind.h"
#import "base/optional.h"
#import "base/values.h"
#import "ios/web/public/js_messaging/web_frame.h"
#import "ios/web/public/js_messaging/web_frames_manager.h"
#import "ios/web/public/ui/crw_web_view_proxy.h"
#import "ios/web/public/ui/crw_web_view_scroll_view_proxy.h"
#import "url/gurl.h"

#if !defined(__has_feature) || !__has_feature(objc_arc)
#error "This file requires ARC support."
#endif

namespace {
const double kJavaScriptFunctionCallTimeoutMs = 200.0;
const char kGetLinkToTextJavaScript[] = "linkToText.getLinkToText";
}  // namespace

LinkToTextTabHelper::LinkToTextTabHelper(web::WebState* web_state)
    : web_state_(web_state), weak_ptr_factory_(this) {
  web_state_->AddObserver(this);
}

LinkToTextTabHelper::~LinkToTextTabHelper() {}

// static
void LinkToTextTabHelper::CreateForWebState(web::WebState* web_state) {
  DCHECK(web_state);
  if (!FromWebState(web_state)) {
    web_state->SetUserData(
        UserDataKey(), base::WrapUnique(new LinkToTextTabHelper(web_state)));
  }
}

bool LinkToTextTabHelper::ShouldOffer() {
  // TODO(crbug.com/1134708): add more checks, like text only.
  return true;
}

void LinkToTextTabHelper::GetLinkToText(LinkToTextCallback callback) {
  base::WeakPtr<LinkToTextTabHelper> weak_ptr = weak_ptr_factory_.GetWeakPtr();
  web_state_->GetWebFramesManager()->GetMainWebFrame()->CallJavaScriptFunction(
      kGetLinkToTextJavaScript, {},
      base::BindOnce(^(const base::Value* response) {
        if (weak_ptr) {
          weak_ptr->OnJavaScriptResponseReceived(callback, response);
        }
      }),
      base::TimeDelta::FromMilliseconds(kJavaScriptFunctionCallTimeoutMs));
}

void LinkToTextTabHelper::OnJavaScriptResponseReceived(
    LinkToTextCallback callback,
    const base::Value* response) {
  if (callback) {
    callback([LinkToTextResponse createFromValue:response webState:web_state_]);
  }
}

void LinkToTextTabHelper::WebStateDestroyed(web::WebState* web_state) {
  DCHECK_EQ(web_state_, web_state);

  web_state_->RemoveObserver(this);
  web_state_ = nil;

  // The call to RemoveUserData cause the destruction of the current instance,
  // so nothing should be done after that point (this is like "delete this;").
  web_state->RemoveUserData(UserDataKey());
}

WEB_STATE_USER_DATA_KEY_IMPL(LinkToTextTabHelper)
