// Copyright 2020 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "base/strings/sys_string_conversions.h"
#include "base/test/scoped_feature_list.h"
#include "components/password_manager/core/common/password_manager_features.h"
#include "components/strings/grit/components_strings.h"
#import "ios/chrome/browser/ui/passwords/password_breach_app_interface.h"
#import "ios/chrome/browser/ui/passwords/password_breach_constants.h"
#import "ios/chrome/browser/ui/settings/password/passwords_table_view_constants.h"
#import "ios/chrome/test/earl_grey/chrome_earl_grey.h"
#import "ios/chrome/test/earl_grey/chrome_matchers.h"
#import "ios/chrome/test/earl_grey/chrome_test_case.h"
#import "ios/testing/earl_grey/app_launch_manager.h"
#import "ios/testing/earl_grey/earl_grey_test.h"
#include "testing/gtest/include/gtest/gtest.h"
#include "ui/base/l10n/l10n_util.h"

#if !defined(__has_feature) || !__has_feature(objc_arc)
#error "This file requires ARC support."
#endif

// TODO(crbug.com/1015113): The EG2 macro is breaking indexing for some reason
// without the trailing semicolon.  For now, disable the extra semi warning
// so Xcode indexing works for the egtest.
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wc++98-compat-extra-semi"
GREY_STUB_CLASS_IN_APP_MAIN_QUEUE(PasswordBreachAppInterface);
#pragma clang diagnostic pop

namespace {

using chrome_test_util::ButtonWithAccessibilityLabel;

id<GREYMatcher> PasswordBreachMatcher() {
  return grey_accessibilityID(kPasswordBreachViewAccessibilityIdentifier);
}

id<GREYMatcher> PasswordListMatcher() {
  return grey_accessibilityID(kPasswordsTableViewId);
}

id<GREYMatcher> CheckPasswordButton() {
  return grey_allOf(ButtonWithAccessibilityLabel(base::SysUTF16ToNSString(
                        l10n_util::GetStringUTF16(IDS_LEAK_CHECK_CREDENTIALS))),
                    grey_interactable(), nullptr);
}

}  // namespace

@interface PasswordBreachTestCase : ChromeTestCase
@end

@implementation PasswordBreachTestCase

#pragma mark - Tests

- (void)testPasswordBreachIsPresented {
  [PasswordBreachAppInterface showPasswordBreachWithCheckButton:NO];
  [[EarlGrey selectElementWithMatcher:PasswordBreachMatcher()]
      assertWithMatcher:grey_notNil()];
}

// Tests that Check password button redirects to the Passwords List.
- (void)testPasswordBreachRedirectToPasswords {
  // TODO(crbug.com/1096986): Remove it once feature is enabled by default.
  AppLaunchConfiguration config;
  config.features_enabled.push_back(password_manager::features::kPasswordCheck);
  [[AppLaunchManager sharedManager] ensureAppLaunchedWithConfiguration:config];

  [PasswordBreachAppInterface showPasswordBreachWithCheckButton:YES];
  [[EarlGrey selectElementWithMatcher:PasswordBreachMatcher()]
      assertWithMatcher:grey_notNil()];

  [[EarlGrey selectElementWithMatcher:CheckPasswordButton()]
      performAction:grey_tap()];

  [[EarlGrey selectElementWithMatcher:PasswordListMatcher()]
      assertWithMatcher:grey_notNil()];
}

@end
