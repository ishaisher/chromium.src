// Copyright 2020 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.toolbar.menu_button;

import android.app.Activity;
import android.view.View.OnKeyListener;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;

import org.chromium.base.supplier.ObservableSupplier;
import org.chromium.base.supplier.OneshotSupplier;
import org.chromium.base.supplier.Supplier;
import org.chromium.chrome.browser.browser_controls.BrowserStateBrowserControlsVisibilityDelegate;
import org.chromium.chrome.browser.toolbar.ThemeColorProvider;
import org.chromium.chrome.browser.toolbar.menu_button.MenuButtonProperties.ShowBadgeProperty;
import org.chromium.chrome.browser.toolbar.menu_button.MenuButtonProperties.ThemeProperty;
import org.chromium.chrome.browser.ui.appmenu.AppMenuButtonHelper;
import org.chromium.chrome.browser.ui.appmenu.AppMenuCoordinator;
import org.chromium.ui.UiUtils;
import org.chromium.ui.modelutil.PropertyModel;
import org.chromium.ui.modelutil.PropertyModelChangeProcessor;

/**
 * Root component for the app menu button on the toolbar. Owns the MenuButton view and handles
 * changes to its visual state, e.g. showing/hiding the app update badge.
 */
public class MenuButtonCoordinator {
    public interface SetFocusFunction {
        void setFocus(boolean focus, int reason);
    }

    private final Activity mActivity;
    private final PropertyModel mPropertyModel;
    private MenuButtonMediator mMediator;
    private AppMenuButtonHelper mAppMenuButtonHelper;
    private MenuButton mMenuButton;
    private PropertyModelChangeProcessor mChangeProcessor;

    /**
     *  @param appMenuCoordinatorSupplier Supplier for the AppMenuCoordinator, which owns all other
     *         app menu MVC components.
     * @param controlsVisibilityDelegate Delegate for forcing persistent display of browser
     *         controls.
     * @param activity Activity in which this object lives.
     * @param setUrlBarFocusFunction Function that allows setting focus on the url bar.
     * @param requestRenderRunnable Runnable that requests a re-rendering of the compositor view
     *         containing the app menu button.
     * @param shouldShowAppUpdateBadge Whether the app menu update badge should be shown if there is
     *         a pending update.
     * @param isInOverviewModeSupplier Supplier of overview mode state.
     * @param themeColorProvider Provider of theme color changes.
     * @param menuButtonId Resource id that should be used to locate the underlying view.
     */
    public MenuButtonCoordinator(OneshotSupplier<AppMenuCoordinator> appMenuCoordinatorSupplier,
            BrowserStateBrowserControlsVisibilityDelegate controlsVisibilityDelegate,
            Activity activity, SetFocusFunction setUrlBarFocusFunction,
            Runnable requestRenderRunnable, boolean shouldShowAppUpdateBadge,
            Supplier<Boolean> isInOverviewModeSupplier, ThemeColorProvider themeColorProvider,
            @IdRes int menuButtonId) {
        mActivity = activity;
        mMenuButton = mActivity.findViewById(menuButtonId);
        mPropertyModel = new PropertyModel.Builder(MenuButtonProperties.ALL_KEYS)
                                 .with(MenuButtonProperties.SHOW_UPDATE_BADGE,
                                         new ShowBadgeProperty(false, false))
                                 .with(MenuButtonProperties.THEME,
                                         new ThemeProperty(themeColorProvider.getTint(),
                                                 themeColorProvider.useLight()))
                                 .with(MenuButtonProperties.IS_VISIBLE, true)
                                 .build();
        mMediator = new MenuButtonMediator(mPropertyModel, shouldShowAppUpdateBadge,
                ()
                        -> mActivity.isFinishing() || mActivity.isDestroyed(),
                requestRenderRunnable, themeColorProvider, isInOverviewModeSupplier,
                controlsVisibilityDelegate, setUrlBarFocusFunction, appMenuCoordinatorSupplier,
                mActivity.getResources());
        mMediator.getMenuButtonHelperSupplier().addObserver(
                (helper) -> mAppMenuButtonHelper = helper);
        if (mMenuButton != null) {
            mChangeProcessor = PropertyModelChangeProcessor.create(
                    mPropertyModel, mMenuButton, new MenuButtonViewBinder());
        }
    }

