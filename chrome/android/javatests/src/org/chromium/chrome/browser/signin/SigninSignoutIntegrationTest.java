// Copyright 2020 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.signin;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.pressBack;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import android.accounts.Account;
import android.support.test.InstrumentationRegistry;

import androidx.test.filters.LargeTest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import org.chromium.base.test.util.CommandLineFlags;
import org.chromium.base.test.util.JniMocker;
import org.chromium.chrome.R;
import org.chromium.chrome.browser.bookmarks.BookmarkModel;
import org.chromium.chrome.browser.flags.ChromeSwitches;
import org.chromium.chrome.browser.profiles.Profile;
import org.chromium.chrome.browser.profiles.ProfileAccountManagementMetrics;
import org.chromium.chrome.browser.settings.SettingsActivityTestRule;
import org.chromium.chrome.browser.sync.settings.AccountManagementFragment;
import org.chromium.chrome.test.ChromeJUnit4ClassRunner;
import org.chromium.chrome.test.ChromeTabbedActivityTestRule;
import org.chromium.chrome.test.util.ActivityUtils;
import org.chromium.chrome.test.util.BookmarkTestUtil;
import org.chromium.chrome.test.util.browser.signin.AccountManagerTestRule;
import org.chromium.components.signin.GAIAServiceType;
import org.chromium.components.signin.identitymanager.ConsentLevel;
import org.chromium.components.signin.metrics.SigninAccessPoint;
import org.chromium.content_public.browser.test.util.CriteriaHelper;
import org.chromium.content_public.browser.test.util.TestThreadUtils;
import org.chromium.ui.test.util.DisableAnimationsTestRule;

/**
 * Test the lifecycle of sign-in and sign-out.
 */
@RunWith(ChromeJUnit4ClassRunner.class)
@CommandLineFlags.Add({ChromeSwitches.DISABLE_FIRST_RUN_EXPERIENCE})
public class SigninSignoutIntegrationTest {
    @Rule
    public final DisableAnimationsTestRule mNoAnimationsRule = new DisableAnimationsTestRule();

    @Rule
    public final SettingsActivityTestRule<AccountManagementFragment> mSettingsActivityTestRule =
            new SettingsActivityTestRule<>(AccountManagementFragment.class);

    private final ChromeTabbedActivityTestRule mActivityTestRule =
            new ChromeTabbedActivityTestRule();

    private final AccountManagerTestRule mAccountManagerTestRule = new AccountManagerTestRule();

    // Mock sign-in environment needs to be destroyed after ChromeActivity in case there are
    // observers registered in the AccountManagerFacade mock.
    @Rule
    public final RuleChain mRuleChain =
            RuleChain.outerRule(mAccountManagerTestRule).around(mActivityTestRule);

    @Rule
    public final JniMocker mocker = new JniMocker();

    @Mock
    private SigninUtils.Natives mSigninUtilsNativeMock;

    @Mock
    private SigninManager.SignInStateObserver mSignInStateObserverMock;

    private SigninManager mSigninManager;

    private BookmarkModel mBookmarkModel;

    @Before
    public void setUp() {
        initMocks(this);
        mocker.mock(SigninUtilsJni.TEST_HOOKS, mSigninUtilsNativeMock);
        mActivityTestRule.startMainActivityOnBlankPage();
        TestThreadUtils.runOnUiThreadBlocking(() -> {
            mSigninManager = IdentityServicesProvider.get().getSigninManager(
                    Profile.getLastUsedRegularProfile());
        });
        mSigninManager.addSignInStateObserver(mSignInStateObserverMock);
    }

    @After
    public void tearDown() {
        mSigninManager.removeSignInStateObserver(mSignInStateObserverMock);
    }

    @Test
    @LargeTest
    public void testSignIn() {
        Account account = mAccountManagerTestRule.addAccountAndWaitForSeeding(
                AccountManagerTestRule.TEST_ACCOUNT_EMAIL);
        SigninActivity signinActivity = ActivityUtils.waitForActivity(
                InstrumentationRegistry.getInstrumentation(), SigninActivity.class, () -> {
                    SigninActivityLauncherImpl.get().launchActivity(
                            mActivityTestRule.getActivity(), SigninAccessPoint.SETTINGS);
                });
        assertSignedOut();
        TestThreadUtils.runOnUiThreadBlocking(
                () -> { signinActivity.findViewById(R.id.positive_button).performClick(); });
        CriteriaHelper.pollUiThread(this::assertSignedIn);
        verify(mSignInStateObserverMock).onSignedIn();
        verify(mSignInStateObserverMock, never()).onSignedOut();
        TestThreadUtils.runOnUiThreadBlocking(() -> {
            Assert.assertEquals(account.name,
                    mSigninManager.getIdentityManager()
                            .getPrimaryAccountInfo(ConsentLevel.SYNC)
                            .getEmail());
        });
    }

    @Test
    @LargeTest
    public void testSignOut() {
        signIn();
        mSettingsActivityTestRule.startSettingsActivity();
        onView(withText(R.string.sign_out_and_turn_off_sync)).perform(click());
        onView(withText(R.string.continue_button)).inRoot(isDialog()).perform(click());
        assertSignedOut();
        verify(mSignInStateObserverMock).onSignedOut();
        verify(mSigninUtilsNativeMock)
                .logEvent(ProfileAccountManagementMetrics.TOGGLE_SIGNOUT,
                        GAIAServiceType.GAIA_SERVICE_TYPE_NONE);
        verify(mSigninUtilsNativeMock)
                .logEvent(ProfileAccountManagementMetrics.SIGNOUT_SIGNOUT,
                        GAIAServiceType.GAIA_SERVICE_TYPE_NONE);
    }

