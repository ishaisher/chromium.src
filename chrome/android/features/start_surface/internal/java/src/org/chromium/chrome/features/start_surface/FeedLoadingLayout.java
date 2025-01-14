// Copyright 2020 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.features.start_surface;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.chromium.chrome.browser.feed.shared.FeedFeatures;
import org.chromium.chrome.start_surface.R;
import org.chromium.components.browser_ui.widget.displaystyle.HorizontalDisplayStyle;
import org.chromium.components.browser_ui.widget.displaystyle.UiConfig;
import org.chromium.components.browser_ui.widget.displaystyle.ViewResizer;

/**
 * A {@link LinearLayout} that shows loading placeholder for Feed cards with thumbnail on the right.
 */
public class FeedLoadingLayout extends LinearLayout {
    private static final int CARD_MARGIN_DP = 12;
    private static final int CARD_TOP_PADDING_DP = 15;
    private static final int IMAGE_PLACEHOLDER_BOTTOM_PADDING_DP = 72;
    private static final int IMAGE_PLACEHOLDER_BOTTOM_PADDING_DENSE_DP = 48;
    private static final int IMAGE_PLACEHOLDER_SIZE_DP = 92;
    private static final int TEXT_CONTENT_HEIGHT_DP = 80;
    private static final int TEXT_PLACEHOLDER_HEIGHT_DP = 20;
    private static final int TEXT_PLACEHOLDER_RADIUS_DP = 12;
    private static final int LARGE_IMAGE_HEIGHT_DP = 207;

    private final Context mContext;
    private final Resources mResources;
    private long mLayoutInflationCompleteMs;
    private int mScreenWidthDp;
    private int mPaddingPx;
    private boolean mIsFirstCardDense;

