// Copyright 2018 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.incognito;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.chromium.base.ApplicationStatus;
import org.chromium.base.ContextUtils;
import org.chromium.base.IntentUtils;
import org.chromium.base.annotations.NativeMethods;
import org.chromium.chrome.browser.ChromeTabbedActivity;
import org.chromium.chrome.browser.IntentHandler;
import org.chromium.chrome.browser.customtabs.CustomTabIncognitoManager;
import org.chromium.chrome.browser.profiles.Profile;
import org.chromium.chrome.browser.tab.TabStateFileManager;
import org.chromium.chrome.browser.tabmodel.IncognitoTabHost;
import org.chromium.chrome.browser.tabmodel.IncognitoTabHostRegistry;
import org.chromium.chrome.browser.tabpersistence.TabStateDirectory;
import org.chromium.chrome.browser.util.AndroidTaskUtils;
import org.chromium.ui.base.WindowAndroid;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Utilities for working with incognito tabs spread across multiple activities.
 */
public class IncognitoUtils {
    private static Boolean sIsEnabledForTesting;

    private IncognitoUtils() {}

    /**
     * Determine whether the incognito profile needs to be destroyed as part of startup.  This is
     * only needed on L+ when it is possible to swipe away tasks from Android recents without
     * killing the process.  When this occurs, the normal incognito profile shutdown does not
     * happen, which can leave behind incognito cookies from an existing session.
     */
    @SuppressLint("NewApi")
    public static boolean shouldDestroyIncognitoProfileOnStartup(
            boolean selectedTabModelIsIncognito) {
        if (!Profile.getLastUsedRegularProfile().hasOffTheRecordProfile()) {
            return false;
        }

        Context context = ContextUtils.getApplicationContext();
        ActivityManager manager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        Set<Integer> tabbedModeTaskIds = new HashSet<>();
        for (ActivityManager.AppTask task : manager.getAppTasks()) {
            ActivityManager.RecentTaskInfo info = AndroidTaskUtils.getTaskInfoFromTask(task);
            if (info == null) continue;
            String componentName = AndroidTaskUtils.getTaskComponentName(task);

            if (ChromeTabbedActivity.isTabbedModeComponentName(componentName)) {
                tabbedModeTaskIds.add(info.id);
            }
        }

        if (tabbedModeTaskIds.size() == 0) {
            return true;
        }

        for (Activity activity : ApplicationStatus.getRunningActivities()) {
            tabbedModeTaskIds.remove(activity.getTaskId());
        }

        // If all tabbed mode tasks listed in Android recents are alive, check to see if
        // any incognito tabs exist and the current tab model isn't incognito. If so, we should
        // destroy the incognito profile; otherwise it's not safe to do so yet.
        if (tabbedModeTaskIds.size() == 0) {
            return !(doIncognitoTabsExist() || selectedTabModelIsIncognito);
        }

        // In this case, we have tabbed mode activities listed in recents that do not have an
        // active running activity associated with them.  We can not accurately get an incognito
        // tab count as we do not know if any incognito tabs are associated with the yet unrestored
        // tabbed mode.  Thus we do not proactively destroy the incognito profile.
        return false;
    }

    /**
     * Determine whether there are any incognito tabs.
     */
    public static boolean doIncognitoTabsExist() {
        for (IncognitoTabHost host : IncognitoTabHostRegistry.getInstance().getHosts()) {
            if (host.hasIncognitoTabs()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Closes all incognito tabs.
     */
    public static void closeAllIncognitoTabs() {
        for (IncognitoTabHost host : IncognitoTabHostRegistry.getInstance().getHosts()) {
            host.closeAllIncognitoTabs();
        }
    }

    /**
     * Deletes files with saved state of incognito tabs.
     * @return whether successful.
     */
    public static boolean deleteIncognitoStateFiles() {
        File directory = TabStateDirectory.getOrCreateTabbedModeStateDirectory();
        File[] tabStateFiles = directory.listFiles();
        if (tabStateFiles == null) return true;

        boolean deletionSuccessful = true;
        for (File file : tabStateFiles) {
            Pair<Integer, Boolean> tabInfo =
                    TabStateFileManager.parseInfoFromFilename(file.getName());
            boolean isIncognito = tabInfo != null && tabInfo.second;
            if (isIncognito) {
                deletionSuccessful &= file.delete();
            }
        }
        return deletionSuccessful;
    }

    /**
     * @return true if incognito mode is enabled.
     */
    public static boolean isIncognitoModeEnabled() {
        if (sIsEnabledForTesting != null) {
            return sIsEnabledForTesting;
        }
        return IncognitoUtilsJni.get().getIncognitoModeEnabled();
    }

    /**
     * @return true if incognito mode is managed by policy.
     */
    public static boolean isIncognitoModeManaged() {
        return IncognitoUtilsJni.get().getIncognitoModeManaged();
    }

    /**
     * Whether intent has any extra that indicates an incognito tab will be launched.
     * @param intent A non-null intent
     * @return True if there is any incognito related extra, otherwise return false.
     */
    public static boolean hasAnyIncognitoExtra(@NonNull Intent intent) {
        return IntentUtils.safeGetBooleanExtra(intent, IntentHandler.EXTRA_INCOGNITO_MODE, false)
                || IntentUtils.safeGetBooleanExtra(
                        intent, IntentHandler.EXTRA_OPEN_NEW_INCOGNITO_TAB, false)
                || IntentUtils.safeGetBooleanExtra(
                        intent, IntentHandler.EXTRA_INVOKED_FROM_LAUNCH_NEW_INCOGNITO_TAB, false);
    }

    /**
     * Returns the non primary OTR profile if any that is associated with a |windowAndroid|
     * instance, otherwise null.
     * <p>
     * A non primary OTR profile is associated only for the case of incognito CustomTabActivity.
     * <p>
     * @param windowAndroid The {@link WindowAndroid} instance for which the non primary OTR
     *         profile is queried.
     */
    public static @Nullable Profile getNonPrimaryOTRProfileFromWindowAndroid(
            @Nullable WindowAndroid windowAndroid) {
        if (windowAndroid == null) return null;

        CustomTabIncognitoManager customTabIncognitoManager =
                CustomTabIncognitoManager.from(windowAndroid);

        if (customTabIncognitoManager == null) return null;
        return customTabIncognitoManager.getProfile();
    }

    @VisibleForTesting
    public static void setEnabledForTesting(Boolean enabled) {
        sIsEnabledForTesting = enabled;
    }

    @NativeMethods
    interface Natives {
        boolean getIncognitoModeEnabled();
        boolean getIncognitoModeManaged();
    }
}