    /**
     * Update the state of AppMenu components that need to know if the current page is loading, e.g.
     * the stop/reload button.
     * @param isLoading Whether the current page is loading.
     */
    public void updateReloadingState(boolean isLoading) {
        if (mMediator == null) return;
        mMediator.updateReloadingState(isLoading);
    }

    /**
     * Disables the menu button, removing it from the view hierarchy and destroying it.
     */
    public void disableMenuButton() {
        if (mMenuButton != null) {
            UiUtils.removeViewFromParent(mMenuButton);
            destroy();
        }
    }

    /**
     * Set the underlying MenuButton view. Use only if the MenuButton instance isn't available at
     * construction time, e.g. if it's lazily inflated. This should only be called once.
     * @param menuButton The underlying MenuButton view.
     */
    public void setMenuButton(MenuButton menuButton) {
        assert mMenuButton == null;
        assert menuButton != null;
        mMenuButton = menuButton;
        mChangeProcessor = PropertyModelChangeProcessor.create(
                mPropertyModel, menuButton, new MenuButtonViewBinder());
    }

    /**
     * Handle the key press event on the menu button.
     * @return Whether the app menu was shown as a result of this action.
     */
    public boolean onEnterKeyPress() {
        if (mAppMenuButtonHelper == null || mMenuButton == null) return false;
        return mAppMenuButtonHelper.onEnterKeyPress(mMenuButton.getImageButton());
    }

    /**
     * @return Whether the menu button is present and visible.
     */
    public boolean isShown() {
        return mMenuButton != null && mMenuButton.isShown();
    }

    /**
     * Get the underlying MenuButton view. Present for legacy reasons only; don't add new usages.
     */
    @Deprecated
    public MenuButton getMenuButton() {
        return mMenuButton;
    }

    /**
     * @param isClickable Whether the underlying MenuButton view should be clickable.
     */
    public void setClickable(boolean isClickable) {
        if (mMediator == null) return;
        mMediator.setClickable(isClickable);
    }

    /**
     * Sets the on key listener for the underlying menu button.
     * @param onKeyListener Listener for key events.
     */
    public void setOnKeyListener(OnKeyListener onKeyListener) {
        if (mMenuButton == null) return;
        mMenuButton.setOnKeyListener(onKeyListener);
    }

    public void destroy() {
        if (mMediator != null) {
            mMediator.destroy();
            mMediator = null;
        }

        if (mChangeProcessor != null) {
            mChangeProcessor.destroy();
            mChangeProcessor = null;
        }

        mMenuButton = null;
        mAppMenuButtonHelper = null;
    }

    /**
     * Signal to MenuButtonCoordinator that native is initialized and it's safe to access
     * dependencies that require native, e.g. the UpdateMenuItemHelper.
     */
    public void onNativeInitialized() {
        if (mMediator == null) return;
        mMediator.onNativeInitialized();
    }

    @Nullable
    public ObservableSupplier<AppMenuButtonHelper> getMenuButtonHelperSupplier() {
        if (mMediator == null) return null;
        return mMediator.getMenuButtonHelperSupplier();
    }

    /**
     * Suppress or un-suppress display of the "update available" badge.
     * @param isSuppressed
     */
    public void setAppMenuUpdateBadgeSuppressed(boolean isSuppressed) {
        if (mMediator == null) return;
        mMediator.setAppMenuUpdateBadgeSuppressed(isSuppressed);
    }

    /**
     * Set the visibility of the MenuButton controlled by this coordinator.
     * @param visible Visibility state, true for visible and false for hidden.
     */

    public void setVisibility(boolean visible) {
        if (mMediator == null) return;
        mMediator.setVisibility(visible);
    }
}
