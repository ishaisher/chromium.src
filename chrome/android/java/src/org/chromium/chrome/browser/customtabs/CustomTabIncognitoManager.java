// Copyright 2019 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.customtabs;

import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.chromium.base.CommandLine;
import org.chromium.base.UnownedUserData;
import org.chromium.base.UnownedUserDataKey;
import org.chromium.base.annotations.CheckDiscard;
import org.chromium.chrome.browser.app.ChromeActivity;
import org.chromium.chrome.browser.browserservices.BrowserServicesIntentDataProvider;
import org.chromium.chrome.browser.customtabs.content.CustomTabActivityNavigationController;
import org.chromium.chrome.browser.customtabs.content.CustomTabActivityTabProvider;
import org.chromium.chrome.browser.dependency_injection.ActivityScope;
import org.chromium.chrome.browser.flags.ChromeFeatureList;
import org.chromium.chrome.browser.flags.ChromeSwitches;
import org.chromium.chrome.browser.lifecycle.ActivityLifecycleDispatcher;
import org.chromium.chrome.browser.lifecycle.Destroyable;
import org.chromium.chrome.browser.lifecycle.NativeInitObserver;
import org.chromium.chrome.browser.profiles.OTRProfileID;
import org.chromium.chrome.browser.profiles.Profile;
import org.chromium.chrome.browser.tabmodel.IncognitoTabHost;
import org.chromium.chrome.browser.tabmodel.IncognitoTabHostRegistry;
import org.chromium.ui.base.WindowAndroid;

import javax.inject.Inject;

/**
 * Implements incognito tab host for the given instance of Custom Tab activity.
 * This class exists for every custom tab, but its only active if
 * |isEnabledIncognitoCCT| returns true.
 */
@ActivityScope
public class CustomTabIncognitoManager implements NativeInitObserver, Destroyable, UnownedUserData {
    private static CustomTabIncognitoManager sCustomTabIncognitoManagerUsedForTesting;

    private static final String TAG = "CctIncognito";
    /** The key for accessing this object on an {@link org.chromium.base.UnownedUserDataHost}. */
    private static final UnownedUserDataKey<CustomTabIncognitoManager> KEY =
            new UnownedUserDataKey<>(CustomTabIncognitoManager.class);

    private final ChromeActivity<?> mChromeActivity;
    private final CustomTabActivityNavigationController mNavigationController;
    private final BrowserServicesIntentDataProvider mIntentDataProvider;
    private final CustomTabActivityTabProvider mTabProvider;
    private final WindowAndroid mWindowAndroid;

    private OTRProfileID mOTRProfileID;

    @Nullable
    private IncognitoTabHost mIncognitoTabHost;

    @Inject
    public CustomTabIncognitoManager(ChromeActivity<?> customTabActivity,
            WindowAndroid windowAndroid, BrowserServicesIntentDataProvider intentDataProvider,
            CustomTabActivityNavigationController navigationController,
            CustomTabActivityTabProvider tabProvider,
            ActivityLifecycleDispatcher lifecycleDispatcher) {
        mChromeActivity = customTabActivity;
        mWindowAndroid = windowAndroid;
        mIntentDataProvider = intentDataProvider;
        mNavigationController = navigationController;
        mTabProvider = tabProvider;

        lifecycleDispatcher.register(this);

        attach(mWindowAndroid, this);
    }

    @CheckDiscard("Test-only setter.")
    @VisibleForTesting
    public static void setCustomTabIncognitoManagerUsedForTesting(
            CustomTabIncognitoManager customTabIncognitoManager) {
        sCustomTabIncognitoManagerUsedForTesting = customTabIncognitoManager;
    }

    /**
     * Get the Activity's {@link CustomTabIncognitoManager} from the provided {@link
     * WindowAndroid}.
     * @param window The window to get the manager from.
     * @return The Activity's {@link CustomTabIncognitoManager}.
     */
    public static @Nullable CustomTabIncognitoManager from(WindowAndroid window) {
        if (sCustomTabIncognitoManagerUsedForTesting != null) {
            return sCustomTabIncognitoManagerUsedForTesting;
        }

        return KEY.retrieveDataFromHost(window.getUnownedUserDataHost());
    }

    /**
     * Make this instance of CustomTabIncognitoManager available through the activity's window.
     * @param window A {@link WindowAndroid} to attach to.
     * @param manager The {@link CustomTabIncognitoManager} to attach.
     */
    private static void attach(WindowAndroid window, CustomTabIncognitoManager manager) {
        KEY.attachToHost(window.getUnownedUserDataHost(), manager);
    }

    /**
     * Detach the provided CustomTabIncognitoManager from any host it is associated with.
     * @param manager The {@link CustomTabIncognitoManager} to detach.
     */
    private static void detach(CustomTabIncognitoManager manager) {
        KEY.detachFromAllHosts(manager);
    }

    public boolean isEnabledIncognitoCCT() {
        return mIntentDataProvider.isIncognito()
                && ChromeFeatureList.isEnabled(ChromeFeatureList.CCT_INCOGNITO);
    }

    public Profile getProfile() {
        if (mOTRProfileID == null) mOTRProfileID = OTRProfileID.createUnique("CCT:Incognito");
        return Profile.getLastUsedRegularProfile().getOffTheRecordProfile(mOTRProfileID);
    }

    @Override
    public void onFinishNativeInitialization() {
        if (isEnabledIncognitoCCT()) {
            initializeIncognito();
        }
    }

    @Override
    public void destroy() {
        if (mIncognitoTabHost != null) {
            IncognitoTabHostRegistry.getInstance().unregister(mIncognitoTabHost);
        }

        if (mOTRProfileID != null) {
            Profile.getLastUsedRegularProfile()
                    .getOffTheRecordProfile(mOTRProfileID)
                    .destroyWhenAppropriate();
            mOTRProfileID = null;
        }

        detach(this);
    }

    private void initializeIncognito() {
        mIncognitoTabHost = new IncognitoCustomTabHost();
        IncognitoTabHostRegistry.getInstance().register(mIncognitoTabHost);
        if (!CommandLine.getInstance().hasSwitch(
                    ChromeSwitches.ENABLE_INCOGNITO_SNAPSHOTS_IN_ANDROID_RECENTS)) {
            // Disable taking screenshots and seeing snapshots in recents.
            mChromeActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    private class IncognitoCustomTabHost implements IncognitoTabHost {
        public IncognitoCustomTabHost() {
            assert mIntentDataProvider.isIncognito();
        }
        @Override
        public boolean hasIncognitoTabs() {
            return !mChromeActivity.isFinishing();
        }
        @Override
        public void closeAllIncognitoTabs() {
            mNavigationController.finish(CustomTabActivityNavigationController.FinishReason.OTHER);
        }
    }
}
