// Copyright 2020 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

import {eventToPromise} from 'chrome-extension://mhjfbmdgcfjbbpaeojofohoefgiehjai/_test_resources/webui/test_util.m.js';
import {FittingType} from 'chrome-extension://mhjfbmdgcfjbbpaeojofohoefgiehjai/constants.js';
import {ViewerPdfToolbarNewElement} from 'chrome-extension://mhjfbmdgcfjbbpaeojofohoefgiehjai/elements/viewer-pdf-toolbar-new.js';

/** @return {!ViewerPdfToolbarNewElement} */
function createToolbar() {
  document.body.innerHTML = '';
  const toolbar = /** @type {!ViewerPdfToolbarNewElement} */ (
      document.createElement('viewer-pdf-toolbar-new'));
  document.body.appendChild(toolbar);
  return toolbar;
}

/**
 * Returns the cr-icon-buttons in |toolbar|'s shadowRoot under |parentId|.
 * @param {!ViewerPdfToolbarNewElement} toolbar
 * @param {string} parentId
 * @return {!NodeList<!CrIconButtonElement>}
 */
function getCrIconButtons(toolbar, parentId) {
  return /** @type {!NodeList<!CrIconButtonElement>} */ (
      toolbar.shadowRoot.querySelectorAll(`#${parentId} cr-icon-button`));
}

/**
 * @param {!HTMLElement} button
 * @param {boolean} enabled
 */
function assertCheckboxMenuButton(button, enabled) {
  chrome.test.assertEq(
      enabled ? 'true' : 'false', button.getAttribute('aria-checked'));
  chrome.test.assertEq(enabled, !button.querySelector('iron-icon').hidden);
}

