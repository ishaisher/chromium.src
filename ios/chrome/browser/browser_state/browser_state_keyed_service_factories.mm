// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "ios/chrome/browser/browser_state/browser_state_keyed_service_factories.h"

#include "base/feature_list.h"
#include "ios/chrome/browser/autocomplete/autocomplete_classifier_factory.h"
#include "ios/chrome/browser/autocomplete/in_memory_url_index_factory.h"
#include "ios/chrome/browser/autocomplete/shortcuts_backend_factory.h"
#include "ios/chrome/browser/autofill/personal_data_manager_factory.h"
#include "ios/chrome/browser/bookmarks/bookmark_model_factory.h"
#import "ios/chrome/browser/bookmarks/managed_bookmark_service_factory.h"
#include "ios/chrome/browser/browsing_data/browsing_data_remover_factory.h"
#include "ios/chrome/browser/content_settings/cookie_settings_factory.h"
#include "ios/chrome/browser/crash_report/breadcrumbs/breadcrumb_manager_keyed_service_factory.h"
#include "ios/chrome/browser/crash_report/breadcrumbs/breadcrumb_persistent_storage_keyed_service_factory.h"
#include "ios/chrome/browser/credential_provider/credential_provider_service_factory.h"
#import "ios/chrome/browser/device_sharing/device_sharing_manager_factory.h"
#include "ios/chrome/browser/discover_feed/discover_feed_service_factory.h"
#include "ios/chrome/browser/dom_distiller/dom_distiller_service_factory.h"
#include "ios/chrome/browser/download/browser_download_service_factory.h"
#include "ios/chrome/browser/favicon/favicon_service_factory.h"
#include "ios/chrome/browser/favicon/ios_chrome_favicon_loader_factory.h"
#include "ios/chrome/browser/favicon/ios_chrome_large_icon_cache_factory.h"
#include "ios/chrome/browser/favicon/ios_chrome_large_icon_service_factory.h"
#include "ios/chrome/browser/feature_engagement/tracker_factory.h"
#include "ios/chrome/browser/gcm/ios_chrome_gcm_profile_service_factory.h"
#include "ios/chrome/browser/google/google_logo_service_factory.h"
#include "ios/chrome/browser/history/history_service_factory.h"
#include "ios/chrome/browser/history/top_sites_factory.h"
#include "ios/chrome/browser/history/web_history_service_factory.h"
#include "ios/chrome/browser/invalidation/ios_chrome_profile_invalidation_provider_factory.h"
#include "ios/chrome/browser/language/language_model_manager_factory.h"
#include "ios/chrome/browser/language/url_language_histogram_factory.h"
#import "ios/chrome/browser/metrics/ios_profile_session_durations_service_factory.h"
#include "ios/chrome/browser/ntp_snippets/ios_chrome_content_suggestions_service_factory.h"
#include "ios/chrome/browser/passwords/ios_chrome_password_check_manager_factory.h"
#include "ios/chrome/browser/passwords/ios_chrome_password_store_factory.h"
#import "ios/chrome/browser/policy/policy_features.h"
#include "ios/chrome/browser/policy_url_blocking/policy_url_blocking_service.h"
#include "ios/chrome/browser/reading_list/reading_list_model_factory.h"
#import "ios/chrome/browser/safe_browsing/real_time_url_lookup_service_factory.h"
#import "ios/chrome/browser/safe_browsing/verdict_cache_manager_factory.h"
#import "ios/chrome/browser/screen_time/features.h"
#include "ios/chrome/browser/search_engines/template_url_service_factory.h"
#include "ios/chrome/browser/signin/about_signin_internals_factory.h"
#include "ios/chrome/browser/signin/account_consistency_service_factory.h"
#include "ios/chrome/browser/signin/account_reconcilor_factory.h"
#include "ios/chrome/browser/signin/authentication_service_factory.h"
#include "ios/chrome/browser/signin/identity_manager_factory.h"
#include "ios/chrome/browser/signin/signin_browser_state_info_updater_factory.h"
#include "ios/chrome/browser/signin/signin_client_factory.h"
#include "ios/chrome/browser/signin/signin_error_controller_factory.h"
#include "ios/chrome/browser/suggestions/suggestions_service_factory.h"
#include "ios/chrome/browser/sync/consent_auditor_factory.h"
#include "ios/chrome/browser/sync/ios_user_event_service_factory.h"
#include "ios/chrome/browser/sync/model_type_store_service_factory.h"
#include "ios/chrome/browser/sync/profile_sync_service_factory.h"
#include "ios/chrome/browser/sync/sync_setup_service_factory.h"
#include "ios/chrome/browser/translate/translate_accept_languages_factory.h"
#include "ios/chrome/browser/translate/translate_ranker_factory.h"
#import "ios/chrome/browser/ui/voice/text_to_speech_playback_controller_factory.h"
#include "ios/chrome/browser/undo/bookmark_undo_service_factory.h"
#include "ios/chrome/browser/unified_consent/unified_consent_service_factory.h"
#include "ios/chrome/browser/webdata_services/web_data_service_factory.h"

