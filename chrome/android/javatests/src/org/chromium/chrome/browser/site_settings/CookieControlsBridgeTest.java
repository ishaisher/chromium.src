// Copyright 2019 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.site_settings;

import static org.junit.Assert.assertEquals;

import android.support.test.InstrumentationRegistry;

import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.chromium.base.test.util.CallbackHelper;
import org.chromium.base.test.util.CommandLineFlags;
import org.chromium.chrome.browser.flags.ChromeSwitches;
import org.chromium.chrome.browser.profiles.Profile;
import org.chromium.chrome.browser.tab.Tab;
import org.chromium.chrome.test.ChromeJUnit4ClassRunner;
import org.chromium.chrome.test.ChromeTabbedActivityTestRule;
import org.chromium.components.browser_ui.site_settings.WebsitePreferenceBridge;
import org.chromium.components.content_settings.ContentSettingsType;
import org.chromium.components.content_settings.CookieControlsBridge;
import org.chromium.components.content_settings.CookieControlsEnforcement;
import org.chromium.components.content_settings.CookieControlsMode;
import org.chromium.components.content_settings.CookieControlsObserver;
import org.chromium.components.content_settings.CookieControlsStatus;
import org.chromium.components.content_settings.PrefNames;
import org.chromium.components.user_prefs.UserPrefs;
import org.chromium.content_public.browser.test.util.JavaScriptUtils;
import org.chromium.content_public.browser.test.util.TestThreadUtils;
import org.chromium.net.test.EmbeddedTestServer;

/**
 * Integration tests for CookieControlsBridge.
 */
@RunWith(ChromeJUnit4ClassRunner.class)
@CommandLineFlags.Add({ChromeSwitches.DISABLE_FIRST_RUN_EXPERIENCE})
public class CookieControlsBridgeTest {
    private class TestCallbackHandler implements CookieControlsObserver {
        private CallbackHelper mHelper;

        public TestCallbackHandler(CallbackHelper helper) {
            mHelper = helper;
        }

        @Override
        public void onCookieBlockingStatusChanged(
                @CookieControlsStatus int status, @CookieControlsEnforcement int enforcement) {
            mStatus = status;
            mEnforcement = enforcement;
            mHelper.notifyCalled();
        }

        @Override
        public void onCookiesCountChanged(int allowedCookies, int blockedCookies) {
            mAllowedCookies = allowedCookies;
            mBlockedCookies = blockedCookies;
            mHelper.notifyCalled();
        }
    }

    @Rule
    public ChromeTabbedActivityTestRule mActivityTestRule = new ChromeTabbedActivityTestRule();
    private EmbeddedTestServer mTestServer;
    private CallbackHelper mCallbackHelper;
    private TestCallbackHandler mCallbackHandler;
    private CookieControlsBridge mCookieControlsBridge;
    private int mStatus;
    private int mEnforcement;
    private int mAllowedCookies;
    private int mBlockedCookies;

    @Before
    public void setUp() throws Exception {
        mCallbackHelper = new CallbackHelper();
        mCallbackHandler = new TestCallbackHandler(mCallbackHelper);
        mActivityTestRule.startMainActivityOnBlankPage();
        mTestServer = EmbeddedTestServer.createAndStartServer(InstrumentationRegistry.getContext());
        mStatus = CookieControlsStatus.UNINITIALIZED;
        mAllowedCookies = -1;
        mBlockedCookies = -1;
    }

    @After
    public void tearDown() {
        mTestServer.stopAndDestroyServer();
    }

