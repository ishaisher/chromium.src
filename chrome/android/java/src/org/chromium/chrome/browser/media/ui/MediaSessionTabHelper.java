// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.media.ui;

import android.content.Intent;
import android.graphics.Bitmap;

import androidx.annotation.VisibleForTesting;

import org.chromium.chrome.R;
import org.chromium.chrome.browser.document.ChromeIntentUtil;
import org.chromium.chrome.browser.profiles.Profile;
import org.chromium.chrome.browser.tab.EmptyTabObserver;
import org.chromium.chrome.browser.tab.Tab;
import org.chromium.chrome.browser.tab.TabObserver;
import org.chromium.chrome.browser.ui.favicon.LargeIconBridge;
import org.chromium.components.browser_ui.media.MediaNotificationImageUtils;
import org.chromium.components.browser_ui.media.MediaNotificationInfo;
import org.chromium.components.browser_ui.media.MediaNotificationManager;
import org.chromium.components.browser_ui.media.MediaSessionHelper;
import org.chromium.content_public.browser.WebContents;

/**
 * A tab helper that wraps {@link MediaSessionHelper} and is responsible for Chrome-specific
 * behavior.
 */
public class MediaSessionTabHelper implements MediaSessionHelper.Delegate {
    private Tab mTab;
    @VisibleForTesting
    MediaSessionHelper mMediaSessionHelper;
    @VisibleForTesting
    LargeIconBridge mLargeIconBridge;

    @VisibleForTesting
    final TabObserver mTabObserver = new EmptyTabObserver() {
        @Override
        public void onContentChanged(Tab tab) {
            assert tab == mTab;
            maybeCreateOrUpdateMediaSessionHelper();
        }

        @Override
        public void onFaviconUpdated(Tab tab, Bitmap icon) {
            assert tab == mTab;
            mMediaSessionHelper.updateFavicon(icon);
        }

        @Override
        public void onDestroyed(Tab tab) {
            assert mTab == tab;

            if (mMediaSessionHelper != null) mMediaSessionHelper.destroy();
            mTab.removeObserver(this);
            mTab = null;
            if (mLargeIconBridge != null) {
                mLargeIconBridge.destroy();
                mLargeIconBridge = null;
            }
        }
    };

    @VisibleForTesting
    MediaSessionTabHelper(Tab tab) {
        mTab = tab;
        mTab.addObserver(mTabObserver);
        maybeCreateOrUpdateMediaSessionHelper();
    }

    private void maybeCreateOrUpdateMediaSessionHelper() {
        if (mMediaSessionHelper != null) {
            mMediaSessionHelper.setWebContents(mTab.getWebContents());
        } else if (mTab.getWebContents() != null) {
            mMediaSessionHelper = new MediaSessionHelper(mTab.getWebContents(), this);
        }
    }

    /**
     * Creates the {@link MediaSessionTabHelper} for the given {@link Tab}.
     * @param tab the tab to attach the helper to.
     */
    public static void createForTab(Tab tab) {
        new MediaSessionTabHelper(tab);
    }

    @Override
    public Intent createBringTabToFrontIntent() {
        return ChromeIntentUtil.createBringTabToFrontIntent(mTab.getId());
    }

    @Override
    public boolean fetchLargeFaviconImage() {
        WebContents webContents = mTab.getWebContents();
        String pageUrl = webContents.getLastCommittedUrl();
        int size = MediaNotificationImageUtils.MINIMAL_MEDIA_IMAGE_SIZE_PX;
        if (mLargeIconBridge == null) {
            mLargeIconBridge = new LargeIconBridge(Profile.fromWebContents(webContents));
        }
        LargeIconBridge.LargeIconCallback callback = new LargeIconBridge.LargeIconCallback() {
            @Override
            public void onLargeIconAvailable(
                    Bitmap icon, int fallbackColor, boolean isFallbackColorDefault, int iconType) {
                mMediaSessionHelper.setLargeIcon(icon);
            }
        };

        return mLargeIconBridge.getLargeIconForStringUrl(pageUrl, size, callback);
    }

    @Override
    public MediaNotificationInfo.Builder createMediaNotificationInfoBuilder() {
        return new MediaNotificationInfo.Builder()
                .setInstanceId(mTab.getId())
                .setId(R.id.media_playback_notification);
    }

    @Override
    public void showMediaNotification(MediaNotificationInfo notificationInfo) {
        ChromeMediaNotificationManager.show(notificationInfo);
    }

    @Override
    public void hideMediaNotification() {
        MediaNotificationManager.hide(mTab.getId(), R.id.media_playback_notification);
    }

    @Override
    public void activateAndroidMediaSession() {
        MediaNotificationManager.activateAndroidMediaSession(
                mTab.getId(), R.id.media_playback_notification);
    }
}
