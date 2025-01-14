// Copyright 2020 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.toolbar.menu_button;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.widget.ImageButton;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.chromium.base.supplier.OneshotSupplierImpl;
import org.chromium.base.test.BaseRobolectricTestRunner;
import org.chromium.chrome.browser.browser_controls.BrowserStateBrowserControlsVisibilityDelegate;
import org.chromium.chrome.browser.omaha.UpdateMenuItemHelper;
import org.chromium.chrome.browser.toolbar.ThemeColorProvider;
import org.chromium.chrome.browser.ui.appmenu.AppMenuButtonHelper;
import org.chromium.chrome.browser.ui.appmenu.AppMenuCoordinator;
import org.chromium.chrome.browser.ui.appmenu.AppMenuHandler;
import org.chromium.chrome.browser.ui.appmenu.AppMenuPropertiesDelegate;

/**
 * Unit tests for ToolbarAppMenuManager.
 */
@RunWith(BaseRobolectricTestRunner.class)
public class MenuButtonCoordinatorTest {
    @Mock
    private BrowserStateBrowserControlsVisibilityDelegate mControlsVisibilityDelegate;
    @Mock
    private Activity mActivity;
    @Mock
    private MenuButtonCoordinator.SetFocusFunction mFocusFunction;
    @Mock
    private AppMenuCoordinator mAppMenuCoordinator;
    @Mock
    private AppMenuHandler mAppMenuHandler;
    @Mock
    private AppMenuButtonHelper mAppMenuButtonHelper;
    @Mock
    MenuButton mMenuButton;
    @Mock
    ImageButton mImageButton;
    @Mock
    private AppMenuPropertiesDelegate mAppMenuPropertiesDelegate;
    @Mock
    private UpdateMenuItemHelper mUpdateMenuItemHelper;
    @Mock
    private Runnable mRequestRenderRunnable;
    @Mock
    ThemeColorProvider mThemeColorProvider;

    private UpdateMenuItemHelper.MenuUiState mMenuUiState;
    private OneshotSupplierImpl<AppMenuCoordinator> mAppMenuSupplier;
    private MenuButtonCoordinator mMenuButtonCoordinator;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        doReturn(mAppMenuHandler).when(mAppMenuCoordinator).getAppMenuHandler();
        doReturn(mAppMenuButtonHelper).when(mAppMenuHandler).createAppMenuButtonHelper();
        doReturn(mAppMenuPropertiesDelegate)
                .when(mAppMenuCoordinator)
                .getAppMenuPropertiesDelegate();
        UpdateMenuItemHelper.setInstanceForTesting(mUpdateMenuItemHelper);
        mAppMenuSupplier = new OneshotSupplierImpl<>();
        mMenuUiState = new UpdateMenuItemHelper.MenuUiState();
        doReturn(mMenuUiState).when(mUpdateMenuItemHelper).getUiState();
        doReturn(mMenuButton)
                .when(mActivity)
                .findViewById(org.chromium.chrome.R.id.menu_button_wrapper);
        doReturn(mImageButton).when(mMenuButton).getImageButton();

        mMenuButtonCoordinator = new MenuButtonCoordinator(mAppMenuSupplier,
                mControlsVisibilityDelegate, mActivity, mFocusFunction, mRequestRenderRunnable,
                true,
                () -> false, mThemeColorProvider, org.chromium.chrome.R.id.menu_button_wrapper);
    }

    @Test
    public void testEnterKeyPress() {
        mAppMenuSupplier.set(mAppMenuCoordinator);

        mMenuButtonCoordinator.onEnterKeyPress();
        verify(mAppMenuButtonHelper).onEnterKeyPress(mImageButton);

        mMenuButtonCoordinator.destroy();
        mMenuButtonCoordinator.onEnterKeyPress();
        verify(mAppMenuButtonHelper, times(1)).onEnterKeyPress(mImageButton);
    }
}
