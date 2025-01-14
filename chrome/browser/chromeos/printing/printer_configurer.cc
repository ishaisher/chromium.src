// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "chrome/browser/chromeos/printing/printer_configurer.h"

#include <map>
#include <set>
#include <utility>
#include <vector>

#include "base/bind.h"
#include "base/callback.h"
#include "base/containers/flat_map.h"
#include "base/feature_list.h"
#include "base/hash/md5.h"
#include "base/logging.h"
#include "base/memory/ptr_util.h"
#include "base/memory/ref_counted.h"
#include "base/metrics/histogram_functions.h"
#include "base/no_destructor.h"
#include "chrome/browser/browser_process.h"
#include "chrome/browser/browser_process_platform_part.h"
#include "chrome/browser/chromeos/printing/ppd_provider_factory.h"
#include "chrome/browser/component_updater/cros_component_installer_chromeos.h"
#include "chrome/browser/profiles/profile.h"
#include "chrome/common/chrome_features.h"
#include "chrome/common/webui_url_constants.h"
#include "chromeos/dbus/dbus_thread_manager.h"
#include "chromeos/dbus/debug_daemon/debug_daemon_client.h"
#include "chromeos/printing/ppd_provider.h"
#include "chromeos/printing/printer_configuration.h"
#include "components/device_event_log/device_event_log.h"
#include "content/public/browser/browser_thread.h"
#include "third_party/cros_system_api/dbus/debugd/dbus-constants.h"

namespace chromeos {

namespace {

// PrinterConfigurer override for testing.
PrinterConfigurer* g_printer_configurer_for_test = nullptr;

PrinterSetupResult PrinterSetupResultFromDbusResultCode(const Printer& printer,
                                                        int result_code) {
  DCHECK_GE(result_code, 0);
  switch (result_code) {
    case debugd::CupsResult::CUPS_SUCCESS:
      PRINTER_LOG(DEBUG) << printer.make_and_model()
                         << " Printer setup successful";
      return PrinterSetupResult::kSuccess;
    case debugd::CupsResult::CUPS_INVALID_PPD:
      PRINTER_LOG(EVENT) << printer.make_and_model() << " PPD Invalid";
      return PrinterSetupResult::kInvalidPpd;
    case debugd::CupsResult::CUPS_LPADMIN_FAILURE:
      PRINTER_LOG(ERROR) << printer.make_and_model()
                         << " lpadmin-manual failed";
      return PrinterSetupResult::kFatalError;
    case debugd::CupsResult::CUPS_AUTOCONF_FAILURE:
      PRINTER_LOG(EVENT) << printer.make_and_model()
                         << " lpadmin-autoconf failed";
      return PrinterSetupResult::kFatalError;
    case debugd::CupsResult::CUPS_BAD_URI:
      PRINTER_LOG(EVENT) << printer.make_and_model() << " Bad URI";
      return PrinterSetupResult::kBadUri;
    case debugd::CupsResult::CUPS_IO_ERROR:
      PRINTER_LOG(EVENT) << printer.make_and_model() << " I/O error";
      return PrinterSetupResult::kIoError;
    case debugd::CupsResult::CUPS_MEMORY_ALLOC_ERROR:
      PRINTER_LOG(EVENT) << printer.make_and_model()
                         << " Memory allocation error";
      return PrinterSetupResult::kMemoryAllocationError;
    case debugd::CupsResult::CUPS_PRINTER_UNREACHABLE:
      PRINTER_LOG(EVENT) << printer.make_and_model()
                         << " Printer is ureachable";
      return PrinterSetupResult::kPrinterUnreachable;
    case debugd::CupsResult::CUPS_PRINTER_WRONG_RESPONSE:
      PRINTER_LOG(EVENT) << printer.make_and_model()
                         << " Unexpected response from printer";
      return PrinterSetupResult::kPrinterSentWrongResponse;
    case debugd::CupsResult::CUPS_PRINTER_NOT_AUTOCONF:
      PRINTER_LOG(EVENT) << printer.make_and_model()
                         << "Printer is not autoconfigurable";
      return PrinterSetupResult::kPrinterIsNotAutoconfigurable;
    case debugd::CupsResult::CUPS_FATAL:
    default:
      // We have no idea.  It must be fatal.
      PRINTER_LOG(ERROR) << printer.make_and_model()
                         << " Unrecognized printer setup error: "
                         << result_code;
      return PrinterSetupResult::kFatalError;
  }
}

// Map D-Bus errors from the debug daemon client to D-Bus errors enumerated
// in PrinterSetupResult.
PrinterSetupResult PrinterSetupResultFromDbusErrorCode(
    DbusLibraryError dbus_error) {
  DCHECK_LT(dbus_error, 0);
  static const base::NoDestructor<
      base::flat_map<DbusLibraryError, PrinterSetupResult>>
      kDbusErrorMap({
          {DbusLibraryError::kNoReply, PrinterSetupResult::kDbusNoReply},
          {DbusLibraryError::kTimeout, PrinterSetupResult::kDbusTimeout},
      });
  auto it = kDbusErrorMap->find(dbus_error);
  return it != kDbusErrorMap->end() ? it->second
                                    : PrinterSetupResult::kDbusError;
}

// Records whether a |printer| contains a valid PpdReference defined as having
// either autoconf or a ppd reference set.
void RecordValidPpdReference(const Printer& printer) {
  const auto& ppd_ref = printer.ppd_reference();
  // A PpdReference is valid if exactly one field is set in PpdReference.
  int refs = ppd_ref.autoconf ? 1 : 0;
  refs += !ppd_ref.user_supplied_ppd_url.empty() ? 1 : 0;
  refs += !ppd_ref.effective_make_and_model.empty() ? 1 : 0;
  base::UmaHistogramBoolean("Printing.CUPS.ValidPpdReference", refs == 1);
}

// Configures printers by downloading PPDs then adding them to CUPS through
// debugd.  This class must be used on the UI thread.
class PrinterConfigurerImpl : public PrinterConfigurer {
 public:
  explicit PrinterConfigurerImpl(Profile* profile)
      : ppd_provider_(CreatePpdProvider(profile)) {}

