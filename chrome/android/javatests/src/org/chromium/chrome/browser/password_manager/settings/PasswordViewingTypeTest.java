// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.password_manager.settings;

import static org.mockito.Mockito.when;

import android.accounts.Account;

import androidx.test.filters.SmallTest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import org.chromium.base.test.BaseJUnit4ClassRunner;
import org.chromium.base.test.util.Feature;
import org.chromium.chrome.browser.settings.MainSettings;
import org.chromium.chrome.browser.settings.SettingsActivityTestRule;
import org.chromium.chrome.browser.sync.AndroidSyncSettings;
import org.chromium.chrome.browser.sync.ProfileSyncService;
import org.chromium.chrome.test.ChromeBrowserTestRule;
import org.chromium.components.browser_ui.settings.ChromeBasePreference;
import org.chromium.content_public.browser.test.util.TestThreadUtils;

/**
 * Tests for verifying whether users are presented with the correct option of viewing
 * passwords according to the user group they belong to (syncing with sync passphrase,
 * syncing without sync passsphrase, non-syncing).
 */
@RunWith(BaseJUnit4ClassRunner.class)
public class PasswordViewingTypeTest {
    private final ChromeBrowserTestRule mChromeBrowserTestRule = new ChromeBrowserTestRule();

    private final SettingsActivityTestRule<MainSettings> mSettingsActivityTestRule =
            new SettingsActivityTestRule<>(MainSettings.class);

    // We need to destroy the SettingsActivity before tearing down the mock sign-in environment
    // setup in ChromeBrowserTestRule to avoid code crash.
    @Rule
    public final RuleChain mRuleChain =
            RuleChain.outerRule(mChromeBrowserTestRule).around(mSettingsActivityTestRule);

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private ChromeBasePreference mPasswordsPref;
    private String mAuthority;
    private Account mAccount;
    @Mock
    private AndroidSyncSettings mAndroidSyncSettings;

    @Before
    public void setUp() {
        mAccount = mChromeBrowserTestRule.addAccount("account@example.com");
        mSettingsActivityTestRule.startSettingsActivity();
        MainSettings mainSettings = mSettingsActivityTestRule.getFragment();
        mPasswordsPref =
                (ChromeBasePreference) mainSettings.findPreference(MainSettings.PREF_PASSWORDS);
        TestThreadUtils.runOnUiThreadBlocking(() -> {
            AndroidSyncSettings.overrideForTests(mAndroidSyncSettings);
            setSyncability(false);
            mAuthority = AndroidSyncSettings.getContractAuthority();
        });
    }

    /**
     * Override ProfileSyncService using FakeProfileSyncService.
     */
    private void overrideProfileSyncService(final boolean usingPassphrase) {
        class FakeProfileSyncService extends ProfileSyncService {
            @Override
            public boolean isUsingSecondaryPassphrase() {
                return usingPassphrase;
            }

            @Override
            public boolean isEngineInitialized() {
                return true;
            }
        }
        TestThreadUtils.runOnUiThreadBlocking(
                () -> { ProfileSyncService.overrideForTests(new FakeProfileSyncService()); });
    }

    /**
     * Turn syncability on/off.
     */
    private void setSyncability(boolean syncState) {
        TestThreadUtils.runOnUiThreadBlocking(() -> {
            when(mAndroidSyncSettings.isSyncEnabled()).thenReturn(syncState);
            when(mAndroidSyncSettings.isChromeSyncEnabled()).thenReturn(syncState);
            when(mAndroidSyncSettings.doesMasterSyncSettingAllowChromeSync()).thenReturn(true);
        });
    }

    /**
     * Verifies that sync settings are being set up correctly in the case of redirecting users.
     * Checks that sync users are allowed to view passwords natively.
     */
    @Test
    @SmallTest
    @Feature({"Sync"})
    public void testUserRedirectSyncSettings() {
        setSyncability(true);
        overrideProfileSyncService(false);
        TestThreadUtils.runOnUiThreadBlocking(() -> {
            Assert.assertTrue(AndroidSyncSettings.get().isSyncEnabled());
            Assert.assertTrue(ProfileSyncService.get().isEngineInitialized());
            Assert.assertFalse(ProfileSyncService.get().isUsingSecondaryPassphrase());
        });
        Assert.assertEquals(
                PasswordSettings.class.getCanonicalName(), mPasswordsPref.getFragment());
    }

    /**
     * Verifies that syncing users with a custom passphrase are allowed to
     * natively view passwords.
     */
    @Test
    @SmallTest
    public void testSyncingNativePasswordView() {
        setSyncability(true);
        overrideProfileSyncService(true);
        Assert.assertEquals(
                PasswordSettings.class.getCanonicalName(), mPasswordsPref.getFragment());
        Assert.assertNotNull(mSettingsActivityTestRule.getActivity().getIntent());
    }

    /**
     * Verifies that non-syncing users are allowed to natively view passwords.
     */
    @Test
    @SmallTest
    public void testNonSyncingNativePasswordView() {
        setSyncability(false);
        Assert.assertEquals(
                PasswordSettings.class.getCanonicalName(), mPasswordsPref.getFragment());
    }
}
