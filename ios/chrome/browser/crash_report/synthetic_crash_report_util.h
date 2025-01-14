// Copyright 2020 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#ifndef IOS_CHROME_BROWSER_CRASH_REPORT_SYNTHETIC_CRASH_REPORT_UTIL_H_
#define IOS_CHROME_BROWSER_CRASH_REPORT_SYNTHETIC_CRASH_REPORT_UTIL_H_

#include <string>

namespace base {
class FilePath;
}

// Creates Synthetic Crash Report for Unexplained Termination Event to be
// uploaded by Breakpad. |path| should be a path to Breakpad directory and
// the rest of the arguments are Breakpad specific values.
void CreateSyntheticCrashReportForUte(
    const base::FilePath& path,
    const std::string& breakpad_product_display,
    const std::string& breakpad_product,
    const std::string& breakpad_version,
    const std::string& breakpad_url);

#endif  // IOS_CHROME_BROWSER_CRASH_REPORT_SYNTHETIC_CRASH_REPORT_UTIL_H_
