// Copyright 2020 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.vr;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.VisibleForTesting;

import com.google.vr.ndk.base.DaydreamApi;

import org.chromium.base.ContextUtils;
import org.chromium.base.Log;
import org.chromium.base.annotations.CalledByNative;
import org.chromium.base.annotations.JNINamespace;
import org.chromium.base.annotations.NativeMethods;
import org.chromium.chrome.browser.infobar.InfoBarIdentifier;
import org.chromium.chrome.browser.tab.Tab;
import org.chromium.chrome.browser.tab.TabUtils;
import org.chromium.chrome.browser.ui.messages.infobar.SimpleConfirmInfoBarBuilder;

/**
 * Manages logic around VrCore Installation and Versioning
 */
@JNINamespace("vr")
public class VrCoreInstallUtils {
    private static final String TAG = "VrCoreInstallUtils";
    // Pseudo-random number to avoid request id collisions. Result codes must fit in lower 16 bits
    // when used with startActivityForResult...
    private static final int VR_SERVICES_UPDATE_RESULT = 7213;

    private static final String VR_CORE_MARKET_URI =
            "market://details?id=" + VrCoreVersionChecker.VR_CORE_PACKAGE_ID;

    // Instance that requested installation of VRCore.
    // Should be non-null only if there is a pending request to install VRCore.
    private static VrCoreInstallUtils sRequestInstallInstance;

    private static VrCoreVersionChecker sVrCoreVersionChecker;
    private static @VrSupportLevel Integer sVrSupportLevel;

    private long mNativeVrCoreInstallUtils;

    @CalledByNative
    @VisibleForTesting
    protected static VrCoreInstallUtils create(long nativeVrCoreInstallUtils) {
        return new VrCoreInstallUtils(nativeVrCoreInstallUtils);
    }

    /**
     * See {@link Activity#onActivityResult}.
     */
    public static boolean onActivityResultWithNative(int requestCode, int resultCode) {
        // Handles the result of requesting to update VR services.
        if (requestCode == VR_SERVICES_UPDATE_RESULT) {
            if (sRequestInstallInstance != null) {
                sRequestInstallInstance.onVrCoreMaybeUpdated();
                sRequestInstallInstance = null;
            }
            return true;
        }
        return false;
    }

    public static boolean isDaydreamReadyDevice() {
        return DaydreamApi.isDaydreamReadyPlatform(ContextUtils.getApplicationContext());
    }

    public static VrCoreVersionChecker getVrCoreVersionChecker() {
        if (sVrCoreVersionChecker == null) sVrCoreVersionChecker = new VrCoreVersionChecker();
        return sVrCoreVersionChecker;
    }

    /**
     * Returns the current {@VrSupportLevel}.
     */
    @CalledByNative
    public static int getVrSupportLevel() {
        if (sVrSupportLevel == null) {
            if (!isVrCoreCompatible()) {
                sVrSupportLevel = VrSupportLevel.VR_NEEDS_UPDATE;
            } else if (isDaydreamReadyDevice()) {
                sVrSupportLevel = VrSupportLevel.VR_DAYDREAM;
            } else {
                sVrSupportLevel = VrSupportLevel.VR_CARDBOARD;
            }
        }
        return sVrSupportLevel;
    }

    /**
     * Returns the @{VrSupportLevel}, if known, without attempting to recalculate.
     */
    public static Integer getCachedVrSupportLevel() {
        return sVrSupportLevel;
    }

    @CalledByNative
    public static boolean vrSupportNeedsUpdate() {
        return getVrSupportLevel() == VrSupportLevel.VR_NEEDS_UPDATE;
    }

    /**
     * Returns whether the device has support for Daydream.
     */
    /* package */ static boolean hasDaydreamSupport() {
        return getVrSupportLevel() == VrSupportLevel.VR_DAYDREAM;
    }

    /**
     * @param versionChecker The VrCoreVersionChecker object this delegate will use
     */
    @VisibleForTesting
    protected static void overrideVrCoreVersionChecker(VrCoreVersionChecker versionChecker) {
        sVrCoreVersionChecker = versionChecker;
        updateVrSupportLevel();
    }

