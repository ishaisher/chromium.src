// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#import "ios/chrome/browser/passwords/ios_chrome_password_manager_client.h"

#include <memory>
#include <utility>

#include "base/bind.h"
#include "base/no_destructor.h"
#include "components/autofill/core/browser/logging/log_manager.h"
#include "components/autofill/core/browser/logging/log_router.h"
#include "components/autofill/core/common/password_form.h"
#include "components/keyed_service/core/service_access_type.h"
#include "components/password_manager/core/browser/password_form_manager_for_ui.h"
#include "components/password_manager/core/browser/password_manager.h"
#include "components/password_manager/core/browser/password_manager_constants.h"
#include "components/password_manager/core/browser/password_manager_driver.h"
#include "components/password_manager/core/browser/password_manager_util.h"
#include "components/password_manager/core/browser/password_requirements_service.h"
#include "components/password_manager/core/browser/store_metrics_reporter.h"
#include "components/password_manager/core/common/password_manager_pref_names.h"
#include "components/password_manager/ios/password_manager_ios_util.h"
#include "components/sync/driver/sync_service.h"
#include "components/translate/core/browser/translate_manager.h"
#import "components/ukm/ios/ukm_url_recorder.h"
#include "ios/chrome/browser/application_context.h"
#include "ios/chrome/browser/browser_state/chrome_browser_state.h"
#include "ios/chrome/browser/passwords/ios_chrome_password_store_factory.h"
#include "ios/chrome/browser/passwords/ios_password_requirements_service_factory.h"
#include "ios/chrome/browser/passwords/password_manager_log_router_factory.h"
#include "ios/chrome/browser/signin/identity_manager_factory.h"
#include "ios/chrome/browser/sync/profile_sync_service_factory.h"
#include "ios/chrome/browser/system_flags.h"
#include "ios/chrome/browser/translate/chrome_ios_translate_client.h"
#import "ios/chrome/browser/ui/ui_feature_flags.h"
#include "net/cert/cert_status_flags.h"
#include "services/metrics/public/cpp/ukm_recorder.h"
#include "services/network/public/cpp/shared_url_loader_factory.h"
#include "url/gurl.h"

#if !defined(__has_feature) || !__has_feature(objc_arc)
#error "This file requires ARC support."
#endif

using password_manager::PasswordFormManagerForUI;
using password_manager::PasswordManagerMetricsRecorder;
using password_manager::PasswordStore;
using password_manager::SyncState;

namespace {

const syncer::SyncService* GetSyncService(ChromeBrowserState* browser_state) {
  return ProfileSyncServiceFactory::GetForBrowserStateIfExists(browser_state);
}

}  // namespace

IOSChromePasswordManagerClient::IOSChromePasswordManagerClient(
    id<IOSChromePasswordManagerClientBridge> bridge)
    : bridge_(bridge),
      password_feature_manager_(GetPrefs(),
                                GetSyncService(bridge_.browserState)),
      credentials_filter_(
          this,
          base::BindRepeating(&GetSyncService, bridge_.browserState)),
      helper_(this) {
  saving_passwords_enabled_.Init(
      password_manager::prefs::kCredentialsEnableService, GetPrefs());
  static base::NoDestructor<password_manager::StoreMetricsReporter> reporter(
      this, GetSyncService(bridge_.browserState), GetIdentityManager(),
      GetPrefs());
  log_manager_ = autofill::LogManager::Create(
      ios::PasswordManagerLogRouterFactory::GetForBrowserState(
          bridge_.browserState),
      base::Closure());
}

IOSChromePasswordManagerClient::~IOSChromePasswordManagerClient() = default;

SyncState IOSChromePasswordManagerClient::GetPasswordSyncState() const {
  syncer::SyncService* sync_service =
      ProfileSyncServiceFactory::GetForBrowserState(bridge_.browserState);
  return password_manager_util::GetPasswordSyncState(sync_service);
}