    @Test
    @LargeTest
    public void testSignOutDismissedByPressingBack() {
        signIn();
        mSettingsActivityTestRule.startSettingsActivity();
        onView(withText(R.string.sign_out_and_turn_off_sync)).perform(click());
        onView(isRoot()).perform(pressBack());
        verify(mSignInStateObserverMock, never()).onSignedOut();
        assertSignedIn();
        verify(mSigninUtilsNativeMock)
                .logEvent(ProfileAccountManagementMetrics.TOGGLE_SIGNOUT,
                        GAIAServiceType.GAIA_SERVICE_TYPE_NONE);
        verify(mSigninUtilsNativeMock)
                .logEvent(ProfileAccountManagementMetrics.SIGNOUT_CANCEL,
                        GAIAServiceType.GAIA_SERVICE_TYPE_NONE);
    }

    @Test
    @LargeTest
    public void testSignOutCancelled() {
        signIn();
        mSettingsActivityTestRule.startSettingsActivity();
        onView(withText(R.string.sign_out_and_turn_off_sync)).perform(click());
        onView(withText(R.string.cancel)).inRoot(isDialog()).perform(click());
        verify(mSignInStateObserverMock, never()).onSignedOut();
        assertSignedIn();
        verify(mSigninUtilsNativeMock)
                .logEvent(ProfileAccountManagementMetrics.TOGGLE_SIGNOUT,
                        GAIAServiceType.GAIA_SERVICE_TYPE_NONE);
        verify(mSigninUtilsNativeMock)
                .logEvent(ProfileAccountManagementMetrics.SIGNOUT_CANCEL,
                        GAIAServiceType.GAIA_SERVICE_TYPE_NONE);
    }

    @Test
    @LargeTest
    public void testSignOutNonManagedAccountWithDataWiped() {
        signIn();
        addOneTestBookmark();
        mSettingsActivityTestRule.startSettingsActivity();
        onView(withText(R.string.sign_out_and_turn_off_sync)).perform(click());
        onView(withId(R.id.remove_local_data)).perform(click());
        onView(withText(R.string.continue_button)).inRoot(isDialog()).perform(click());
        assertSignedOut();
        TestThreadUtils.runOnUiThreadBlocking(() -> {
            Assert.assertEquals(0, mBookmarkModel.getChildCount(mBookmarkModel.getDefaultFolder()));
        });
    }

    @Test
    @LargeTest
    public void testSignOutNonManagedAccountWithoutWipingData() {
        signIn();
        addOneTestBookmark();
        mSettingsActivityTestRule.startSettingsActivity();
        onView(withText(R.string.sign_out_and_turn_off_sync)).perform(click());
        onView(withText(R.string.continue_button)).inRoot(isDialog()).perform(click());
        assertSignedOut();
        TestThreadUtils.runOnUiThreadBlocking(() -> {
            Assert.assertEquals(1, mBookmarkModel.getChildCount(mBookmarkModel.getDefaultFolder()));
        });
    }

    private void addOneTestBookmark() {
        Assert.assertNull("This method should be called only once!", mBookmarkModel);
        TestThreadUtils.runOnUiThreadBlocking(() -> {
            mBookmarkModel = new BookmarkModel(Profile.fromWebContents(
                    mActivityTestRule.getActivity().getActivityTab().getWebContents()));
            mBookmarkModel.loadFakePartnerBookmarkShimForTesting();
        });
        BookmarkTestUtil.waitForBookmarkModelLoaded();
        TestThreadUtils.runOnUiThreadBlocking(() -> {
            Assert.assertEquals(0, mBookmarkModel.getChildCount(mBookmarkModel.getDefaultFolder()));
            mBookmarkModel.addBookmark(
                    mBookmarkModel.getDefaultFolder(), 0, "Test Bookmark", "http://google.com");
            Assert.assertEquals(1, mBookmarkModel.getChildCount(mBookmarkModel.getDefaultFolder()));
        });
    }

    private void signIn() {
        Account account = mAccountManagerTestRule.addAccountAndWaitForSeeding(
                AccountManagerTestRule.TEST_ACCOUNT_EMAIL);
        TestThreadUtils.runOnUiThreadBlocking(() -> {
            mSigninManager.signinAndEnableSync(SigninAccessPoint.SETTINGS, account, null);
        });
        assertSignedIn();
    }

    private void assertSignedIn() {
        TestThreadUtils.runOnUiThreadBlocking(() -> {
            Assert.assertTrue("Account should be signed in!",
                    mSigninManager.getIdentityManager().hasPrimaryAccount());
        });
    }

    private void assertSignedOut() {
        TestThreadUtils.runOnUiThreadBlocking(() -> {
            Assert.assertFalse("Account should be signed out!",
                    mSigninManager.getIdentityManager().hasPrimaryAccount());
            Assert.assertNull(
                    mSigninManager.getIdentityManager().getPrimaryAccountInfo(ConsentLevel.SYNC));
        });
    }
}
