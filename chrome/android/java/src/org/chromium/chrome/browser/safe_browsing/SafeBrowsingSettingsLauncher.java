// Copyright 2020 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
package org.chromium.chrome.browser.safe_browsing;

import android.content.Context;

import org.chromium.base.annotations.CalledByNative;
import org.chromium.chrome.browser.safe_browsing.metrics.SettingsAccessPoint;
import org.chromium.chrome.browser.safe_browsing.settings.SecuritySettingsFragment;
import org.chromium.chrome.browser.settings.SettingsLauncher;
import org.chromium.chrome.browser.settings.SettingsLauncherImpl;
import org.chromium.content_public.browser.WebContents;
import org.chromium.ui.base.WindowAndroid;

/**
 * Bridge between Java and native SafeBrowsing code to launch the Safe Browsing settings page.
 */
public class SafeBrowsingSettingsLauncher {
    private SafeBrowsingSettingsLauncher() {}

    @CalledByNative
    private static void showSafeBrowsingSettings(WebContents webContents) {
        WindowAndroid window = webContents.getTopLevelNativeWindow();
        if (window == null) return;
        Context currentContext = window.getContext().get();
        SettingsLauncher settingsLauncher = new SettingsLauncherImpl();
        settingsLauncher.launchSettingsActivity(currentContext, SecuritySettingsFragment.class,
                SecuritySettingsFragment.createArguments(
                        SettingsAccessPoint.SECURITY_INTERSTITIAL));
    }
}