#if defined(__IPHONE_14_0) && __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_14_0
#import "ios/chrome/browser/screen_time/screen_time_history_deleter_factory.h"
#endif  // __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_14_0

#if !defined(__has_feature) || !__has_feature(objc_arc)
#error "This file requires ARC support."
#endif

// This method gets the instance of each ServiceFactory. We do this so that
// each ServiceFactory initializes itself and registers its dependencies with
// the global PreferenceDependencyManager. We need to have a complete
// dependency graph when we create a browser state so we can dispatch the
// browser state creation message to the services that want to create their
// services at browser state creation time.
//
// TODO(erg): This needs to be something else. I don't think putting every
// FooServiceFactory here will scale or is desirable long term.
void EnsureBrowserStateKeyedServiceFactoriesBuilt() {
  autofill::PersonalDataManagerFactory::GetInstance();
  dom_distiller::DomDistillerServiceFactory::GetInstance();
  feature_engagement::TrackerFactory::GetInstance();
  ios::AboutSigninInternalsFactory::GetInstance();
  ios::AccountConsistencyServiceFactory::GetInstance();
  ios::AccountReconcilorFactory::GetInstance();
  ios::AutocompleteClassifierFactory::GetInstance();
  ios::BookmarkModelFactory::GetInstance();
  ios::BookmarkUndoServiceFactory::GetInstance();
  ios::CookieSettingsFactory::GetInstance();
  ios::FaviconServiceFactory::GetInstance();
  ios::HistoryServiceFactory::GetInstance();
  ios::InMemoryURLIndexFactory::GetInstance();
  ios::ShortcutsBackendFactory::GetInstance();
  ios::SigninErrorControllerFactory::GetInstance();
  ios::TemplateURLServiceFactory::GetInstance();
  ios::TopSitesFactory::GetInstance();
  ios::WebDataServiceFactory::GetInstance();
  ios::WebHistoryServiceFactory::GetInstance();
  translate::TranslateRankerFactory::GetInstance();
  suggestions::SuggestionsServiceFactory::GetInstance();
  AuthenticationServiceFactory::GetInstance();
  BreadcrumbManagerKeyedServiceFactory::GetInstance();
  BreadcrumbPersistentStorageKeyedServiceFactory::GetInstance();
  BrowserDownloadServiceFactory::GetInstance();
  BrowsingDataRemoverFactory::GetInstance();
  ConsentAuditorFactory::GetInstance();
  CredentialProviderServiceFactory::GetInstance();
  DeviceSharingManagerFactory::GetInstance();
  DiscoverFeedServiceFactory::GetInstance();
  GoogleLogoServiceFactory::GetInstance();
  IdentityManagerFactory::GetInstance();
  IOSChromeContentSuggestionsServiceFactory::GetInstance();
  IOSChromeFaviconLoaderFactory::GetInstance();
  IOSChromeGCMProfileServiceFactory::GetInstance();
  IOSChromeLargeIconCacheFactory::GetInstance();
  IOSChromeLargeIconServiceFactory::GetInstance();
  IOSChromePasswordCheckManagerFactory::GetInstance();
  IOSChromePasswordStoreFactory::GetInstance();
  IOSChromeProfileInvalidationProviderFactory::GetInstance();
  IOSProfileSessionDurationsServiceFactory::GetInstance();
  IOSUserEventServiceFactory::GetInstance();
  LanguageModelManagerFactory::GetInstance();
  ManagedBookmarkServiceFactory::GetInstance();
  ModelTypeStoreServiceFactory::GetInstance();
  ProfileSyncServiceFactory::GetInstance();
  ReadingListModelFactory::GetInstance();
  RealTimeUrlLookupServiceFactory::GetInstance();
  SigninBrowserStateInfoUpdaterFactory::GetInstance();
  SigninClientFactory::GetInstance();
  SyncSetupServiceFactory::GetInstance();
  TextToSpeechPlaybackControllerFactory::GetInstance();
  TranslateAcceptLanguagesFactory::GetInstance();
  UnifiedConsentServiceFactory::GetInstance();
  UrlLanguageHistogramFactory::GetInstance();
  VerdictCacheManagerFactory::GetInstance();

  if (IsURLBlocklistEnabled()) {
    PolicyBlocklistServiceFactory::GetInstance();
  }

#if defined(__IPHONE_14_0) && __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_14_0
  if (@available(iOS 14, *)) {
    if (IsScreenTimeIntegrationEnabled()) {
      ScreenTimeHistoryDeleterFactory::GetInstance();
    }
  }
#endif  // __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_14_0
}
