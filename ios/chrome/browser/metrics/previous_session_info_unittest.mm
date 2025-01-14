// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "ios/chrome/browser/metrics/previous_session_info.h"

#include "base/strings/sys_string_conversions.h"
#include "components/version_info/version_info.h"
#include "ios/chrome/browser/metrics/previous_session_info_private.h"
#include "testing/gtest/include/gtest/gtest.h"
#include "testing/gtest_mac.h"
#include "testing/platform_test.h"

#if !defined(__has_feature) || !__has_feature(objc_arc)
#error "This file requires ARC support."
#endif

using previous_session_info_constants::kPreviousSessionInfoRestoringSession;
using previous_session_info_constants::
    kPreviousSessionInfoConnectedSceneSessionIDs;

namespace {

// Key in the UserDefaults for a boolean value keeping track of memory warnings.
NSString* const kDidSeeMemoryWarningShortlyBeforeTerminating =
    previous_session_info_constants::
        kDidSeeMemoryWarningShortlyBeforeTerminating;

// Key in the NSUserDefaults for a string value that stores the version of the
// last session.
NSString* const kLastRanVersion = @"LastRanVersion";
// Key in the NSUserDefaults for a string value that stores the language of the
// last session.
NSString* const kLastRanLanguage = @"LastRanLanguage";

// IDs to be used for testing scene sessions.
NSString* const kTestSession1ID = @"test_session_1";
NSString* const kTestSession2ID = @"test_session_2";
NSString* const kTestSession3ID = @"test_session_3";

using PreviousSessionInfoTest = PlatformTest;

TEST_F(PreviousSessionInfoTest, InitializationWithEmptyDefaults) {
  [PreviousSessionInfo resetSharedInstanceForTesting];
  NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];
  [defaults removeObjectForKey:kDidSeeMemoryWarningShortlyBeforeTerminating];
  [defaults removeObjectForKey:kLastRanVersion];
  [defaults removeObjectForKey:kLastRanLanguage];

  // Instantiate the PreviousSessionInfo sharedInstance.
  PreviousSessionInfo* sharedInstance = [PreviousSessionInfo sharedInstance];

  // Checks the default values.
  EXPECT_FALSE([sharedInstance didSeeMemoryWarningShortlyBeforeTerminating]);
  EXPECT_TRUE([sharedInstance isFirstSessionAfterUpgrade]);
  EXPECT_TRUE([sharedInstance isFirstSessionAfterLanguageChange]);
}

TEST_F(PreviousSessionInfoTest, InitializationWithSameLanguage) {
  [PreviousSessionInfo resetSharedInstanceForTesting];
  NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];
  [defaults removeObjectForKey:kLastRanLanguage];

  // Set the current language as the last ran language.
  NSString* currentVersion = [[NSLocale preferredLanguages] objectAtIndex:0];
  [defaults setObject:currentVersion forKey:kLastRanVersion];

  // Instantiate the PreviousSessionInfo sharedInstance.
  PreviousSessionInfo* sharedInstance = [PreviousSessionInfo sharedInstance];

  // Checks the values.
  EXPECT_TRUE([sharedInstance isFirstSessionAfterLanguageChange]);
}

TEST_F(PreviousSessionInfoTest, InitializationWithDifferentLanguage) {
  [PreviousSessionInfo resetSharedInstanceForTesting];
  NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];
  [defaults removeObjectForKey:kLastRanLanguage];

  // Set the current language as the last ran language.
  NSString* currentVersion = @"Fake Language";
  [defaults setObject:currentVersion forKey:kLastRanVersion];

  // Instantiate the PreviousSessionInfo sharedInstance.
  PreviousSessionInfo* sharedInstance = [PreviousSessionInfo sharedInstance];

  // Checks the values.
  EXPECT_TRUE([sharedInstance isFirstSessionAfterLanguageChange]);
}

