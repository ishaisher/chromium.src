// Copyright 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#import "ios/chrome/browser/metrics/tab_usage_recorder_browser_agent.h"

#import <UIKit/UIKit.h>

#include <memory>

#include "base/metrics/histogram_samples.h"
#include "base/test/metrics/histogram_tester.h"
#include "base/test/task_environment.h"
#import "ios/chrome/browser/main/test_browser.h"
#import "ios/chrome/browser/metrics/previous_session_info.h"
#import "ios/chrome/browser/web_state_list/web_state_list.h"
#import "ios/chrome/browser/web_state_list/web_state_opener.h"
#import "ios/web/public/test/fakes/test_navigation_manager.h"
#import "ios/web/public/test/fakes/test_web_state.h"
#include "ios/web/public/test/web_task_environment.h"
#include "testing/gtest/include/gtest/gtest.h"
#include "testing/platform_test.h"
#import "third_party/ocmock/OCMock/OCMock.h"

#if !defined(__has_feature) || !__has_feature(objc_arc)
#error "This file requires ARC support."
#endif

namespace {

// The number of alive tabs at a renderer termination used by unit test.
const int kAliveTabsCountAtRendererTermination = 2;

// The number of timestamps added to the renderer termination timestamp list
// that are not counted in the RecentlyAliveTabs metric.
const int kExpiredTimesAddedCount = 2;

// URL constants used by TabUsageRecorderBrowserAgentTest.
const char kURL[] = "http://www.chromium.org";
const char kNativeURL[] = "chrome://version";

// Option to InsertTestWebState() to create the WebState for a tab that is in
// memory or not.
enum WebStateInMemoryOption { NOT_IN_MEMORY = 0, IN_MEMORY };

}  // namespace

class TabUsageRecorderBrowserAgentTest : public PlatformTest {
 protected:
  TabUsageRecorderBrowserAgentTest()
      : application_(OCMClassMock([UIApplication class])) {
    TabUsageRecorderBrowserAgent::CreateForBrowser(&browser_);
    tab_usage_recorder_ = TabUsageRecorderBrowserAgent::FromBrowser(&browser_);
    OCMStub([application_ sharedApplication]).andReturn(application_);
  }

  ~TabUsageRecorderBrowserAgentTest() override { [application_ stopMocking]; }

  web::TestWebState* InsertTestWebState(const char* url,
                                        WebStateInMemoryOption in_memory) {
    auto test_navigation_manager =
        std::make_unique<web::TestNavigationManager>();
    web::NavigationItem* item =
        InsertItemToTestNavigationManager(test_navigation_manager.get(), url);
    test_navigation_manager->SetLastCommittedItem(item);

    auto test_web_state = std::make_unique<web::TestWebState>();
    test_web_state->SetNavigationManager(std::move(test_navigation_manager));
    test_web_state->SetIsEvicted(in_memory == NOT_IN_MEMORY);

    const int insertion_index = browser_.GetWebStateList()->InsertWebState(
        WebStateList::kInvalidIndex, std::move(test_web_state),
        WebStateList::INSERT_NO_FLAGS, WebStateOpener());

    return static_cast<web::TestWebState*>(
        browser_.GetWebStateList()->GetWebStateAt(insertion_index));
  }

  web::NavigationItem* InsertItemToTestNavigationManager(
      web::TestNavigationManager* test_navigation_manager,
      const char* url) {
    test_navigation_manager->AddItem(GURL(), ui::PAGE_TRANSITION_LINK);
    web::NavigationItem* item = test_navigation_manager->GetItemAtIndex(
        test_navigation_manager->GetLastCommittedItemIndex());
    item->SetVirtualURL(GURL(url));
    return item;
  }

  void AddTimeToDequeInTabUsageRecorder(base::TimeTicks time) {
    tab_usage_recorder_->termination_timestamps_.push_back(time);
  }

  web::WebTaskEnvironment task_environment_;
  TestBrowser browser_;
  base::HistogramTester histogram_tester_;
  TabUsageRecorderBrowserAgent* tab_usage_recorder_;
  id application_;
};