  ~PrinterConfigurerImpl() override {}

  void SetUpPrinter(const Printer& printer,
                    PrinterSetupCallback callback) override {
    DCHECK_CURRENTLY_ON(content::BrowserThread::UI);
    DCHECK(!printer.id().empty());
    DCHECK(printer.HasUri());
    PRINTER_LOG(USER) << printer.make_and_model() << " Printer setup requested";
    // Record if autoconf and a PPD are set.  crbug.com/814374.
    RecordValidPpdReference(printer);

    if (!printer.IsIppEverywhere()) {
      PRINTER_LOG(DEBUG) << printer.make_and_model() << " Lookup PPD";
      ppd_provider_->ResolvePpd(
          printer.ppd_reference(),
          base::BindOnce(&PrinterConfigurerImpl::ResolvePpdDone,
                         weak_factory_.GetWeakPtr(), printer,
                         std::move(callback)));
      return;
    }

    PRINTER_LOG(DEBUG) << printer.make_and_model()
                       << " Attempting autoconf setup";
    auto* client = DBusThreadManager::Get()->GetDebugDaemonClient();
    client->CupsAddAutoConfiguredPrinter(
        printer.id(), printer.uri().GetNormalized(),
        base::BindOnce(&PrinterConfigurerImpl::OnAddedPrinter,
                       weak_factory_.GetWeakPtr(), printer,
                       std::move(callback)));
  }

 private:
  // Receive the callback from the debug daemon client once we attempt to
  // add the printer.
  void OnAddedPrinter(const Printer& printer,
                      PrinterSetupCallback cb,
                      int32_t result_code) {
    // It's expected that debug daemon posts callbacks on the UI thread.
    DCHECK_CURRENTLY_ON(content::BrowserThread::UI);

    PrinterSetupResult setup_result =
        result_code < 0
            ? PrinterSetupResultFromDbusErrorCode(
                  static_cast<DbusLibraryError>(result_code))
            : PrinterSetupResultFromDbusResultCode(printer, result_code);
    std::move(cb).Run(setup_result);
  }

  void AddPrinter(const Printer& printer,
                  const std::string& ppd_contents,
                  PrinterSetupCallback cb) {
    auto* client = DBusThreadManager::Get()->GetDebugDaemonClient();

    PRINTER_LOG(EVENT) << printer.make_and_model() << " Manual printer setup";
    client->CupsAddManuallyConfiguredPrinter(
        printer.id(), printer.uri().GetNormalized(), ppd_contents,
        base::BindOnce(&PrinterConfigurerImpl::OnAddedPrinter,
                       weak_factory_.GetWeakPtr(), printer, std::move(cb)));
  }

  void ResolvePpdDone(const Printer& printer,
                      PrinterSetupCallback cb,
                      PpdProvider::CallbackResultCode result,
                      const std::string& ppd_contents) {
    DCHECK_CURRENTLY_ON(content::BrowserThread::UI);
    PRINTER_LOG(EVENT) << printer.make_and_model()
                       << " PPD Resolution Result: " << result;
    switch (result) {
      case PpdProvider::SUCCESS:
        DCHECK(!ppd_contents.empty());
        AddPrinter(printer, ppd_contents, std::move(cb));
        break;
      case PpdProvider::CallbackResultCode::NOT_FOUND:
        std::move(cb).Run(PrinterSetupResult::kPpdNotFound);
        break;
      case PpdProvider::CallbackResultCode::SERVER_ERROR:
        std::move(cb).Run(PrinterSetupResult::kPpdUnretrievable);
        break;
      case PpdProvider::CallbackResultCode::INTERNAL_ERROR:
        std::move(cb).Run(PrinterSetupResult::kFatalError);
        break;
      case PpdProvider::CallbackResultCode::PPD_TOO_LARGE:
        std::move(cb).Run(PrinterSetupResult::kPpdTooLarge);
        break;
    }
  }

