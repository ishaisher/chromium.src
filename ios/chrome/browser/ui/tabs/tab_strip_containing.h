// Copyright 2020 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#ifndef IOS_CHROME_BROWSER_UI_TABS_TAB_STRIP_CONTAINING_H_
#define IOS_CHROME_BROWSER_UI_TABS_TAB_STRIP_CONTAINING_H_

// Protocol defining an interface that takes snapshots of the Tab Strip View.
@protocol TabStripContaining

// Returns a snapshot of the Tab Strip view.
- (UIView*)screenshotForAnimation;

@end

#endif  // IOS_CHROME_BROWSER_UI_TABS_TAB_STRIP_CONTAINING_H_