TEST_F(TabUsageRecorderBrowserAgentTest, SwitchBetweenInMemoryTabs) {
  web::TestWebState* mock_tab_a = InsertTestWebState(kURL, IN_MEMORY);
  web::TestWebState* mock_tab_b = InsertTestWebState(kURL, IN_MEMORY);

  tab_usage_recorder_->RecordTabSwitched(mock_tab_a, mock_tab_b);
  histogram_tester_.ExpectUniqueSample(
      tab_usage_recorder::kSelectedTabHistogramName,
      tab_usage_recorder::IN_MEMORY, 1);
}

TEST_F(TabUsageRecorderBrowserAgentTest, SwitchToEvictedTab) {
  web::TestWebState* mock_tab_a = InsertTestWebState(kURL, IN_MEMORY);
  web::TestWebState* mock_tab_b = InsertTestWebState(kURL, NOT_IN_MEMORY);

  tab_usage_recorder_->RecordTabSwitched(mock_tab_a, mock_tab_b);
  histogram_tester_.ExpectUniqueSample(
      tab_usage_recorder::kSelectedTabHistogramName,
      tab_usage_recorder::EVICTED, 1);
}

TEST_F(TabUsageRecorderBrowserAgentTest, SwitchFromEvictedTab) {
  web::TestWebState* mock_tab_a = InsertTestWebState(kURL, NOT_IN_MEMORY);
  web::TestWebState* mock_tab_b = InsertTestWebState(kURL, IN_MEMORY);

  tab_usage_recorder_->RecordTabSwitched(mock_tab_a, mock_tab_b);
  histogram_tester_.ExpectUniqueSample(
      tab_usage_recorder::kSelectedTabHistogramName,
      tab_usage_recorder::IN_MEMORY, 1);
}

TEST_F(TabUsageRecorderBrowserAgentTest, SwitchBetweenEvictedTabs) {
  web::TestWebState* mock_tab_a = InsertTestWebState(kURL, NOT_IN_MEMORY);
  web::TestWebState* mock_tab_b = InsertTestWebState(kURL, NOT_IN_MEMORY);

  tab_usage_recorder_->RecordTabSwitched(mock_tab_a, mock_tab_b);
  histogram_tester_.ExpectUniqueSample(
      tab_usage_recorder::kSelectedTabHistogramName,
      tab_usage_recorder::EVICTED, 1);
}

TEST_F(TabUsageRecorderBrowserAgentTest, CountPageLoadsBeforeEvictedTab) {
  web::TestWebState* mock_tab_a = InsertTestWebState(kURL, IN_MEMORY);
  web::TestWebState* mock_tab_b = InsertTestWebState(kURL, NOT_IN_MEMORY);

  // Call reload an arbitrary number of times.
  const int kNumReloads = 4;
  for (int i = 0; i < kNumReloads; i++) {
    tab_usage_recorder_->RecordPageLoadStart(mock_tab_a);
  }
  tab_usage_recorder_->RecordTabSwitched(mock_tab_a, mock_tab_b);
  histogram_tester_.ExpectUniqueSample(
      tab_usage_recorder::kPageLoadsBeforeEvictedTabSelected, kNumReloads, 1);
}

// Tests that chrome:// URLs are not counted in page load stats.
TEST_F(TabUsageRecorderBrowserAgentTest, CountNativePageLoadsBeforeEvictedTab) {
  web::TestWebState* mock_tab_a = InsertTestWebState(kNativeURL, IN_MEMORY);
  web::TestWebState* mock_tab_b = InsertTestWebState(kNativeURL, NOT_IN_MEMORY);

  // Call reload an arbitrary number of times.
  const int kNumReloads = 4;
  for (int i = 0; i < kNumReloads; i++) {
    tab_usage_recorder_->RecordPageLoadStart(mock_tab_a);
  }

  tab_usage_recorder_->RecordTabSwitched(mock_tab_a, mock_tab_b);
  histogram_tester_.ExpectTotalCount(
      tab_usage_recorder::kPageLoadsBeforeEvictedTabSelected, 0);
}