TEST_F(PreviousSessionInfoTest, InitializationWithSameVersionNoMemoryWarning) {
  [PreviousSessionInfo resetSharedInstanceForTesting];
  NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];
  [defaults removeObjectForKey:kDidSeeMemoryWarningShortlyBeforeTerminating];
  [defaults removeObjectForKey:kLastRanVersion];

  // Set the current version as the last ran version.
  NSString* currentVersion =
      base::SysUTF8ToNSString(version_info::GetVersionNumber());
  [defaults setObject:currentVersion forKey:kLastRanVersion];

  // Instantiate the PreviousSessionInfo sharedInstance.
  PreviousSessionInfo* sharedInstance = [PreviousSessionInfo sharedInstance];

  // Checks the values.
  EXPECT_FALSE([sharedInstance didSeeMemoryWarningShortlyBeforeTerminating]);
  EXPECT_FALSE([sharedInstance isFirstSessionAfterUpgrade]);
}

TEST_F(PreviousSessionInfoTest, InitializationWithSameVersionMemoryWarning) {
  [PreviousSessionInfo resetSharedInstanceForTesting];
  NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];
  [defaults removeObjectForKey:kDidSeeMemoryWarningShortlyBeforeTerminating];
  [defaults removeObjectForKey:kLastRanVersion];

  // Set the current version as the last ran version.
  NSString* currentVersion =
      base::SysUTF8ToNSString(version_info::GetVersionNumber());
  [defaults setObject:currentVersion forKey:kLastRanVersion];

  // Set the memory warning flag as a previous session would have.
  [defaults setBool:YES forKey:kDidSeeMemoryWarningShortlyBeforeTerminating];

  // Instantiate the PreviousSessionInfo sharedInstance.
  PreviousSessionInfo* sharedInstance = [PreviousSessionInfo sharedInstance];

  // Checks the values.
  EXPECT_TRUE([sharedInstance didSeeMemoryWarningShortlyBeforeTerminating]);
  EXPECT_FALSE([sharedInstance isFirstSessionAfterUpgrade]);
}

TEST_F(PreviousSessionInfoTest, InitializationDifferentVersionNoMemoryWarning) {
  [PreviousSessionInfo resetSharedInstanceForTesting];
  NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];
  [defaults removeObjectForKey:kDidSeeMemoryWarningShortlyBeforeTerminating];
  [defaults removeObjectForKey:kLastRanVersion];

  // Set the current version as the last ran version.
  [defaults setObject:@"Fake Version" forKey:kLastRanVersion];

  // Instantiate the PreviousSessionInfo sharedInstance.
  PreviousSessionInfo* sharedInstance = [PreviousSessionInfo sharedInstance];

  // Checks the values.
  EXPECT_FALSE([sharedInstance didSeeMemoryWarningShortlyBeforeTerminating]);
  EXPECT_TRUE([sharedInstance isFirstSessionAfterUpgrade]);
}

TEST_F(PreviousSessionInfoTest, InitializationDifferentVersionMemoryWarning) {
  [PreviousSessionInfo resetSharedInstanceForTesting];
  NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];
  [defaults removeObjectForKey:kDidSeeMemoryWarningShortlyBeforeTerminating];
  [defaults removeObjectForKey:kLastRanVersion];

  // Set the current version as the last ran version.
  [defaults setObject:@"Fake Version" forKey:kLastRanVersion];

  // Set the memory warning flag as a previous session would have.
  [defaults setBool:YES forKey:kDidSeeMemoryWarningShortlyBeforeTerminating];

  // Instantiate the PreviousSessionInfo sharedInstance.
  PreviousSessionInfo* sharedInstance = [PreviousSessionInfo sharedInstance];

  // Checks the values.
  EXPECT_TRUE([sharedInstance didSeeMemoryWarningShortlyBeforeTerminating]);
  EXPECT_TRUE([sharedInstance isFirstSessionAfterUpgrade]);
}