bool IOSChromePasswordManagerClient::PromptUserToChooseCredentials(
    std::vector<std::unique_ptr<autofill::PasswordForm>> local_forms,
    const url::Origin& origin,
    CredentialsCallback callback) {
  NOTIMPLEMENTED();
  return false;
}

bool IOSChromePasswordManagerClient::PromptUserToSaveOrUpdatePassword(
    std::unique_ptr<PasswordFormManagerForUI> form_to_save,
    bool update_password) {
  if (form_to_save->IsBlacklisted())
    return false;

  [bridge_ removePasswordInfoBarManualFallback:YES];

  if (update_password) {
    [bridge_ showUpdatePasswordInfoBar:std::move(form_to_save) manual:NO];
  } else {
    [bridge_ showSavePasswordInfoBar:std::move(form_to_save) manual:NO];
  }

  return true;
}

void IOSChromePasswordManagerClient::PromptUserToMovePasswordToAccount(
    std::unique_ptr<password_manager::PasswordFormManagerForUI> form_to_move) {
  NOTIMPLEMENTED();
}

bool IOSChromePasswordManagerClient::RequiresReauthToFill() {
  return base::FeatureList::IsEnabled(kEnableAutofillPasswordReauthIOS);
}

void IOSChromePasswordManagerClient::ShowManualFallbackForSaving(
    std::unique_ptr<password_manager::PasswordFormManagerForUI> form_to_save,
    bool has_generated_password,
    bool is_update) {
  if (is_update) {
    [bridge_ showUpdatePasswordInfoBar:std::move(form_to_save) manual:YES];
  } else {
    [bridge_ showSavePasswordInfoBar:std::move(form_to_save) manual:YES];
  }
}

void IOSChromePasswordManagerClient::HideManualFallbackForSaving() {
  [bridge_ removePasswordInfoBarManualFallback:YES];
}

void IOSChromePasswordManagerClient::FocusedInputChanged(
    password_manager::PasswordManagerDriver* driver,
    autofill::mojom::FocusedFieldType focused_field_type) {
  NOTIMPLEMENTED();
}

void IOSChromePasswordManagerClient::AutomaticPasswordSave(
    std::unique_ptr<PasswordFormManagerForUI> saved_form_manager) {
  NOTIMPLEMENTED();
}

void IOSChromePasswordManagerClient::PromptUserToEnableAutosignin() {
  // TODO(crbug.com/435048): Implement this method.
  NOTIMPLEMENTED();
}

bool IOSChromePasswordManagerClient::IsIncognito() const {
  return (bridge_.browserState)->IsOffTheRecord();
}

const password_manager::PasswordManager*
IOSChromePasswordManagerClient::GetPasswordManager() const {
  return bridge_.passwordManager;
}

const password_manager::PasswordFeatureManager*
IOSChromePasswordManagerClient::GetPasswordFeatureManager() const {
  return &password_feature_manager_;
}

PrefService* IOSChromePasswordManagerClient::GetPrefs() const {
  return (bridge_.browserState)->GetPrefs();
}

PasswordStore* IOSChromePasswordManagerClient::GetProfilePasswordStore() const {
  return IOSChromePasswordStoreFactory::GetForBrowserState(
             bridge_.browserState, ServiceAccessType::EXPLICIT_ACCESS)
      .get();
}

PasswordStore* IOSChromePasswordManagerClient::GetAccountPasswordStore() const {
  // AccountPasswordStore is currenly not supported on iOS.
  return nullptr;
}

void IOSChromePasswordManagerClient::NotifyUserAutoSignin(
    std::vector<std::unique_ptr<autofill::PasswordForm>> local_forms,
    const url::Origin& origin) {
  DCHECK(!local_forms.empty());
  helper_.NotifyUserAutoSignin();
  [bridge_ showAutosigninNotification:std::move(local_forms[0])];
}

void IOSChromePasswordManagerClient::NotifyUserCouldBeAutoSignedIn(
    std::unique_ptr<autofill::PasswordForm> form) {
  helper_.NotifyUserCouldBeAutoSignedIn(std::move(form));
}