// Tests that page load stats is not updated for an evicted tab that has a
// pending chrome:// URL.
TEST_F(TabUsageRecorderBrowserAgentTest,
       CountPendingNativePageLoadBeforeEvictedTab) {
  web::TestWebState* old_tab = InsertTestWebState(kURL, IN_MEMORY);
  web::TestWebState* new_evicted_tab = InsertTestWebState(kURL, NOT_IN_MEMORY);

  tab_usage_recorder_->RecordPageLoadStart(old_tab);

  auto* test_navigation_manager = static_cast<web::TestNavigationManager*>(
      new_evicted_tab->GetNavigationManager());
  web::NavigationItem* item =
      InsertItemToTestNavigationManager(test_navigation_manager, kNativeURL);
  test_navigation_manager->SetPendingItem(item);

  tab_usage_recorder_->RecordTabSwitched(old_tab, new_evicted_tab);
  histogram_tester_.ExpectTotalCount(
      tab_usage_recorder::kPageLoadsBeforeEvictedTabSelected, 0);
}

TEST_F(TabUsageRecorderBrowserAgentTest, TestColdStartTabs) {
  web::TestWebState* mock_tab_a = InsertTestWebState(kURL, NOT_IN_MEMORY);
  web::TestWebState* mock_tab_b = InsertTestWebState(kURL, NOT_IN_MEMORY);
  web::TestWebState* mock_tab_c = InsertTestWebState(kURL, NOT_IN_MEMORY);
  // Set A and B as cold-start evicted tabs.  Leave C just evicted.
  std::vector<web::WebState*> cold_start_web_states = {
      mock_tab_a,
      mock_tab_b,
  };
  tab_usage_recorder_->InitialRestoredTabs(mock_tab_a, cold_start_web_states);

  // Switch from A (cold start evicted) to B (cold start evicted).
  tab_usage_recorder_->RecordTabSwitched(mock_tab_a, mock_tab_b);
  // Switch from B (cold start evicted) to C (evicted).
  tab_usage_recorder_->RecordTabSwitched(mock_tab_b, mock_tab_c);
  histogram_tester_.ExpectTotalCount(
      tab_usage_recorder::kSelectedTabHistogramName, 2);
  histogram_tester_.ExpectBucketCount(
      tab_usage_recorder::kSelectedTabHistogramName,
      tab_usage_recorder::EVICTED_DUE_TO_COLD_START, 1);
  histogram_tester_.ExpectBucketCount(
      tab_usage_recorder::kSelectedTabHistogramName,
      tab_usage_recorder::EVICTED, 1);
}

TEST_F(TabUsageRecorderBrowserAgentTest, TestSwitchedModeTabs) {
  web::TestWebState* mock_tab_a = InsertTestWebState(kURL, NOT_IN_MEMORY);
  web::TestWebState* mock_tab_b = InsertTestWebState(kURL, NOT_IN_MEMORY);
  web::TestWebState* mock_tab_c = InsertTestWebState(kURL, NOT_IN_MEMORY);
  tab_usage_recorder_->RecordPrimaryTabModelChange(false, nullptr);

  // Switch from A (incognito evicted) to B (incognito evicted).
  tab_usage_recorder_->RecordTabSwitched(mock_tab_a, mock_tab_b);
  // Switch from B (incognito evicted) to C (evicted).
  tab_usage_recorder_->RecordTabSwitched(mock_tab_b, mock_tab_c);
  histogram_tester_.ExpectTotalCount(
      tab_usage_recorder::kSelectedTabHistogramName, 2);
  histogram_tester_.ExpectBucketCount(
      tab_usage_recorder::kSelectedTabHistogramName,
      tab_usage_recorder::EVICTED_DUE_TO_INCOGNITO, 0);
  histogram_tester_.ExpectBucketCount(
      tab_usage_recorder::kSelectedTabHistogramName,
      tab_usage_recorder::EVICTED, 2);
}

TEST_F(TabUsageRecorderBrowserAgentTest, TestEvictedTabReloadTime) {
  web::TestWebState* mock_tab_a = InsertTestWebState(kURL, IN_MEMORY);
  web::TestWebState* mock_tab_b = InsertTestWebState(kURL, NOT_IN_MEMORY);
  tab_usage_recorder_->RecordTabSwitched(mock_tab_a, mock_tab_b);
  tab_usage_recorder_->RecordPageLoadStart(mock_tab_b);
  tab_usage_recorder_->RecordPageLoadDone(mock_tab_b, true);
  histogram_tester_.ExpectTotalCount(tab_usage_recorder::kEvictedTabReloadTime,
                                     1);
}