// Creates conditions that exist on the first app run and tests
// OSRestartedAfterPreviousSession property.
TEST_F(PreviousSessionInfoTest, InitializationWithoutSystemStartTime) {
  [PreviousSessionInfo resetSharedInstanceForTesting];
  [[NSUserDefaults standardUserDefaults]
      removeObjectForKey:previous_session_info_constants::kOSStartTime];

  EXPECT_FALSE(
      [[PreviousSessionInfo sharedInstance] OSRestartedAfterPreviousSession]);
}

// Creates conditions that exist when OS was restarted after the previous app
// run and tests OSRestartedAfterPreviousSession property.
TEST_F(PreviousSessionInfoTest, InitializationAfterOSRestart) {
  [PreviousSessionInfo resetSharedInstanceForTesting];

  // For the previous session OS started 60 seconds before OS has started for
  // this session.
  NSTimeInterval current_system_start_time =
      NSDate.timeIntervalSinceReferenceDate -
      NSProcessInfo.processInfo.systemUptime;
  [[NSUserDefaults standardUserDefaults]
      setDouble:current_system_start_time - 60
         forKey:previous_session_info_constants::kOSStartTime];

  EXPECT_TRUE(
      [[PreviousSessionInfo sharedInstance] OSRestartedAfterPreviousSession]);
}

// Creates conditions that exist when OS was not restarted after the previous
// app run and tests OSRestartedAfterPreviousSession property.
TEST_F(PreviousSessionInfoTest, InitializationForSecondSessionAfterOSRestart) {
  [PreviousSessionInfo resetSharedInstanceForTesting];

  // OS startup time is the same for this and previous session.
  NSTimeInterval current_system_start_time =
      NSDate.timeIntervalSinceReferenceDate -
      NSProcessInfo.processInfo.systemUptime;
  [[NSUserDefaults standardUserDefaults]
      setDouble:current_system_start_time
         forKey:previous_session_info_constants::kOSStartTime];

  EXPECT_FALSE(
      [[PreviousSessionInfo sharedInstance] OSRestartedAfterPreviousSession]);
}

TEST_F(PreviousSessionInfoTest, BeginRecordingCurrentSession) {
  [PreviousSessionInfo resetSharedInstanceForTesting];
  NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];
  [defaults removeObjectForKey:kDidSeeMemoryWarningShortlyBeforeTerminating];
  [defaults removeObjectForKey:kLastRanVersion];

  // Set the memory warning flag as a previous session would have.
  [defaults setBool:YES forKey:kDidSeeMemoryWarningShortlyBeforeTerminating];

  [[PreviousSessionInfo sharedInstance] beginRecordingCurrentSession];

  // Check that the version has been updated.
  EXPECT_NSEQ(base::SysUTF8ToNSString(version_info::GetVersionNumber()),
              [defaults stringForKey:kLastRanVersion]);

  // Check that the memory warning flag has been reset.
  EXPECT_FALSE(
      [defaults boolForKey:kDidSeeMemoryWarningShortlyBeforeTerminating]);
}

TEST_F(PreviousSessionInfoTest, SetMemoryWarningFlagNoOpUntilRecordingBegins) {
  [PreviousSessionInfo resetSharedInstanceForTesting];
  NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];
  [defaults removeObjectForKey:kDidSeeMemoryWarningShortlyBeforeTerminating];
  [defaults removeObjectForKey:kLastRanVersion];

  // Call the flag setter.
  [[PreviousSessionInfo sharedInstance] setMemoryWarningFlag];

  EXPECT_FALSE(
      [defaults boolForKey:kDidSeeMemoryWarningShortlyBeforeTerminating]);
}

TEST_F(PreviousSessionInfoTest,
       ResetMemoryWarningFlagNoOpUntilRecordingBegins) {
  [PreviousSessionInfo resetSharedInstanceForTesting];
  NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];
  [defaults removeObjectForKey:kDidSeeMemoryWarningShortlyBeforeTerminating];
  [defaults removeObjectForKey:kLastRanVersion];

  // Set the memory warning flag as a previous session would have.
  [defaults setBool:YES forKey:kDidSeeMemoryWarningShortlyBeforeTerminating];

  // Call the memory warning flag resetter.
  [[PreviousSessionInfo sharedInstance] resetMemoryWarningFlag];

  EXPECT_TRUE(
      [defaults boolForKey:kDidSeeMemoryWarningShortlyBeforeTerminating]);
}

