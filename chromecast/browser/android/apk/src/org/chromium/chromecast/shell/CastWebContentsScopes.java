// Copyright 2018 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chromecast.shell;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.IBinder;
import android.widget.FrameLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

import org.chromium.base.supplier.Supplier;
import org.chromium.chromecast.base.Observer;
import org.chromium.components.embedder_support.view.ContentView;
import org.chromium.components.embedder_support.view.ContentViewRenderView;
import org.chromium.content_public.browser.WebContents;
import org.chromium.ui.base.ActivityWindowAndroid;
import org.chromium.ui.base.WindowAndroid;

class CastWebContentsScopes {
    interface WindowTokenProvider {
        @Nullable
        IBinder provideWindowToken();
    }

    static final String VIEW_TAG_CONTENT_VIEW = "ContentView";

    public static Observer<WebContents> onLayoutActivity(
            Activity activity, FrameLayout layout, @ColorInt int backgroundColor) {
        layout.setBackgroundColor(backgroundColor);
        return onLayoutInternal(
                activity, layout, () -> new ActivityWindowAndroid(activity), backgroundColor);
    }

    public static Observer<WebContents> onLayoutFragment(
            Activity activity, FrameLayout layout, @ColorInt int backgroundColor) {
        layout.setBackgroundColor(backgroundColor);
        return onLayoutInternal(
                activity, layout, () -> new WindowAndroid(activity), backgroundColor);
    }

    static Observer<WebContents> onLayoutView(Context context, FrameLayout layout,
            @ColorInt int backgroundColor, WindowTokenProvider windowTokenProvider) {
        layout.setBackgroundColor(backgroundColor);
        return onLayoutInternal(context, layout, () -> new WindowAndroid(context) {
            @Override
            protected IBinder getWindowToken() {
                return windowTokenProvider.provideWindowToken();
            }
        }, backgroundColor);
    }

    // Note: the |windowFactory| should create a new instance of a WindowAndroid each time it is
    // invoked.
    private static Observer<WebContents> onLayoutInternal(Context context, FrameLayout layout,
            Supplier<WindowAndroid> windowFactory, @ColorInt int backgroundColor) {
        return (WebContents webContents) -> {
            WindowAndroid window = windowFactory.get();
            ContentViewRenderView contentViewRenderView = new ContentViewRenderView(context) {
                @Override
                protected void onReadyToRender() {
                    setOverlayVideoMode(true);
                }
            };
            contentViewRenderView.onNativeLibraryLoaded(window);
            contentViewRenderView.setSurfaceViewBackgroundColor(backgroundColor);
            FrameLayout.LayoutParams matchParent = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
            layout.addView(contentViewRenderView, matchParent);

            ContentView contentView = ContentView.createContentView(
                    context, null /* eventOffsetHandler */, webContents);
            WebContentsRegistry.initializeWebContents(webContents, contentView, window);

            // Enable display of current webContents.
            webContents.onShow();
            layout.addView(contentView, matchParent);
            contentView.setFocusable(true);
            contentView.requestFocus();
            contentView.setTag(VIEW_TAG_CONTENT_VIEW);
            contentViewRenderView.setCurrentWebContents(webContents);
            return () -> {
                layout.setForeground(new ColorDrawable(backgroundColor));
                layout.removeView(contentView);
                layout.removeView(contentViewRenderView);
                webContents.setTopLevelNativeWindow(null);
                contentViewRenderView.destroy();
                window.destroy();
            };
        };
    }

    public static Observer<WebContents> withoutLayout(Context context) {
        return (WebContents webContents) -> {
            WindowAndroid window = new WindowAndroid(context);
            ContentView contentView = ContentView.createContentView(
                    context, null /* eventOffsetHandler */, webContents);
            WebContentsRegistry.initializeWebContents(webContents, contentView, window);
            // Enable display of current webContents.
            webContents.onShow();
            return () -> {
                if (!webContents.isDestroyed()) {
                    // WebContents can be destroyed by the app before CastWebContentsComponent
                    // unbinds, which is why we need this check.
                    webContents.onHide();

                    if (webContents.getTopLevelNativeWindow() == window) {
                        webContents.setTopLevelNativeWindow(null);
                    }
                }
                window.destroy();
            };
        };
    }
}
