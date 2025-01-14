// Copyright 2020 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
package org.chromium.chrome.browser.suggestions.tile;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;

import org.chromium.ui.modelutil.PropertyModel;
import org.chromium.ui.modelutil.PropertyModelChangeProcessor;

/**
 * Coordinator for the TileView.
 */
public class TileViewCoordinator {
    private final TileView mView;
    private final TileViewMediator mMediator;

    public TileViewCoordinator(Context context, @LayoutRes int tileLayoutRes, ViewGroup parent) {
        mView = (TileView) LayoutInflater.from(context).inflate(tileLayoutRes, parent, false);
        PropertyModel model = new PropertyModel();
        PropertyModelChangeProcessor.create(model, mView, TileViewBinder::bind);
        mMediator = new TileViewMediator(model);
    }

    /**
     * Set the Tile Title.
     * The title will be shown in at most TITLE_LINES lines.
     *
     * @param title Title to be displayed.
     */
    public void setTitle(String title) {
        mMediator.setTitle(title);
    }

    /**
     * Set the max number of lines for the TextView showing the TITLE.
     *
     * @param lines Maximum number of lines that can be used to present the Title.
     */
    public void setTitleLines(int lines) {
        mMediator.setTitleLines(lines);
    }

    /**
     * Set the Tile Icon.
     *
     * @param icon Icon to show within the tile.
     */
    public void setIcon(Drawable icon) {
        mMediator.setIcon(icon);
    }

    /**
     * Set whether the Icon Badge should be visible.
     *
     * @param badgeVisible Whether icon badge should be visible.
     */
    public void setBadgeVisible(boolean badgeVisible) {
        mMediator.setBadgeVisible(badgeVisible);
    }

    /**
     * Set whether Tile should present a large Icon.
     *
     * @param showLargeIcon Whether Tile Icon should be large.
     */
    public void setShowLargeIcon(boolean showLargeIcon) {
        mMediator.setShowLargeIcon(showLargeIcon);
    }

    /**
     * Set the handler receiving click events.
     *
     * @param listener Handler receiving click events.
     */
    public void setOnClickListener(View.OnClickListener listener) {
        mMediator.setOnClickListener(listener);
    }

    /**
     * Set the handler receiving long click events.
     *
     * @param listener Handler receiving long click events.
     */
    public void setOnLongClickListener(View.OnLongClickListener listener) {
        mMediator.setOnLongClickListener(listener);
    }

    /**
     * Set the handler receiving context menu create events.
     *
     * @param listener Handler receiving context menu create events.
     */
    public void setOnCreateContextMenuListener(View.OnCreateContextMenuListener listener) {
        mMediator.setOnCreateContextMenuListener(listener);
    }
}