TEST_F(TabUsageRecorderBrowserAgentTest, TestEvictedTabReloadSuccess) {
  web::TestWebState* mock_tab_a = InsertTestWebState(kURL, IN_MEMORY);
  web::TestWebState* mock_tab_b = InsertTestWebState(kURL, NOT_IN_MEMORY);
  tab_usage_recorder_->RecordTabSwitched(mock_tab_a, mock_tab_b);
  tab_usage_recorder_->RecordPageLoadStart(mock_tab_b);
  tab_usage_recorder_->RecordPageLoadDone(mock_tab_b, true);
  histogram_tester_.ExpectUniqueSample(
      tab_usage_recorder::kEvictedTabReloadSuccessRate,
      tab_usage_recorder::LOAD_SUCCESS, 1);
}

TEST_F(TabUsageRecorderBrowserAgentTest, TestEvictedTabReloadFailure) {
  web::TestWebState* mock_tab_a = InsertTestWebState(kURL, IN_MEMORY);
  web::TestWebState* mock_tab_b = InsertTestWebState(kURL, NOT_IN_MEMORY);
  tab_usage_recorder_->RecordTabSwitched(mock_tab_a, mock_tab_b);
  tab_usage_recorder_->RecordPageLoadStart(mock_tab_b);
  tab_usage_recorder_->RecordPageLoadDone(mock_tab_b, false);
  histogram_tester_.ExpectUniqueSample(
      tab_usage_recorder::kEvictedTabReloadSuccessRate,
      tab_usage_recorder::LOAD_FAILURE, 1);
}

TEST_F(TabUsageRecorderBrowserAgentTest, TestUserWaitedForEvictedTabLoad) {
  web::TestWebState* mock_tab_a = InsertTestWebState(kURL, IN_MEMORY);
  web::TestWebState* mock_tab_b = InsertTestWebState(kURL, NOT_IN_MEMORY);
  tab_usage_recorder_->RecordTabSwitched(mock_tab_a, mock_tab_b);
  tab_usage_recorder_->RecordPageLoadStart(mock_tab_b);
  tab_usage_recorder_->RecordPageLoadDone(mock_tab_b, true);
  tab_usage_recorder_->RecordTabSwitched(mock_tab_b, mock_tab_a);
  histogram_tester_.ExpectUniqueSample(
      tab_usage_recorder::kDidUserWaitForEvictedTabReload,
      tab_usage_recorder::USER_WAITED, 1);
}

TEST_F(TabUsageRecorderBrowserAgentTest, TestUserDidNotWaitForEvictedTabLoad) {
  web::TestWebState* mock_tab_a = InsertTestWebState(kURL, IN_MEMORY);
  web::TestWebState* mock_tab_b = InsertTestWebState(kURL, NOT_IN_MEMORY);
  tab_usage_recorder_->RecordTabSwitched(mock_tab_a, mock_tab_b);
  tab_usage_recorder_->RecordPageLoadStart(mock_tab_b);
  tab_usage_recorder_->RecordTabSwitched(mock_tab_b, mock_tab_a);
  histogram_tester_.ExpectUniqueSample(
      tab_usage_recorder::kDidUserWaitForEvictedTabReload,
      tab_usage_recorder::USER_DID_NOT_WAIT, 1);
}

TEST_F(TabUsageRecorderBrowserAgentTest,
       TestUserBackgroundedDuringEvictedTabLoad) {
  web::TestWebState* mock_tab_a = InsertTestWebState(kURL, IN_MEMORY);
  web::TestWebState* mock_tab_b = InsertTestWebState(kURL, NOT_IN_MEMORY);
  tab_usage_recorder_->RecordTabSwitched(mock_tab_a, mock_tab_b);
  tab_usage_recorder_->RecordPageLoadStart(mock_tab_b);
  tab_usage_recorder_->AppDidEnterBackground();
  histogram_tester_.ExpectUniqueSample(
      tab_usage_recorder::kDidUserWaitForEvictedTabReload,
      tab_usage_recorder::USER_LEFT_CHROME, 1);
}