TEST_F(PreviousSessionInfoTest, MemoryWarningFlagMethodsAfterRecordingBegins) {
  [PreviousSessionInfo resetSharedInstanceForTesting];
  NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];
  [defaults removeObjectForKey:kDidSeeMemoryWarningShortlyBeforeTerminating];
  [defaults removeObjectForKey:kLastRanVersion];

  // Launch the recording of the session.
  [[PreviousSessionInfo sharedInstance] beginRecordingCurrentSession];

  EXPECT_FALSE(
      [defaults boolForKey:kDidSeeMemoryWarningShortlyBeforeTerminating]);

  // Call the memory warning flag setter.
  [[PreviousSessionInfo sharedInstance] setMemoryWarningFlag];

  EXPECT_TRUE(
      [defaults boolForKey:kDidSeeMemoryWarningShortlyBeforeTerminating]);

  // Call the memory warning flag resetter.
  [[PreviousSessionInfo sharedInstance] resetMemoryWarningFlag];

  EXPECT_FALSE(
      [defaults boolForKey:kDidSeeMemoryWarningShortlyBeforeTerminating]);
}

// Tests restoringSession is in sync with User Defaults.
TEST_F(PreviousSessionInfoTest, NoSessionRestorationInProgress) {
  [PreviousSessionInfo resetSharedInstanceForTesting];

  [NSUserDefaults.standardUserDefaults
      removeObjectForKey:kPreviousSessionInfoRestoringSession];
  [[PreviousSessionInfo sharedInstance] beginRecordingCurrentSession];

  EXPECT_FALSE([[PreviousSessionInfo sharedInstance]
      terminatedDuringSessionRestoration]);
}

// Tests restoringSession is in sync with User Defaults.
TEST_F(PreviousSessionInfoTest, SessionRestorationInProgress) {
  [PreviousSessionInfo resetSharedInstanceForTesting];

  [NSUserDefaults.standardUserDefaults
      setBool:YES
       forKey:kPreviousSessionInfoRestoringSession];
  [[PreviousSessionInfo sharedInstance] beginRecordingCurrentSession];

  EXPECT_TRUE([[PreviousSessionInfo sharedInstance]
      terminatedDuringSessionRestoration]);
}

// Tests that resetSessionRestorationFlag resets User Defaults.
TEST_F(PreviousSessionInfoTest, ResetSessionRestorationFlag) {
  [PreviousSessionInfo resetSharedInstanceForTesting];
  [NSUserDefaults.standardUserDefaults
      setBool:YES
       forKey:kPreviousSessionInfoRestoringSession];
  [[PreviousSessionInfo sharedInstance] beginRecordingCurrentSession];

  ASSERT_TRUE([NSUserDefaults.standardUserDefaults
      boolForKey:kPreviousSessionInfoRestoringSession]);
  EXPECT_TRUE([[PreviousSessionInfo sharedInstance]
      terminatedDuringSessionRestoration]);

  [[PreviousSessionInfo sharedInstance] resetSessionRestorationFlag];

  EXPECT_FALSE([NSUserDefaults.standardUserDefaults
      boolForKey:kPreviousSessionInfoRestoringSession]);
  EXPECT_FALSE([[PreviousSessionInfo sharedInstance]
      terminatedDuringSessionRestoration]);
}