  scoped_refptr<PpdProvider> ppd_provider_;
  base::WeakPtrFactory<PrinterConfigurerImpl> weak_factory_{this};

  DISALLOW_COPY_AND_ASSIGN(PrinterConfigurerImpl);
};

}  // namespace

// static
std::string PrinterConfigurer::SetupFingerprint(const Printer& printer) {
  base::MD5Context ctx;
  base::MD5Init(&ctx);
  base::MD5Update(&ctx, printer.id());
  base::MD5Update(&ctx, printer.uri().GetNormalized());
  base::MD5Update(&ctx, printer.ppd_reference().user_supplied_ppd_url);
  base::MD5Update(&ctx, printer.ppd_reference().effective_make_and_model);
  char autoconf = printer.ppd_reference().autoconf ? 1 : 0;
  base::MD5Update(&ctx, std::string(&autoconf, sizeof(autoconf)));
  base::MD5Digest digest;
  base::MD5Final(&digest, &ctx);
  return std::string(reinterpret_cast<char*>(&digest.a[0]), sizeof(digest.a));
}

// static
void PrinterConfigurer::RecordUsbPrinterSetupSource(
    UsbPrinterSetupSource source) {
  base::UmaHistogramEnumeration("Printing.CUPS.UsbSetupSource", source);
}

// static
std::unique_ptr<PrinterConfigurer> PrinterConfigurer::Create(Profile* profile) {
  if (g_printer_configurer_for_test) {
    auto printer_configurer =
        base::WrapUnique<PrinterConfigurer>(g_printer_configurer_for_test);
    g_printer_configurer_for_test = nullptr;
    return printer_configurer;
  }
  return std::make_unique<PrinterConfigurerImpl>(profile);
}

// static
void PrinterConfigurer::SetPrinterConfigurerForTesting(
    std::unique_ptr<PrinterConfigurer> printer_configurer) {
  DCHECK(!g_printer_configurer_for_test);
  g_printer_configurer_for_test = printer_configurer.release();
}

// static
GURL PrinterConfigurer::GeneratePrinterEulaUrl(const std::string& license) {
  GURL eula_url(chrome::kChromeUIOSCreditsURL);
  // Construct the URL with proper reference fragment.
  GURL::Replacements replacements;
  replacements.SetRefStr(license);
  return eula_url.ReplaceComponents(replacements);
}

std::string ResultCodeToMessage(const PrinterSetupResult result) {
  switch (result) {
    // Success.
    case PrinterSetupResult::kSuccess:
      return "Printer successfully configured.";
    case PrinterSetupResult::kEditSuccess:
      return "Printer successfully updated.";
    // Invalid configuration.
    case PrinterSetupResult::kNativePrintersNotAllowed:
      return "Unable to add or edit printer due to enterprise policy.";
    case PrinterSetupResult::kBadUri:
      return "Invalid URI.";
    case PrinterSetupResult::kInvalidPrinterUpdate:
      return "Requested printer changes would make printer unusable.";
    // Problem with a printer.
    case PrinterSetupResult::kPrinterUnreachable:
      return "Could not contact printer for configuration.";
    case PrinterSetupResult::kPrinterSentWrongResponse:
      return "Printer sent unexpected response.";
    case PrinterSetupResult::kPrinterIsNotAutoconfigurable:
      return "Printer is not autoconfigurable.";
    // Problem with a PPD file.
    case PrinterSetupResult::kPpdTooLarge:
      return "PPD is too large.";
    case PrinterSetupResult::kInvalidPpd:
      return "Provided PPD is invalid.";
    case PrinterSetupResult::kPpdNotFound:
      return "Could not locate requested PPD. Check printer configuration.";
    case PrinterSetupResult::kPpdUnretrievable:
      return "Could not retrieve PPD from server. Check Internet connection.";
    // Cannot load a required compomonent.
    case PrinterSetupResult::kComponentUnavailable:
      return "Could not install component.";
    // Problem with D-Bus.
    case PrinterSetupResult::kDbusError:
      return "D-Bus error occurred. Reboot required.";
    case PrinterSetupResult::kDbusNoReply:
      return "Couldn't talk to debugd over D-Bus.";
    case PrinterSetupResult::kDbusTimeout:
      return "Timed out trying to reach debugd over D-Bus.";
    // Problem reported by OS.
    case PrinterSetupResult::kIoError:
      return "I/O error occurred.";
    case PrinterSetupResult::kMemoryAllocationError:
      return "Memory allocation error occurred.";
    // Unknown problem.
    case PrinterSetupResult::kFatalError:
      return "Unknown error occurred.";
    // This is not supposed to happen.
    case PrinterSetupResult::kMaxValue:
      return "The error code is invalid.";
  }
}

}  // namespace chromeos
