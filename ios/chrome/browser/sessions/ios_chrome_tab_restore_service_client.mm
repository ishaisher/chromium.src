// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "ios/chrome/browser/sessions/ios_chrome_tab_restore_service_client.h"

#include "base/bind.h"
#include "base/callback.h"
#include "components/sessions/ios/ios_live_tab.h"
#include "components/tab_groups/tab_group_id.h"
#include "ios/chrome/browser/application_context.h"
#include "ios/chrome/browser/browser_state/chrome_browser_state.h"
#include "ios/chrome/browser/browser_state/chrome_browser_state_manager.h"
#include "ios/chrome/browser/chrome_url_constants.h"
#include "ios/chrome/browser/main/browser.h"
#include "ios/chrome/browser/main/browser_list.h"
#include "ios/chrome/browser/main/browser_list_factory.h"
#include "ios/chrome/browser/sessions/live_tab_context_browser_agent.h"
#include "ios/chrome/browser/tabs/synced_window_delegate_browser_agent.h"
#import "ios/chrome/browser/web_state_list/web_state_list.h"
#include "ios/public/provider/chrome/browser/chrome_browser_provider.h"
#include "url/gurl.h"

#if !defined(__has_feature) || !__has_feature(objc_arc)
#error "This file requires ARC support."
#endif

namespace {
sessions::LiveTabContext* FindLiveTabContextWithCondition(
    base::RepeatingCallback<bool(Browser*)> condition) {
  std::vector<ChromeBrowserState*> browser_states =
      GetApplicationContext()
          ->GetChromeBrowserStateManager()
          ->GetLoadedBrowserStates();

  for (ChromeBrowserState* browser_state : browser_states) {
    DCHECK(!browser_state->IsOffTheRecord());
    BrowserList* browsers =
        BrowserListFactory::GetForBrowserState(browser_state);
    for (Browser* browser : browsers->AllRegularBrowsers()) {
      if (condition.Run(browser)) {
        return LiveTabContextBrowserAgent::FromBrowser(browser);
      }
    }

    for (Browser* browser : browsers->AllIncognitoBrowsers()) {
      if (condition.Run(browser)) {
        return LiveTabContextBrowserAgent::FromBrowser(browser);
      }
    }
  }

  return nullptr;
}
}  // namespace

IOSChromeTabRestoreServiceClient::IOSChromeTabRestoreServiceClient(
    ChromeBrowserState* browser_state)
    : browser_state_(browser_state) {}

IOSChromeTabRestoreServiceClient::~IOSChromeTabRestoreServiceClient() {}

sessions::LiveTabContext*
IOSChromeTabRestoreServiceClient::CreateLiveTabContext(
    const std::string& /* app_name */,
    const gfx::Rect& /* bounds */,
    ui::WindowShowState /* show_state */,
    const std::string& /* workspace */,
    const std::string& /* user_title */) {
  NOTREACHED() << "Tab restore service attempting to create a new window.";
  return nullptr;
}

sessions::LiveTabContext*
IOSChromeTabRestoreServiceClient::FindLiveTabContextForTab(
    const sessions::LiveTab* tab) {
  const sessions::IOSLiveTab* requested_tab =
      static_cast<const sessions::IOSLiveTab*>(tab);
  const web::WebState* web_state = requested_tab->GetWebState();
  if (!web_state) {
    return nullptr;
  }
  return FindLiveTabContextWithCondition(base::Bind(
      [](const web::WebState* web_state, Browser* browser) {
        WebStateList* web_state_list = browser->GetWebStateList();
        const int index = web_state_list->GetIndexOfWebState(web_state);
        return index != WebStateList::kInvalidIndex;
      },
      web_state));
}

sessions::LiveTabContext*
IOSChromeTabRestoreServiceClient::FindLiveTabContextWithID(
    SessionID desired_id) {
  return FindLiveTabContextWithCondition(base::Bind(
      [](SessionID desired_id, Browser* browser) {
        SyncedWindowDelegateBrowserAgent* syncedWindowDelegate =
            SyncedWindowDelegateBrowserAgent::FromBrowser(browser);
        return syncedWindowDelegate->GetSessionId() == desired_id;
      },
      desired_id));
}

sessions::LiveTabContext*
IOSChromeTabRestoreServiceClient::FindLiveTabContextWithGroup(
    tab_groups::TabGroupId group) {
  return nullptr;
}

bool IOSChromeTabRestoreServiceClient::ShouldTrackURLForRestore(
    const GURL& url) {
  // NOTE: In the //chrome client, chrome://quit and chrome://restart are
  // blocked from being tracked to avoid entering infinite loops. However,
  // iOS intentionally does not support those URLs, so there is no need to
  // block them here.
  return url.is_valid();
}

std::string IOSChromeTabRestoreServiceClient::GetExtensionAppIDForTab(
    sessions::LiveTab* tab) {
  return std::string();
}

base::FilePath IOSChromeTabRestoreServiceClient::GetPathToSaveTo() {
  // Note that this will return a different path in incognito from normal mode.
  // In this case, that shouldn't be an issue because the tab restore service
  // is not used in incognito mode.
  return browser_state_->GetStatePath();
}

GURL IOSChromeTabRestoreServiceClient::GetNewTabURL() {
  return GURL(kChromeUINewTabURL);
}

bool IOSChromeTabRestoreServiceClient::HasLastSession() {
  return false;
}

void IOSChromeTabRestoreServiceClient::GetLastSession(
    sessions::GetLastSessionCallback callback) {
  NOTREACHED();
}