    private static boolean isVrCoreCompatible() {
        VrCoreVersionChecker checker = getVrCoreVersionChecker();
        if (checker == null) return false;
        return checker.getVrCoreCompatibility()
                == VrCoreVersionChecker.VrCoreCompatibility.VR_READY;
    }

    /**
     * Forces a recalculation of the current @{VrSupportLevel} as it may have changed.
     */
    private static int updateVrSupportLevel() {
        sVrSupportLevel = null;
        return getVrSupportLevel();
    }

    private VrCoreInstallUtils(long nativeVrCoreInstallUtils) {
        mNativeVrCoreInstallUtils = nativeVrCoreInstallUtils;
    }

    @CalledByNative
    private void onNativeDestroy() {
        mNativeVrCoreInstallUtils = 0;
    }

    /**
     * Prompts the user to install or update VRSupport if needed.
     */
    @CalledByNative
    @VisibleForTesting
    protected void requestInstallVrCore(final Tab tab) {
        if (tab == null) {
            maybeNotifyNativeOnInstallResult(false);
            return;
        }

        // Force a recalculation of the VrSupportLevel
        updateVrSupportLevel();
        if (!vrSupportNeedsUpdate()) {
            // If we don't need an update, just return that install succeeded.
            maybeNotifyNativeOnInstallResult(true);
            return;
        }

        @VrCoreVersionChecker.VrCoreCompatibility
        int vrCoreCompatibility = getVrCoreVersionChecker().getVrCoreCompatibility();

        String infobarText;
        String buttonText;

        if (vrCoreCompatibility == VrCoreVersionChecker.VrCoreCompatibility.VR_NOT_AVAILABLE) {
            // Supported, but not installed. Ask user to install instead of upgrade.
            infobarText = ContextUtils.getApplicationContext().getString(
                    org.chromium.chrome.vr.R.string.vr_services_check_infobar_install_text);
            buttonText = ContextUtils.getApplicationContext().getString(
                    org.chromium.chrome.vr.R.string.vr_services_check_infobar_install_button);
        } else if (vrCoreCompatibility == VrCoreVersionChecker.VrCoreCompatibility.VR_OUT_OF_DATE) {
            infobarText = ContextUtils.getApplicationContext().getString(
                    org.chromium.chrome.vr.R.string.vr_services_check_infobar_update_text);
            buttonText = ContextUtils.getApplicationContext().getString(
                    org.chromium.chrome.vr.R.string.vr_services_check_infobar_update_button);
        } else {
            Log.e(TAG, "Unknown VrCore compatibility: " + vrCoreCompatibility);
            return;
        }

        final Activity activity = TabUtils.getActivity(tab);
        SimpleConfirmInfoBarBuilder.Listener listener = new SimpleConfirmInfoBarBuilder.Listener() {
            @Override
            public void onInfoBarDismissed() {
                maybeNotifyNativeOnInstallResult(false);
            }

            @Override
            public boolean onInfoBarButtonClicked(boolean isPrimary) {
                assert sRequestInstallInstance == null;
                sRequestInstallInstance = VrCoreInstallUtils.this;
                activity.startActivityForResult(
                        new Intent(Intent.ACTION_VIEW, Uri.parse(VR_CORE_MARKET_URI)),
                        VR_SERVICES_UPDATE_RESULT);
                return false;
            }

            @Override
            public boolean onInfoBarLinkClicked() {
                return false;
            }
        };
        SimpleConfirmInfoBarBuilder.create(tab.getWebContents(), listener,
                InfoBarIdentifier.VR_SERVICES_UPGRADE_ANDROID, activity,
                org.chromium.chrome.vr.R.drawable.vr_services, infobarText, buttonText, null, null,
                true);
    }

    private void onVrCoreMaybeUpdated() {
        maybeNotifyNativeOnInstallResult(updateVrSupportLevel() != VrSupportLevel.VR_NEEDS_UPDATE);
    }

    /**
     * Helper used to notify native code about the result of the request to install VRCore.
     */
    private void maybeNotifyNativeOnInstallResult(boolean success) {
        if (mNativeVrCoreInstallUtils != 0) {
            VrCoreInstallUtilsJni.get().onInstallResult(mNativeVrCoreInstallUtils, success);
        }
    }

    @NativeMethods
    interface Natives {
        void onInstallResult(long nativeVrCoreInstallHelper, boolean success);
    }
}