    public FeedLoadingLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mResources = mContext.getResources();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setPlaceholders();
        mLayoutInflationCompleteMs = SystemClock.elapsedRealtime();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setPlaceholders();
    }

    /**
     * Set the header blank for the placeholder.The header blank should be consistent with the
     * sectionHeaderView of {@link ExploreSurfaceCoordinator.FeedSurfaceCreator#}
     */
    @SuppressLint("InflateParams")
    private void setHeader() {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View header;
        // FeedFeatures.cachedIsReportingUserActions uses CachedFeatureFlags for checking feature
        // states, but these same features are checked directly with ChromeFeatureList in other
        // places. Using the cached check here is deliberate for pre-native usage. This
        // inconsistency is fine because the check here is for the Feed header blank size, the
        // mismatch is bearable and only once for every change.
        if (FeedFeatures.cachedIsReportingUserActions()) {
            header = inflater.inflate(
                    R.layout.new_tab_page_snippets_expandable_header_with_menu, null, false);
            header.findViewById(R.id.header_menu).setVisibility(INVISIBLE);
        } else {
            header = inflater.inflate(R.layout.ss_feed_header, null, false);
        }
        LinearLayout headerView = findViewById(R.id.feed_placeholder_header);
        headerView.addView(header);
    }

    private void setPlaceholders() {
        setHeader();
        setPadding();
        LinearLayout cardsParentView = findViewById(R.id.placeholders_layout);
        cardsParentView.removeAllViews();

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dpToPx(CARD_MARGIN_DP));

        // Set the First placeholder container - an image-right card.
        // If it's in landscape mode, the placeholder should always show in dense mode. Otherwise,
        // whether the placeholder is dense depends on whether the first article card of Feed is
        // dense.
        boolean isLandscape = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        mIsFirstCardDense = isLandscape || StartSurfaceConfiguration.isFeedPlaceholderDense();
        setPlaceholders(cardsParentView, true, lp);

        // Set the second and the third placeholder containers - the large image on the top.
        setPlaceholders(cardsParentView, false, lp);
        setPlaceholders(cardsParentView, false, lp);
    }

    private void setPlaceholders(
            LinearLayout parent, boolean isSmallCard, ViewGroup.LayoutParams lp) {
        LinearLayout container = new LinearLayout(mContext);
        container.setLayoutParams(lp);
        container.setOrientation(isSmallCard ? HORIZONTAL : VERTICAL);
        ImageView imagePlaceholder = getImagePlaceholder(isSmallCard);
        ImageView textPlaceholder = getTextPlaceholder(isSmallCard);
        container.addView(isSmallCard ? textPlaceholder : imagePlaceholder);
        container.addView(isSmallCard ? imagePlaceholder : textPlaceholder);
        parent.addView(container);
    }

    private ImageView getImagePlaceholder(boolean isSmallCard) {
        LinearLayout.LayoutParams imagePlaceholderLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ImageView imagePlaceholder = new ImageView(mContext);
        imagePlaceholder.setImageDrawable(
                isSmallCard ? getSmallImageDrawable() : getLargeImageDrawable());
        imagePlaceholder.setLayoutParams(imagePlaceholderLp);
        imagePlaceholder.setScaleType(ImageView.ScaleType.FIT_XY);
        return imagePlaceholder;
    }

    private LayerDrawable getSmallImageDrawable() {
        int imageSize = dpToPx(IMAGE_PLACEHOLDER_SIZE_DP);
        int top = dpToPx(CARD_TOP_PADDING_DP);
        GradientDrawable[] placeholder = getRectangles(1, imageSize, imageSize);
        LayerDrawable layerDrawable = new LayerDrawable(placeholder);
        layerDrawable.setLayerInset(0, 0, top, 0,
                mIsFirstCardDense ? dpToPx(IMAGE_PLACEHOLDER_BOTTOM_PADDING_DENSE_DP)
                                  : dpToPx(IMAGE_PLACEHOLDER_BOTTOM_PADDING_DP));
        return layerDrawable;
    }

    private LayerDrawable getLargeImageDrawable() {
        GradientDrawable[] placeholder = getRectangles(
                1, dpToPx(mScreenWidthDp) - mPaddingPx * 2, dpToPx(LARGE_IMAGE_HEIGHT_DP));
        return new LayerDrawable(placeholder);
    }

    private ImageView getTextPlaceholder(boolean isSmallCard) {
        int top = dpToPx(CARD_TOP_PADDING_DP);
        int left = top / 2;
        int height = dpToPx(TEXT_PLACEHOLDER_HEIGHT_DP);
        int width = dpToPx(mScreenWidthDp) - mPaddingPx * 2;
        int contentHeight = dpToPx(TEXT_CONTENT_HEIGHT_DP);

        LinearLayout.LayoutParams textPlaceholderLp = isSmallCard
                ? new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1)
                : new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        LayerDrawable layerDrawable = isSmallCard
                ? getSmallTextDrawable(top,
                        width - dpToPx(IMAGE_PLACEHOLDER_SIZE_DP) - dpToPx(CARD_TOP_PADDING_DP),
                        height, contentHeight)
                : getLargeTextDrawable(top, left, width, height, contentHeight + 2 * top);

        ImageView textPlaceholder = new ImageView(mContext);
        textPlaceholder.setImageDrawable(layerDrawable);
        textPlaceholder.setLayoutParams(textPlaceholderLp);
        textPlaceholder.setScaleType(ImageView.ScaleType.FIT_XY);
        return textPlaceholder;
    }

    private LayerDrawable getSmallTextDrawable(int top, int width, int height, int contentHeight) {
        GradientDrawable[] placeholders = getRectangles(4, width, height);
        int cardHeight = dpToPx(IMAGE_PLACEHOLDER_SIZE_DP) + dpToPx(CARD_TOP_PADDING_DP)
                + (mIsFirstCardDense ? dpToPx(IMAGE_PLACEHOLDER_BOTTOM_PADDING_DENSE_DP)
                                     : dpToPx(IMAGE_PLACEHOLDER_BOTTOM_PADDING_DP));
        LayerDrawable layerDrawable = new LayerDrawable(placeholders);
        // Title Placeholder
        layerDrawable.setLayerInset(0, 0, top, top, cardHeight - top - height);
        // Content Placeholder
        layerDrawable.setLayerInset(1, 0, (contentHeight - height) / 2 + top, top,
                cardHeight - top - (height + contentHeight) / 2);
        layerDrawable.setLayerInset(
                2, 0, top + contentHeight - height, top, cardHeight - top - contentHeight);
        // Publisher Placeholder
        layerDrawable.setLayerInset(3, 0, cardHeight - top - height, top * 7, top);
        return layerDrawable;
    }

    private LayerDrawable getLargeTextDrawable(
            int top, int left, int width, int height, int contentHeight) {
        GradientDrawable[] placeholders = getRectangles(3, width, height);
        LayerDrawable layerDrawable = new LayerDrawable(placeholders);
        layerDrawable.setLayerInset(0, left, top, top, contentHeight - top - height);
        layerDrawable.setLayerInset(
                1, left, (contentHeight - height) / 2, top, (contentHeight - height) / 2);
        layerDrawable.setLayerInset(2, left, contentHeight - top - height, top, top);
        return layerDrawable;
    }

    private GradientDrawable[] getRectangles(int num, int width, int height) {
        GradientDrawable[] placeholders = new GradientDrawable[num];
        int radius = dpToPx(TEXT_PLACEHOLDER_RADIUS_DP);
        for (int i = 0; i < num; i++) {
            placeholders[i] = new GradientDrawable();
            placeholders[i].setShape(GradientDrawable.RECTANGLE);
            placeholders[i].setSize(width, height);
            placeholders[i].setCornerRadius(radius);
            placeholders[i].setColor(mResources.getColor(R.color.feed_placeholder_color));
        }
        return placeholders;
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    /**
     * Make the padding of placeholder consistent with that of native articles recyclerview which
     * is resized by {@link ViewResizer} in {@link FeedLoadingCoordinator}
     */
    private void setPadding() {
        int defaultPadding =
                mResources.getDimensionPixelSize(R.dimen.content_suggestions_card_modern_margin);
        UiConfig uiConfig = new UiConfig(this);
        // mUiConfig.getContext().getResources() is used here instead of mView.getResources()
        // because lemon compression, somehow, causes the resources to return a different
        // configuration.
        Resources resources = uiConfig.getContext().getResources();
        mScreenWidthDp = resources.getConfiguration().screenWidthDp;

        if (uiConfig.getCurrentDisplayStyle().horizontal == HorizontalDisplayStyle.WIDE) {
            mPaddingPx = computePadding();
        } else {
            mPaddingPx = defaultPadding;
        }
        setPaddingRelative(mPaddingPx, 0, mPaddingPx, 0);
    }

    private int computePadding() {
        int widePadding = mResources.getDimensionPixelSize(R.dimen.ntp_wide_card_lateral_margins);
        int padding =
                dpToPx((int) ((mScreenWidthDp - UiConfig.WIDE_DISPLAY_STYLE_MIN_WIDTH_DP) / 2.f));
        padding = Math.max(widePadding, padding);

        return padding;
    }

    long getLayoutInflationCompleteMs() {
        return mLayoutInflationCompleteMs;
    }
}
