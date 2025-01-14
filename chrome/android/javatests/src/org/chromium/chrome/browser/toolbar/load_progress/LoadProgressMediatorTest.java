// Copyright 2020 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.toolbar.load_progress;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.test.filters.SmallTest;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.chromium.base.MathUtils;
import org.chromium.base.test.BaseJUnit4ClassRunner;
import org.chromium.base.test.UiThreadTest;
import org.chromium.base.test.util.Batch;
import org.chromium.chrome.browser.ActivityTabProvider;
import org.chromium.chrome.browser.ActivityTabProvider.ActivityTabObserver;
import org.chromium.chrome.browser.tab.TabImpl;
import org.chromium.chrome.browser.tab.TabObserver;
import org.chromium.chrome.browser.toolbar.load_progress.LoadProgressProperties.CompletionState;
import org.chromium.content_public.browser.NavigationHandle;
import org.chromium.content_public.browser.test.util.Criteria;
import org.chromium.content_public.browser.test.util.CriteriaHelper;
import org.chromium.ui.modelutil.PropertyModel;
import org.chromium.url.GURL;

/** Unit tests for LoadProgressMediator. */
@RunWith(BaseJUnit4ClassRunner.class)
@Batch(Batch.UNIT_TESTS)
public class LoadProgressMediatorTest {
    private static final String URL_1 = "http://starting.url";
    private static final String NATIVE_PAGE_URL = "chrome-native://newtab";

    @Mock
    public ActivityTabProvider mActivityTabProvider;
    @Mock
    private TabImpl mTab;
    @Mock
    private TabImpl mTab2;

    @Captor
    public ArgumentCaptor<TabObserver> mTabObserverCaptor;
    @Captor
    public ArgumentCaptor<ActivityTabObserver> mActivityTabObserverCaptor;