// Tests that AddSceneSessionID adds to User Defaults.
TEST_F(PreviousSessionInfoTest, AddSceneSessionID) {
  [PreviousSessionInfo resetSharedInstanceForTesting];
  [[PreviousSessionInfo sharedInstance] addSceneSessionID:kTestSession1ID];
  [[PreviousSessionInfo sharedInstance] addSceneSessionID:kTestSession2ID];
  NSArray<NSString*>* sessionIDs = [NSUserDefaults.standardUserDefaults
      stringArrayForKey:kPreviousSessionInfoConnectedSceneSessionIDs];
  EXPECT_TRUE([sessionIDs containsObject:kTestSession1ID]);
  EXPECT_TRUE([sessionIDs containsObject:kTestSession2ID]);
  EXPECT_EQ(2U, [sessionIDs count]);
}

// Tests that RemoveSceneSessionID removes id from User Defaults.
TEST_F(PreviousSessionInfoTest, RemoveSceneSessionID) {
  [PreviousSessionInfo resetSharedInstanceForTesting];
  [[PreviousSessionInfo sharedInstance] addSceneSessionID:kTestSession1ID];
  [[PreviousSessionInfo sharedInstance] addSceneSessionID:kTestSession2ID];
  [[PreviousSessionInfo sharedInstance] addSceneSessionID:kTestSession3ID];
  NSArray<NSString*>* sessionIDs = [NSUserDefaults.standardUserDefaults
      stringArrayForKey:kPreviousSessionInfoConnectedSceneSessionIDs];
  ASSERT_EQ(3U, [sessionIDs count]);
  [[PreviousSessionInfo sharedInstance] removeSceneSessionID:kTestSession3ID];
  [[PreviousSessionInfo sharedInstance] removeSceneSessionID:kTestSession1ID];
  sessionIDs = [NSUserDefaults.standardUserDefaults
      stringArrayForKey:kPreviousSessionInfoConnectedSceneSessionIDs];
  EXPECT_FALSE([sessionIDs containsObject:kTestSession3ID]);
  EXPECT_FALSE([sessionIDs containsObject:kTestSession1ID]);
  EXPECT_EQ(1U, [sessionIDs count]);
}

// Tests that resetConnectedSceneSessionIDs remove all session ids from User
// Defaults.
TEST_F(PreviousSessionInfoTest, resetConnectedSceneSessionIDs) {
  [PreviousSessionInfo resetSharedInstanceForTesting];
  [[PreviousSessionInfo sharedInstance] addSceneSessionID:kTestSession1ID];
  [[PreviousSessionInfo sharedInstance] addSceneSessionID:kTestSession2ID];
  [[PreviousSessionInfo sharedInstance] addSceneSessionID:kTestSession3ID];
  NSArray<NSString*>* sessionIDs = [NSUserDefaults.standardUserDefaults
      stringArrayForKey:kPreviousSessionInfoConnectedSceneSessionIDs];
  ASSERT_EQ(3U, [sessionIDs count]);
  [[PreviousSessionInfo sharedInstance] resetConnectedSceneSessionIDs];
  sessionIDs = [NSUserDefaults.standardUserDefaults
      stringArrayForKey:kPreviousSessionInfoConnectedSceneSessionIDs];
  EXPECT_EQ(0U, [sessionIDs count]);
}