TEST_F(TabUsageRecorderBrowserAgentTest, TestTimeBetweenRestores) {
  web::TestWebState* mock_tab_a = InsertTestWebState(kURL, NOT_IN_MEMORY);
  web::TestWebState* mock_tab_b = InsertTestWebState(kURL, NOT_IN_MEMORY);
  tab_usage_recorder_->RecordTabSwitched(mock_tab_a, mock_tab_b);
  // Should record the time since launch until this page load begins.
  tab_usage_recorder_->RecordPageLoadStart(mock_tab_b);
  tab_usage_recorder_->RecordTabSwitched(mock_tab_b, mock_tab_a);
  // Should record the time since previous restore until this restore.
  tab_usage_recorder_->RecordPageLoadStart(mock_tab_a);
  histogram_tester_.ExpectTotalCount(tab_usage_recorder::kTimeBetweenRestores,
                                     2);
}

TEST_F(TabUsageRecorderBrowserAgentTest, TestTimeAfterLastRestore) {
  web::TestWebState* mock_tab_a = InsertTestWebState(kURL, NOT_IN_MEMORY);
  web::TestWebState* mock_tab_b = InsertTestWebState(kURL, NOT_IN_MEMORY);
  // Should record time since launch until background.
  tab_usage_recorder_->AppDidEnterBackground();
  tab_usage_recorder_->AppWillEnterForeground();
  tab_usage_recorder_->RecordTabSwitched(mock_tab_a, mock_tab_b);
  // Should record nothing.
  tab_usage_recorder_->RecordPageLoadStart(mock_tab_b);
  histogram_tester_.ExpectTotalCount(tab_usage_recorder::kTimeAfterLastRestore,
                                     1);
}

// Verifies that metrics are recorded correctly when a renderer terminates.
TEST_F(TabUsageRecorderBrowserAgentTest, RendererTerminated) {
  web::TestWebState* mock_tab_a = InsertTestWebState(kURL, NOT_IN_MEMORY);
  OCMStub([static_cast<UIApplication*>(application_) applicationState])
      .andReturn(UIApplicationStateActive);

  // Add some extra WebStates that are not considered evicted so that
  // TabUsageRecorder count kAliveTabsCountAtRendererTermination tabs
  // as alive when mock_tab_a is evicted.
  for (int ii = 0; ii < kAliveTabsCountAtRendererTermination; ++ii) {
    ignore_result(InsertTestWebState(kURL, IN_MEMORY));
  }

  base::TimeTicks now = base::TimeTicks::Now();

  // Add |kExpiredTimesAddedCount| expired timestamps and one recent timestamp
  // to the termination timestamp list.
  for (int seconds = kExpiredTimesAddedCount; seconds > 0; seconds--) {
    int expired_time_delta =
        tab_usage_recorder::kSecondsBeforeRendererTermination + seconds;
    AddTimeToDequeInTabUsageRecorder(
        now - base::TimeDelta::FromSeconds(expired_time_delta));
  }
  base::TimeTicks recent_time =
      now - base::TimeDelta::FromSeconds(
                tab_usage_recorder::kSecondsBeforeRendererTermination / 2);
  AddTimeToDequeInTabUsageRecorder(recent_time);

  mock_tab_a->OnRenderProcessGone();

  NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];
  BOOL saw_memory_warning =
      [defaults boolForKey:previous_session_info_constants::
                               kDidSeeMemoryWarningShortlyBeforeTerminating];
  histogram_tester_.ExpectUniqueSample(
      tab_usage_recorder::kRendererTerminationSawMemoryWarning,
      saw_memory_warning, 1);
  histogram_tester_.ExpectUniqueSample(
      tab_usage_recorder::kRendererTerminationAliveRenderers,
      kAliveTabsCountAtRendererTermination, 1);
  // Tests that the logged count of recently alive renderers is equal to the
  // live count at termination plus the recent termination and the
  // renderer terminated just now.
  histogram_tester_.ExpectUniqueSample(
      tab_usage_recorder::kRendererTerminationRecentlyAliveRenderers,
      kAliveTabsCountAtRendererTermination + 2, 1);

  // Regression test for crbug.com/935205
  // Terminate the same tab again. Verify that it isn't double-counted.
  mock_tab_a->OnRenderProcessGone();
  histogram_tester_.ExpectUniqueSample(
      tab_usage_recorder::kRendererTerminationAliveRenderers,
      kAliveTabsCountAtRendererTermination, 1);
  histogram_tester_.ExpectUniqueSample(
      tab_usage_recorder::kRendererTerminationRecentlyAliveRenderers,
      kAliveTabsCountAtRendererTermination + 2, 1);
}

