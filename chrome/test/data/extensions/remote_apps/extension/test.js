// Copyright 2020 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

let api;

const testCases = [
  async function AddApp() {
    const result1 = await api.addApp('App 1', '', '');
    chrome.test.assertFalse(!!result1.error);
    chrome.test.assertEq('Id 1', result1.appId);

    const result2 = await api.addApp('App 2', 'missing', '');
    chrome.test.assertEq('Folder ID provided does not exist', result2.error);

    chrome.test.succeed();
  },
  async function AddFolderAndApps() {
    const result1 = await api.addFolder('Folder 1');
    const folderId = result1.folderId;
    chrome.test.assertFalse(!!result1.error);
    chrome.test.assertEq('Id 1', folderId);

    const result2 = await api.addApp('App 1', folderId, '');
    chrome.test.assertFalse(!!result2.error);
    chrome.test.assertEq('Id 2', result2.appId);

    const result3 = await api.addApp('App 2', folderId, '');
    chrome.test.assertFalse(!!result3.error);
    chrome.test.assertEq('Id 3', result3.appId);

    chrome.test.succeed();
  },
  async function OnRemoteAppLaunched() {
    let actualId = '';
    await new Promise(async (resolve) => {
      await api.addRemoteAppLaunchObserver(id => {
        actualId = id;
        resolve();
      });
      await api.addApp('App 1', '', '');
      chrome.test.sendMessage('Remote app added');
    });

    chrome.test.assertEq('Id 1', actualId);
    chrome.test.succeed();
  },
];

chrome.test.getConfig(async (config) => {
  try {
    api = await chrome.mojoPrivate.requireAsync('chromeos.remote_apps');
  } catch (e) {
    chrome.test.notifyFail('Could not get mojoPrivate bindings: ' + e.message);
    return;
  }

  const testName = config.customArg;
  const testCase = testCases.find((f) => f.name === testName);
  if (!testCase) {
    chrome.test.notifyFail('Test case \'' + testName + '\' not found');
    return;
  }

  chrome.test.runTests([testCase]);
});
