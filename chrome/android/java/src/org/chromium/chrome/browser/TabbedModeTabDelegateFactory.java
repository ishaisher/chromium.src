// Copyright 2019 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser;

import org.chromium.base.supplier.Supplier;
import org.chromium.chrome.browser.app.ChromeActivity;
import org.chromium.chrome.browser.app.tab_activity_glue.ActivityTabWebContentsDelegateAndroid;
import org.chromium.chrome.browser.compositor.bottombar.ephemeraltab.EphemeralTabCoordinator;
import org.chromium.chrome.browser.contextmenu.ChromeContextMenuPopulator;
import org.chromium.chrome.browser.contextmenu.ChromeContextMenuPopulatorFactory;
import org.chromium.chrome.browser.contextmenu.ContextMenuPopulatorFactory;
import org.chromium.chrome.browser.externalnav.ExternalNavigationDelegateImpl;
import org.chromium.chrome.browser.native_page.NativePageFactory;
import org.chromium.chrome.browser.share.ShareDelegate;
import org.chromium.chrome.browser.tab.Tab;
import org.chromium.chrome.browser.tab.TabContextMenuItemDelegate;
import org.chromium.chrome.browser.tab.TabDelegateFactory;
import org.chromium.chrome.browser.tab.TabStateBrowserControlsVisibilityDelegate;
import org.chromium.chrome.browser.tab.TabWebContentsDelegateAndroid;
import org.chromium.chrome.browser.ui.native_page.NativePage;
import org.chromium.components.browser_ui.bottomsheet.BottomSheetController;
import org.chromium.components.browser_ui.util.BrowserControlsVisibilityDelegate;
import org.chromium.components.browser_ui.util.ComposedBrowserControlsVisibilityDelegate;
import org.chromium.components.external_intents.ExternalNavigationHandler;

/**
 * {@link TabDelegateFactory} class to be used in all {@link Tab} instances owned by a
 * {@link ChromeTabbedActivity}.
 */
public class TabbedModeTabDelegateFactory implements TabDelegateFactory {
    private final ChromeActivity mActivity;
    private final BrowserControlsVisibilityDelegate mAppBrowserControlsVisibilityDelegate;
    private final Supplier<ShareDelegate> mShareDelegateSupplier;
    private final Supplier<EphemeralTabCoordinator> mEphemeralTabCoordinatorSupplier;
    private final BottomSheetController mBottomSheetController;
    private NativePageFactory mNativePageFactory;

    public TabbedModeTabDelegateFactory(ChromeActivity activity,
            BrowserControlsVisibilityDelegate appBrowserControlsVisibilityDelegate,
            Supplier<ShareDelegate> shareDelegateSupplier,
            Supplier<EphemeralTabCoordinator> ephemeralTabCoordinatorSupplier,
            BottomSheetController sheetController) {
        mActivity = activity;
        mAppBrowserControlsVisibilityDelegate = appBrowserControlsVisibilityDelegate;
        mShareDelegateSupplier = shareDelegateSupplier;
        mEphemeralTabCoordinatorSupplier = ephemeralTabCoordinatorSupplier;
        mBottomSheetController = sheetController;
    }

    @Override
    public TabWebContentsDelegateAndroid createWebContentsDelegate(Tab tab) {
        return new ActivityTabWebContentsDelegateAndroid(tab, mActivity);
    }

    @Override
    public ExternalNavigationHandler createExternalNavigationHandler(Tab tab) {
        return new ExternalNavigationHandler(new ExternalNavigationDelegateImpl(tab));
    }

    @Override
    public ContextMenuPopulatorFactory createContextMenuPopulatorFactory(Tab tab) {
        return new ChromeContextMenuPopulatorFactory(
                new TabContextMenuItemDelegate(
                        tab, mActivity.getTabModelSelector(), mEphemeralTabCoordinatorSupplier),
                mShareDelegateSupplier, ChromeContextMenuPopulator.ContextMenuMode.NORMAL,
                AppHooks.get().getExternalAuthUtils());
    }

    @Override
    public BrowserControlsVisibilityDelegate createBrowserControlsVisibilityDelegate(Tab tab) {
        return new ComposedBrowserControlsVisibilityDelegate(
                new TabStateBrowserControlsVisibilityDelegate(tab),
                mAppBrowserControlsVisibilityDelegate);
    }

    @Override
    public NativePage createNativePage(String url, NativePage candidatePage, Tab tab) {
        if (mNativePageFactory == null) {
            mNativePageFactory = new NativePageFactory(mActivity, mBottomSheetController);
        }
        return mNativePageFactory.createNativePage(url, candidatePage, tab);
    }
}