// Unit tests for the viewer-pdf-toolbar-new element.
const tests = [
  /**
   * Test that the toolbar toggles between showing the fit-to-page and
   * fit-to-width buttons.
   */
  function testFitButton() {
    const toolbar = createToolbar();
    const fitButton = getCrIconButtons(toolbar, 'center')[2];
    const fitWidthIcon = 'pdf:fit-to-width';
    const fitHeightIcon = 'pdf:fit-to-height';

    let lastFitType = '';
    let numEvents = 0;
    toolbar.addEventListener('fit-to-changed', e => {
      lastFitType = e.detail;
      numEvents++;
    });

    // Initially FIT_TO_WIDTH, show FIT_TO_PAGE.
    chrome.test.assertEq(fitHeightIcon, fitButton.ironIcon);

    // Tap 1: Fire fit-to-changed(FIT_TO_PAGE), show fit-to-width.
    fitButton.click();
    chrome.test.assertEq(FittingType.FIT_TO_PAGE, lastFitType);
    chrome.test.assertEq(1, numEvents);
    chrome.test.assertEq(fitWidthIcon, fitButton.ironIcon);

    // Tap 2: Fire fit-to-changed(FIT_TO_WIDTH), show fit-to-page.
    fitButton.click();
    chrome.test.assertEq(FittingType.FIT_TO_WIDTH, lastFitType);
    chrome.test.assertEq(2, numEvents);
    chrome.test.assertEq(fitHeightIcon, fitButton.ironIcon);

    // Do the same as above, but with fitToggle().
    toolbar.fitToggle();
    chrome.test.assertEq(FittingType.FIT_TO_PAGE, lastFitType);
    chrome.test.assertEq(3, numEvents);
    chrome.test.assertEq(fitWidthIcon, fitButton.ironIcon);
    toolbar.fitToggle();
    chrome.test.assertEq(FittingType.FIT_TO_WIDTH, lastFitType);
    chrome.test.assertEq(4, numEvents);
    chrome.test.assertEq(fitHeightIcon, fitButton.ironIcon);

    // Test forceFit(FIT_TO_PAGE): Updates the icon, does not fire an event.
    toolbar.forceFit(FittingType.FIT_TO_PAGE);
    chrome.test.assertEq(4, numEvents);
    chrome.test.assertEq(fitWidthIcon, fitButton.ironIcon);

    // Force fitting the same fit as the existing fit should do nothing.
    toolbar.forceFit(FittingType.FIT_TO_PAGE);
    chrome.test.assertEq(4, numEvents);
    chrome.test.assertEq(fitWidthIcon, fitButton.ironIcon);

    // Force fit width.
    toolbar.forceFit(FittingType.FIT_TO_WIDTH);
    chrome.test.assertEq(4, numEvents);
    chrome.test.assertEq(fitHeightIcon, fitButton.ironIcon);

    // Force fit height.
    toolbar.forceFit(FittingType.FIT_TO_HEIGHT);
    chrome.test.assertEq(4, numEvents);
    chrome.test.assertEq(fitWidthIcon, fitButton.ironIcon);

    chrome.test.succeed();
  },

  function testZoomButtons() {
    const toolbar = createToolbar();

    let zoomInCount = 0;
    let zoomOutCount = 0;
    toolbar.addEventListener('zoom-in', () => zoomInCount++);
    toolbar.addEventListener('zoom-out', () => zoomOutCount++);

    const zoomButtons = getCrIconButtons(toolbar, 'zoom-controls');

    // Zoom out
    chrome.test.assertEq('pdf:remove', zoomButtons[0].ironIcon);
    zoomButtons[0].click();
    chrome.test.assertEq(0, zoomInCount);
    chrome.test.assertEq(1, zoomOutCount);

    // Zoom in
    chrome.test.assertEq('pdf:add', zoomButtons[1].ironIcon);
    zoomButtons[1].click();
    chrome.test.assertEq(1, zoomInCount);
    chrome.test.assertEq(1, zoomOutCount);

    chrome.test.succeed();
  },

  function testRotateButton() {
    const toolbar = createToolbar();
    const rotateButton = getCrIconButtons(toolbar, 'center')[3];
    chrome.test.assertEq('pdf:rotate-left', rotateButton.ironIcon);

    const promise = eventToPromise('rotate-left', toolbar);
    rotateButton.click();
    promise.then(() => chrome.test.succeed());
  },

  function testZoomField() {
    const toolbar = createToolbar();
    toolbar.viewportZoom = .8;
    toolbar.zoomBounds = {min: 25, max: 500};
    const zoomField = toolbar.shadowRoot.querySelector('#zoom-controls input');
    chrome.test.assertEq('80%', zoomField.value);

    // Value is set based on viewport zoom.
    toolbar.viewportZoom = .533;
    chrome.test.assertEq('53%', zoomField.value);

    // Setting a non-number value resets to viewport zoom.
    zoomField.value = 'abc';
    zoomField.dispatchEvent(new CustomEvent('change'));
    chrome.test.assertEq('53%', zoomField.value);

    // Setting a value that is over the max zoom clips to the max value.
    const whenSent = eventToPromise('zoom-changed', toolbar);
    zoomField.value = '90000%';
    zoomField.dispatchEvent(new CustomEvent('change'));
    whenSent
        .then(e => {
          chrome.test.assertEq(500, e.detail);

          // This happens in the parent.
          toolbar.viewportZoom = 5;
          chrome.test.assertEq('500%', zoomField.value);

          // Setting a value that is over the maximum again restores the max
          // value, even though no event is sent.
          zoomField.value = '80000%';
          zoomField.dispatchEvent(new CustomEvent('change'));
          chrome.test.assertEq('500%', zoomField.value);

          // Setting a new value sends the value in a zoom-changed event.
          const whenSentNew = eventToPromise('zoom-changed', toolbar);
          zoomField.value = '110%';
          zoomField.dispatchEvent(new CustomEvent('change'));
          return whenSentNew;
        })
        .then(e => {
          chrome.test.assertEq(110, e.detail);

          // Setting a new value and blurring sends the value in a zoom-changed
          // event. If the value is below the minimum, this sends the minimum
          // zoom.
          const whenSentFromBlur = eventToPromise('zoom-changed', toolbar);
          zoomField.value = '18%';
          zoomField.dispatchEvent(new CustomEvent('blur'));
          return whenSentFromBlur;
        })
        .then(e => {
          chrome.test.assertEq(25, e.detail);
          chrome.test.succeed();
        });
  },

  // Test that the overflow menu closes when an action is triggered.
  function testOverflowMenuCloses() {
    const toolbar = createToolbar();
    const menu = toolbar.shadowRoot.querySelector('cr-action-menu');
    chrome.test.assertFalse(menu.open);

    const more = toolbar.shadowRoot.querySelector('#more');
    const buttons = menu.querySelectorAll('.dropdown-item');
    chrome.test.assertTrue(buttons.length > 0);

    for (const button of buttons) {
      // Open overflow menu.
      more.click();
      chrome.test.assertTrue(menu.open);
      button.click();
      chrome.test.assertFalse(menu.open);
    }
    chrome.test.succeed();
  },

  function testTwoPageViewToggle() {
    const toolbar = createToolbar();
    toolbar.twoUpViewEnabled = false;
    const button = /** @type {!HTMLElement} */ (
        toolbar.shadowRoot.querySelector('#two-page-view-button'));
    assertCheckboxMenuButton(button, false);

    let whenChanged = eventToPromise('two-up-view-changed', toolbar);
    button.click();
    whenChanged
        .then(e => {
          // Happens in the parent.
          toolbar.twoUpViewEnabled = true;
          chrome.test.assertEq(true, e.detail);
          assertCheckboxMenuButton(button, true);
          whenChanged = eventToPromise('two-up-view-changed', toolbar);
          button.click();
          return whenChanged;
        })
        .then(e => {
          // Happens in the parent.
          toolbar.twoUpViewEnabled = false;
          chrome.test.assertEq(false, e.detail);
          assertCheckboxMenuButton(button, false);
          chrome.test.succeed();
        });
  },

  function testShowAnnotationsToggle() {
    const toolbar = createToolbar();
    const button = /** @type {!HTMLElement} */ (
        toolbar.shadowRoot.querySelector('#show-annotations-button'));
    assertCheckboxMenuButton(button, true);

    let whenChanged = eventToPromise('display-annotations-changed', toolbar);
    button.click();
    whenChanged
        .then(e => {
          chrome.test.assertEq(false, e.detail);
          assertCheckboxMenuButton(button, false);
          whenChanged = eventToPromise('display-annotations-changed', toolbar);
          button.click();
          return whenChanged;
        })
        .then(e => {
          chrome.test.assertEq(true, e.detail);
          assertCheckboxMenuButton(button, true);
          chrome.test.succeed();
        });
  },

  function testSidenavToggleButton() {
    const toolbar = createToolbar();
    chrome.test.assertFalse(toolbar.sidenavCollapsed);

    const toggleButton = toolbar.shadowRoot.querySelector('#sidenavToggle');
    chrome.test.assertTrue(toggleButton.hasAttribute('aria-label'));
    chrome.test.assertTrue(toggleButton.hasAttribute('title'));
    chrome.test.assertEq('true', toggleButton.getAttribute('aria-expanded'));

    toolbar.sidenavCollapsed = true;
    chrome.test.assertEq('false', toggleButton.getAttribute('aria-expanded'));

    toolbar.addEventListener(
        'sidenav-toggle-click', () => chrome.test.succeed());
    toggleButton.click();
  },
];

chrome.test.runTests(tests);