    private PropertyModel mModel;
    private LoadProgressMediator mMediator;
    private TabObserver mTabObserver;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mActivityTabProvider.get()).thenReturn(mTab);

        mModel = new PropertyModel(LoadProgressProperties.ALL_KEYS);
        mMediator = new LoadProgressMediator(mActivityTabProvider, mModel);
        verify(mActivityTabProvider).addObserver(mActivityTabObserverCaptor.capture());
        verify(mTab).addObserver(mTabObserverCaptor.capture());
        mTabObserver = mTabObserverCaptor.getValue();
    }

    @Test
    @SmallTest
    public void loadRegularPage() {
        assertEquals(mModel.get(LoadProgressProperties.COMPLETION_STATE),
                CompletionState.FINISHED_DONT_ANIMATE);

        NavigationHandle navigation = new NavigationHandle(0, new GURL(URL_1), true, false, false);
        mTabObserver.onDidStartNavigation(mTab, navigation);
        assertEquals(
                mModel.get(LoadProgressProperties.COMPLETION_STATE), CompletionState.UNFINISHED);
        assertEquals(mModel.get(LoadProgressProperties.PROGRESS),
                LoadProgressMediator.MINIMUM_LOAD_PROGRESS, MathUtils.EPSILON);

        mTabObserver.onLoadProgressChanged(mTab, 0.1f);
        assertEquals(mModel.get(LoadProgressProperties.PROGRESS), 0.1f, MathUtils.EPSILON);

        mTabObserver.onLoadProgressChanged(mTab, 1.0f);
        assertEquals(mModel.get(LoadProgressProperties.PROGRESS), 1.0f, MathUtils.EPSILON);
        assertEquals(mModel.get(LoadProgressProperties.COMPLETION_STATE),
                CompletionState.FINISHED_DO_ANIMATE);
    }

    @Test
    @SmallTest
    public void switchToLoadingTab() {
        doReturn(true).when(mTab2).isLoading();
        doReturn(0.1f).when(mTab2).getProgress();
        mActivityTabObserverCaptor.getValue().onActivityTabChanged(mTab2, false);
        verify(mTab2, times(1)).addObserver(any());

        assertEquals(
                mModel.get(LoadProgressProperties.COMPLETION_STATE), CompletionState.UNFINISHED);
        assertEquals(mModel.get(LoadProgressProperties.PROGRESS), 0.1f, MathUtils.EPSILON);
    }

    @Test
    @SmallTest
    public void switchToLoadedTab() {
        NavigationHandle navigation = new NavigationHandle(0, new GURL(URL_1), true, false, false);
        mTabObserver.onDidStartNavigation(mTab, navigation);
        assertEquals(
                mModel.get(LoadProgressProperties.COMPLETION_STATE), CompletionState.UNFINISHED);
        assertEquals(mModel.get(LoadProgressProperties.PROGRESS),
                LoadProgressMediator.MINIMUM_LOAD_PROGRESS, MathUtils.EPSILON);

        mActivityTabObserverCaptor.getValue().onActivityTabChanged(mTab2, false);
        verify(mTab2, times(1)).addObserver(any());
        assertEquals(mModel.get(LoadProgressProperties.COMPLETION_STATE),
                CompletionState.FINISHED_DONT_ANIMATE);
    }

    @Test
    @SmallTest
    public void loadNativePage() {
        doReturn(0.1f).when(mTab).getProgress();
        NavigationHandle navigation = new NavigationHandle(0, new GURL(URL_1), true, false, false);
        mTabObserver.onDidStartNavigation(mTab, navigation);
        assertEquals(
                mModel.get(LoadProgressProperties.COMPLETION_STATE), CompletionState.UNFINISHED);
        assertEquals(mModel.get(LoadProgressProperties.PROGRESS), 0.1f, MathUtils.EPSILON);

        navigation = new NavigationHandle(0, new GURL(NATIVE_PAGE_URL), true, false, false);
        mTabObserver.onDidStartNavigation(mTab, navigation);
        assertEquals(mModel.get(LoadProgressProperties.COMPLETION_STATE),
                CompletionState.FINISHED_DONT_ANIMATE);
    }

    @Test
    @SmallTest
    public void switchToTabWithNativePage() {
        NavigationHandle navigation = new NavigationHandle(0, new GURL(URL_1), true, false, false);
        mTabObserver.onDidStartNavigation(mTab, navigation);
        assertEquals(
                mModel.get(LoadProgressProperties.COMPLETION_STATE), CompletionState.UNFINISHED);
        assertEquals(mModel.get(LoadProgressProperties.PROGRESS),
                LoadProgressMediator.MINIMUM_LOAD_PROGRESS, MathUtils.EPSILON);

        when(mTab2.getUrlString()).thenReturn(NATIVE_PAGE_URL);
        mActivityTabObserverCaptor.getValue().onActivityTabChanged(mTab2, false);
        verify(mTab2, times(1)).addObserver(any());
        assertEquals(mModel.get(LoadProgressProperties.COMPLETION_STATE),
                CompletionState.FINISHED_DONT_ANIMATE);
        assertEquals(mModel.get(LoadProgressProperties.PROGRESS),
                LoadProgressMediator.MINIMUM_LOAD_PROGRESS, MathUtils.EPSILON);
    }

    @Test
    @SmallTest
    public void pageCrashes() {
        NavigationHandle navigation = new NavigationHandle(0, new GURL(URL_1), true, false, false);
        mTabObserver.onDidStartNavigation(mTab, navigation);
        assertEquals(
                mModel.get(LoadProgressProperties.COMPLETION_STATE), CompletionState.UNFINISHED);
        assertEquals(mModel.get(LoadProgressProperties.PROGRESS),
                LoadProgressMediator.MINIMUM_LOAD_PROGRESS, MathUtils.EPSILON);

        mTabObserver.onCrash(mTab);
        assertEquals(mModel.get(LoadProgressProperties.COMPLETION_STATE),
                CompletionState.FINISHED_DONT_ANIMATE);
        assertEquals(mModel.get(LoadProgressProperties.PROGRESS),
                LoadProgressMediator.MINIMUM_LOAD_PROGRESS, MathUtils.EPSILON);
    }

    @Test
    @SmallTest
    @UiThreadTest
    public void testSwapWebContents() {
        assertEquals(mModel.get(LoadProgressProperties.COMPLETION_STATE),
                CompletionState.FINISHED_DONT_ANIMATE);
        mTabObserver.onWebContentsSwapped(mTab, true, true);
        assertEquals(
                mModel.get(LoadProgressProperties.COMPLETION_STATE), CompletionState.UNFINISHED);
        assertEquals(mModel.get(LoadProgressProperties.PROGRESS), 0, MathUtils.EPSILON);
        float expectedProgress = LoadProgressSimulator.PROGRESS_INCREMENT;
        while (expectedProgress < 1.0f + LoadProgressSimulator.PROGRESS_INCREMENT) {
            final float nextExpectedProgress = expectedProgress;
            CriteriaHelper.pollUiThreadNested(() -> {
                Criteria.checkThat((double) mModel.get(LoadProgressProperties.PROGRESS),
                        Matchers.closeTo(nextExpectedProgress, MathUtils.EPSILON));
            }, CriteriaHelper.DEFAULT_MAX_TIME_TO_POLL, 0);
            expectedProgress += LoadProgressSimulator.PROGRESS_INCREMENT;
        }

        assertEquals(mModel.get(LoadProgressProperties.COMPLETION_STATE),
                CompletionState.FINISHED_DO_ANIMATE);
    }

    @Test
    @SmallTest
    public void testSameDocumentLoad_afterFinishedLoading() {
        GURL gurl = new GURL(URL_1);
        assertEquals(mModel.get(LoadProgressProperties.COMPLETION_STATE),
                CompletionState.FINISHED_DONT_ANIMATE);

        NavigationHandle navigation = new NavigationHandle(0, gurl, true, false, false);
        mTabObserver.onDidStartNavigation(mTab, navigation);
        mTabObserver.onLoadProgressChanged(mTab, 1.0f);
        assertEquals(mModel.get(LoadProgressProperties.PROGRESS), 1.0f, MathUtils.EPSILON);
        assertEquals(mModel.get(LoadProgressProperties.COMPLETION_STATE),
                CompletionState.FINISHED_DO_ANIMATE);
        NavigationHandle sameDocNav = new NavigationHandle(0, gurl, true, true, false);
        mTabObserver.onDidStartNavigation(mTab, sameDocNav);

        assertEquals(mModel.get(LoadProgressProperties.PROGRESS), 1.0f, MathUtils.EPSILON);
        assertEquals(mModel.get(LoadProgressProperties.COMPLETION_STATE),
                CompletionState.FINISHED_DO_ANIMATE);
    }
}