// Tests that scoped object returned from startSessionRestoration correctly
// resets User Defaults.
TEST_F(PreviousSessionInfoTest, ParallelSessionRestorations) {
  [PreviousSessionInfo resetSharedInstanceForTesting];

  [NSUserDefaults.standardUserDefaults
      removeObjectForKey:kPreviousSessionInfoRestoringSession];
  [[PreviousSessionInfo sharedInstance] beginRecordingCurrentSession];
  ASSERT_FALSE([[PreviousSessionInfo sharedInstance]
      terminatedDuringSessionRestoration]);

  {
    base::ScopedClosureRunner scoped_restoration =
        [[PreviousSessionInfo sharedInstance] startSessionRestoration];
    EXPECT_TRUE([NSUserDefaults.standardUserDefaults
        boolForKey:kPreviousSessionInfoRestoringSession]);
    // This should reset to NO after beginRecordingCurrentSession or
    // resetSessionRestorationFlag
    EXPECT_FALSE([[PreviousSessionInfo sharedInstance]
        terminatedDuringSessionRestoration]);
    {
      base::ScopedClosureRunner scoped_restoration2 =
          [[PreviousSessionInfo sharedInstance] startSessionRestoration];
      EXPECT_TRUE([NSUserDefaults.standardUserDefaults
          boolForKey:kPreviousSessionInfoRestoringSession]);
      // This should reset to NO after beginRecordingCurrentSession or
      // resetSessionRestorationFlag
      EXPECT_FALSE([[PreviousSessionInfo sharedInstance]
          terminatedDuringSessionRestoration]);
    }
    EXPECT_TRUE([NSUserDefaults.standardUserDefaults
        boolForKey:kPreviousSessionInfoRestoringSession]);
    // This should reset to NO after beginRecordingCurrentSession or
    // resetSessionRestorationFlag
    EXPECT_FALSE([[PreviousSessionInfo sharedInstance]
        terminatedDuringSessionRestoration]);
  }
  EXPECT_FALSE([NSUserDefaults.standardUserDefaults
      boolForKey:kPreviousSessionInfoRestoringSession]);
  EXPECT_FALSE([[PreviousSessionInfo sharedInstance]
      terminatedDuringSessionRestoration]);
}

// Tests that resetSessionRestorationFlag resets the flag during session
// restoration and that flag is kept reset after restoration is finished.
TEST_F(PreviousSessionInfoTest,
       ResetSessionRestorationFlagDuringParallelSessionRestorations) {
  [PreviousSessionInfo resetSharedInstanceForTesting];

  [NSUserDefaults.standardUserDefaults
      removeObjectForKey:kPreviousSessionInfoRestoringSession];
  [[PreviousSessionInfo sharedInstance] beginRecordingCurrentSession];
  ASSERT_FALSE([[PreviousSessionInfo sharedInstance]
      terminatedDuringSessionRestoration]);

  {
    base::ScopedClosureRunner scoped_restoration =
        [[PreviousSessionInfo sharedInstance] startSessionRestoration];
    EXPECT_TRUE([NSUserDefaults.standardUserDefaults
        boolForKey:kPreviousSessionInfoRestoringSession]);
    // This should reset to NO after beginRecordingCurrentSession or
    // resetSessionRestorationFlag
    EXPECT_FALSE([[PreviousSessionInfo sharedInstance]
        terminatedDuringSessionRestoration]);
    {
      base::ScopedClosureRunner scoped_restoration2 =
          [[PreviousSessionInfo sharedInstance] startSessionRestoration];
      EXPECT_TRUE([NSUserDefaults.standardUserDefaults
          boolForKey:kPreviousSessionInfoRestoringSession]);
      // This should reset to NO after beginRecordingCurrentSession or
      // resetSessionRestorationFlag
      EXPECT_FALSE([[PreviousSessionInfo sharedInstance]
          terminatedDuringSessionRestoration]);

      [[PreviousSessionInfo sharedInstance] resetSessionRestorationFlag];
      EXPECT_FALSE([[PreviousSessionInfo sharedInstance]
          terminatedDuringSessionRestoration]);
      EXPECT_FALSE([NSUserDefaults.standardUserDefaults
          boolForKey:kPreviousSessionInfoRestoringSession]);
    }
    // scoped_restoration2 should not set |restoringSession| to previous state
    // (YES), but rather leave the reset state.
    EXPECT_FALSE([NSUserDefaults.standardUserDefaults
        boolForKey:kPreviousSessionInfoRestoringSession]);
    EXPECT_FALSE([[PreviousSessionInfo sharedInstance]
        terminatedDuringSessionRestoration]);
  }
  EXPECT_FALSE([NSUserDefaults.standardUserDefaults
      boolForKey:kPreviousSessionInfoRestoringSession]);
  EXPECT_FALSE([[PreviousSessionInfo sharedInstance]
      terminatedDuringSessionRestoration]);
}

}  // namespace