void IOSChromePasswordManagerClient::NotifySuccessfulLoginWithExistingPassword(
    std::unique_ptr<password_manager::PasswordFormManagerForUI>
        submitted_manager) {
  helper_.NotifySuccessfulLoginWithExistingPassword(
      std::move(submitted_manager));
}

void IOSChromePasswordManagerClient::NotifyStorePasswordCalled() {
  helper_.NotifyStorePasswordCalled();
}

void IOSChromePasswordManagerClient::NotifyUserCredentialsWereLeaked(
    password_manager::CredentialLeakType leak_type,
    password_manager::CompromisedSitesCount saved_sites,
    const GURL& origin,
    const base::string16& username) {
  [bridge_ showPasswordBreachForLeakType:leak_type URL:origin];
}

bool IOSChromePasswordManagerClient::IsSavingAndFillingEnabled(
    const GURL& url) const {
  return *saving_passwords_enabled_ && !IsIncognito() &&
         !net::IsCertStatusError(GetMainFrameCertStatus()) &&
         IsFillingEnabled(url);
}

bool IOSChromePasswordManagerClient::IsFillingEnabled(const GURL& url) const {
  return url.GetOrigin() !=
         GURL(password_manager::kPasswordManagerAccountDashboardURL);
}

bool IOSChromePasswordManagerClient::IsCommittedMainFrameSecure() const {
  return password_manager::WebStateContentIsSecureHtml(bridge_.webState);
}

const GURL& IOSChromePasswordManagerClient::GetLastCommittedURL() const {
  return bridge_.lastCommittedURL;
}

url::Origin IOSChromePasswordManagerClient::GetLastCommittedOrigin() const {
  return url::Origin::Create(bridge_.lastCommittedURL);
}

std::string IOSChromePasswordManagerClient::GetPageLanguage() const {
  // TODO(crbug.com/912597): Add WebState to the IOSChromePasswordManagerClient
  // to be able to get the pages LanguageState from the TranslateManager.
  return std::string();
}

const password_manager::CredentialsFilter*
IOSChromePasswordManagerClient::GetStoreResultFilter() const {
  return &credentials_filter_;
}

const autofill::LogManager* IOSChromePasswordManagerClient::GetLogManager()
    const {
  return log_manager_.get();
}

ukm::SourceId IOSChromePasswordManagerClient::GetUkmSourceId() {
  return bridge_.webState
             ? ukm::GetSourceIdForWebStateDocument(bridge_.webState)
             : ukm::kInvalidSourceId;
}

PasswordManagerMetricsRecorder*
IOSChromePasswordManagerClient::GetMetricsRecorder() {
  if (!metrics_recorder_) {
    metrics_recorder_.emplace(GetUkmSourceId(),
                              /*navigation_metric_recorder=*/nullptr);
  }
  return base::OptionalOrNullptr(metrics_recorder_);
}

signin::IdentityManager* IOSChromePasswordManagerClient::GetIdentityManager() {
  return IdentityManagerFactory::GetForBrowserState(bridge_.browserState);
}

scoped_refptr<network::SharedURLLoaderFactory>
IOSChromePasswordManagerClient::GetURLLoaderFactory() {
  return (bridge_.browserState)->GetSharedURLLoaderFactory();
}

password_manager::PasswordRequirementsService*
IOSChromePasswordManagerClient::GetPasswordRequirementsService() {
  return IOSPasswordRequirementsServiceFactory::GetForBrowserState(
      bridge_.browserState, ServiceAccessType::EXPLICIT_ACCESS);
}

bool IOSChromePasswordManagerClient::IsIsolationForPasswordSitesEnabled()
    const {
  return false;
}

bool IOSChromePasswordManagerClient::IsNewTabPage() const {
  return false;
}

password_manager::FieldInfoManager*
IOSChromePasswordManagerClient::GetFieldInfoManager() const {
  return nullptr;
}