// Verifies that metrics are recorded correctly when a renderer terminated tab
// is switched to and reloaded.
TEST_F(TabUsageRecorderBrowserAgentTest, SwitchToRendererTerminatedTab) {
  web::TestWebState* mock_tab_a = InsertTestWebState(kURL, IN_MEMORY);
  web::TestWebState* mock_tab_b = InsertTestWebState(kURL, NOT_IN_MEMORY);
  OCMStub([static_cast<UIApplication*>(application_) applicationState])
      .andReturn(UIApplicationStateActive);

  mock_tab_b->OnRenderProcessGone();
  tab_usage_recorder_->RecordTabSwitched(mock_tab_a, mock_tab_b);

  histogram_tester_.ExpectUniqueSample(
      tab_usage_recorder::kSelectedTabHistogramName,
      tab_usage_recorder::EVICTED_DUE_TO_RENDERER_TERMINATION, 1);
}

// Verifies that Tab.StateAtRendererTermination metric is correctly reported
// when the application is in the foreground.
TEST_F(TabUsageRecorderBrowserAgentTest, StateAtRendererTerminationForeground) {
  web::TestWebState* mock_tab_a = InsertTestWebState(kURL, IN_MEMORY);
  web::TestWebState* mock_tab_b = InsertTestWebState(kURL, IN_MEMORY);
  OCMStub([static_cast<UIApplication*>(application_) applicationState])
      .andReturn(UIApplicationStateActive);

  mock_tab_a->WasShown();
  mock_tab_a->OnRenderProcessGone();
  histogram_tester_.ExpectBucketCount(
      tab_usage_recorder::kRendererTerminationStateHistogram,
      tab_usage_recorder::FOREGROUND_TAB_FOREGROUND_APP, 1);

  mock_tab_b->OnRenderProcessGone();
  histogram_tester_.ExpectBucketCount(
      tab_usage_recorder::kRendererTerminationStateHistogram,
      tab_usage_recorder::BACKGROUND_TAB_FOREGROUND_APP, 1);
}

// Verifies that Tab.StateAtRendererTermination metric is correctly reported
// when the application is in the background.
TEST_F(TabUsageRecorderBrowserAgentTest, StateAtRendererTerminationBackground) {
  web::TestWebState* mock_tab_a = InsertTestWebState(kURL, IN_MEMORY);
  web::TestWebState* mock_tab_b = InsertTestWebState(kURL, IN_MEMORY);
  OCMStub([static_cast<UIApplication*>(application_) applicationState])
      .andReturn(UIApplicationStateBackground);

  mock_tab_a->WasShown();
  mock_tab_a->OnRenderProcessGone();
  histogram_tester_.ExpectBucketCount(
      tab_usage_recorder::kRendererTerminationStateHistogram,
      tab_usage_recorder::FOREGROUND_TAB_BACKGROUND_APP, 1);

  mock_tab_b->OnRenderProcessGone();
  histogram_tester_.ExpectBucketCount(
      tab_usage_recorder::kRendererTerminationStateHistogram,
      tab_usage_recorder::BACKGROUND_TAB_BACKGROUND_APP, 1);
}

// Verifies that Tab.StateAtRendererTermination metric is correctly reported
// when the application is in the inactive state.
TEST_F(TabUsageRecorderBrowserAgentTest, StateAtRendererTerminationInactive) {
  web::TestWebState* mock_tab_a = InsertTestWebState(kURL, IN_MEMORY);
  web::TestWebState* mock_tab_b = InsertTestWebState(kURL, IN_MEMORY);
  OCMStub([static_cast<UIApplication*>(application_) applicationState])
      .andReturn(UIApplicationStateInactive);

  mock_tab_a->WasShown();
  mock_tab_a->OnRenderProcessGone();
  histogram_tester_.ExpectBucketCount(
      tab_usage_recorder::kRendererTerminationStateHistogram,
      tab_usage_recorder::FOREGROUND_TAB_BACKGROUND_APP, 1);

  mock_tab_b->OnRenderProcessGone();
  histogram_tester_.ExpectBucketCount(
      tab_usage_recorder::kRendererTerminationStateHistogram,
      tab_usage_recorder::BACKGROUND_TAB_BACKGROUND_APP, 1);
}