    /**
     * Test two callbacks (one for status disabled, one for blocked cookies count) if cookie
     * controls is off.
     */
    @Test
    @SmallTest
    public void testCookieBridgeWithTPCookiesDisabled() throws Exception {
        TestThreadUtils.runOnUiThreadBlocking(() -> {
            // Set CookieControlsMode Pref to Off
            UserPrefs.get(Profile.getLastUsedRegularProfile())
                    .setInteger(PrefNames.COOKIE_CONTROLS_MODE, CookieControlsMode.OFF);
        });
        int currentCallCount = mCallbackHelper.getCallCount();

        // Navigate to a page
        final String url = mTestServer.getURL("/chrome/test/data/android/cookie.html");
        Tab tab = mActivityTestRule.loadUrlInNewTab(url, false);

        // Create cookie bridge and wait for desired callbacks.
        TestThreadUtils.runOnUiThreadBlocking(() -> {
            mCookieControlsBridge =
                    new CookieControlsBridge(mCallbackHandler, tab.getWebContents(), null);
        });

        mCallbackHelper.waitForCallback(currentCallCount, 2);
        assertEquals(CookieControlsStatus.DISABLED, mStatus);
        assertEquals(CookieControlsEnforcement.NO_ENFORCEMENT, mEnforcement);
        assertEquals(0, mAllowedCookies);
        assertEquals(0, mBlockedCookies);
    }

    /**
     * Test two callbacks (one for status enabled, one for blocked cookies count) if cookie controls
     * is on. No cookies trying to be set.
     */
    @Test
    @SmallTest
    public void testCookieBridgeWith3PCookiesEnabled() throws Exception {
        TestThreadUtils.runOnUiThreadBlocking(() -> {
            UserPrefs.get(Profile.getLastUsedRegularProfile())
                    .setInteger(
                            PrefNames.COOKIE_CONTROLS_MODE, CookieControlsMode.BLOCK_THIRD_PARTY);
        });
        int currentCallCount = mCallbackHelper.getCallCount();

        // Navigate to a page
        final String url = mTestServer.getURL("/chrome/test/data/android/cookie.html");
        Tab tab = mActivityTestRule.loadUrlInNewTab(url, false);

        // Create cookie bridge and wait for desired callbacks.
        TestThreadUtils.runOnUiThreadBlocking(() -> {
            mCookieControlsBridge =
                    new CookieControlsBridge(mCallbackHandler, tab.getWebContents(), null);
        });

        mCallbackHelper.waitForCallback(currentCallCount, 2);
        assertEquals(CookieControlsStatus.ENABLED, mStatus);
        assertEquals(CookieControlsEnforcement.NO_ENFORCEMENT, mEnforcement);
        assertEquals(0, mAllowedCookies);
        assertEquals(0, mBlockedCookies);
    }

    /**
     * Test blocked cookies count changes when new cookie tries to be set. Only one callback because
     * status remains enabled.
     */
    @Test
    @SmallTest
    public void testCookieBridgeWithChangingAllowedCookiesCount() throws Exception {
        int currentCallCount = mCallbackHelper.getCallCount();

        // Navigate to a page
        final String url = mTestServer.getURL("/chrome/test/data/android/cookie.html");
        Tab tab = mActivityTestRule.loadUrlInNewTab(url, false);

        // Create cookie bridge and wait for desired callbacks.
        TestThreadUtils.runOnUiThreadBlocking(() -> {
            mCookieControlsBridge =
                    new CookieControlsBridge(mCallbackHandler, tab.getWebContents(), null);
        });

        mCallbackHelper.waitForCallback(currentCallCount, 2);
        assertEquals(CookieControlsStatus.DISABLED, mStatus);
        assertEquals(CookieControlsEnforcement.NO_ENFORCEMENT, mEnforcement);
        assertEquals(0, mAllowedCookies);
        assertEquals(0, mBlockedCookies);

        // Try to set a cookie on the page when cookies are allowed.
        currentCallCount = mCallbackHelper.getCallCount();
        JavaScriptUtils.executeJavaScriptAndWaitForResult(tab.getWebContents(), "setCookie()");
        mCallbackHelper.waitForCallback(currentCallCount, 1);
        assertEquals(1, mAllowedCookies);
        assertEquals(0, mBlockedCookies);
    }

    /**
     * Test blocked cookies count changes when new cookie tries to be set. Only one callback because
     * status remains enabled.
     */
    @Test
    @SmallTest
    public void testCookieBridgeWithChangingBlockedCookiesCount() throws Exception {
        TestThreadUtils.runOnUiThreadBlocking(() -> {
            UserPrefs.get(Profile.getLastUsedRegularProfile())
                    .setInteger(
                            PrefNames.COOKIE_CONTROLS_MODE, CookieControlsMode.BLOCK_THIRD_PARTY);
            // Block all cookies
            WebsitePreferenceBridge.setCategoryEnabled(
                    Profile.getLastUsedRegularProfile(), ContentSettingsType.COOKIES, false);
        });
        int currentCallCount = mCallbackHelper.getCallCount();

        // Navigate to a page
        final String url = mTestServer.getURL("/chrome/test/data/android/cookie.html");
        Tab tab = mActivityTestRule.loadUrlInNewTab(url, false);

        // Create cookie bridge and wait for desired callbacks.
        TestThreadUtils.runOnUiThreadBlocking(() -> {
            mCookieControlsBridge =
                    new CookieControlsBridge(mCallbackHandler, tab.getWebContents(), null);
        });

        mCallbackHelper.waitForCallback(currentCallCount, 2);
        assertEquals(CookieControlsStatus.ENABLED, mStatus);
        assertEquals(CookieControlsEnforcement.NO_ENFORCEMENT, mEnforcement);
        assertEquals(0, mAllowedCookies);
        assertEquals(0, mBlockedCookies);

        // Try to set a cookie on the page when cookies are blocked.
        currentCallCount = mCallbackHelper.getCallCount();
        JavaScriptUtils.executeJavaScriptAndWaitForResult(tab.getWebContents(), "setCookie()");
        mCallbackHelper.waitForCallback(currentCallCount, 1);
        assertEquals(0, mAllowedCookies);
        assertEquals(1, mBlockedCookies);
    }

    /**
     * Test blocked cookies works with CookieControlsMode.INCOGNITO_ONLY.
     */
    @Test
    @SmallTest
    public void testCookieBridgeWithIncognitoSetting() throws Exception {
        TestThreadUtils.runOnUiThreadBlocking(() -> {
            // Set CookieControlsMode Pref to IncognitoOnly
            UserPrefs.get(Profile.getLastUsedRegularProfile())
                    .setInteger(PrefNames.COOKIE_CONTROLS_MODE, CookieControlsMode.INCOGNITO_ONLY);
        });
        int currentCallCount = mCallbackHelper.getCallCount();

        // Navigate to a normal page
        final String url = mTestServer.getURL("/chrome/test/data/android/cookie.html");
        Tab tab = mActivityTestRule.loadUrlInNewTab(url, false);

        // Create cookie bridge and wait for desired callbacks.
        TestThreadUtils.runOnUiThreadBlocking(() -> {
            mCookieControlsBridge =
                    new CookieControlsBridge(mCallbackHandler, tab.getWebContents(), null);
        });

        mCallbackHelper.waitForCallback(currentCallCount, 2);
        assertEquals(CookieControlsStatus.DISABLED, mStatus);
        assertEquals(CookieControlsEnforcement.NO_ENFORCEMENT, mEnforcement);
        assertEquals(0, mAllowedCookies);
        assertEquals(0, mBlockedCookies);

        // Make new incognito page now
        Tab incognitoTab = mActivityTestRule.loadUrlInNewTab(url, true);
        TestThreadUtils.runOnUiThreadBlocking(() -> {
            mCookieControlsBridge = new CookieControlsBridge(mCallbackHandler,
                    incognitoTab.getWebContents(),
                    Profile.fromWebContents(incognitoTab.getWebContents()).getOriginalProfile());
        });
        mCallbackHelper.waitForCallback(currentCallCount, 2);
        assertEquals(CookieControlsStatus.ENABLED, mStatus);
        assertEquals(CookieControlsEnforcement.NO_ENFORCEMENT, mEnforcement);
        assertEquals(0, mAllowedCookies);
        assertEquals(0, mBlockedCookies);
    }
}
