// Copyright 2020 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#import "ios/chrome/browser/ui/gestures/view_revealing_vertical_pan_handler.h"

#import <UIKit/UIKit.h>

#include "base/check_op.h"
#import "base/test/ios/wait_util.h"
#import "ios/chrome/browser/ui/gestures/layout_switcher.h"
#import "ios/chrome/browser/ui/gestures/layout_switcher_provider.h"
#import "ios/chrome/browser/ui/gestures/view_revealing_animatee.h"
#include "testing/platform_test.h"
#import "third_party/ocmock/OCMock/OCMock.h"

#if !defined(__has_feature) || !__has_feature(objc_arc)
#error "This file requires ARC support."
#endif

// A fake animatee with one observable animated property.
@interface FakeAnimatee : UIView <ViewRevealingAnimatee>
@property(nonatomic, assign) ViewRevealState state;
@end

@implementation FakeAnimatee
- (instancetype)init {
  self = [super init];
  if (self) {
    self.state = ViewRevealState::Hidden;
  }
  return self;
}
- (void)willAnimateViewReveal:(ViewRevealState)viewRevealState {
}
- (void)animateViewReveal:(ViewRevealState)viewRevealState {
  self.state = viewRevealState;
}
- (void)didAnimateViewReveal:(ViewRevealState)viewRevealState {
}
@end

// A fake layout switcher provider.
@interface FakeLayoutSwitcherProvider : NSObject <LayoutSwitcherProvider>
@end

@implementation FakeLayoutSwitcherProvider
@synthesize layoutSwitcher = _layoutSwitcher;

- (instancetype)initWithLayoutSwitcher:(id<LayoutSwitcher>)layoutSwitcher {
  self = [super init];
  if (self) {
    _layoutSwitcher = layoutSwitcher;
  }
  return self;
}
@end

// A fake layout switcher with observable layout properties.
@interface FakeLayoutSwitcher : NSObject <LayoutSwitcher>
@property(nonatomic, assign) LayoutSwitcherState state;
@property(nonatomic, assign) LayoutSwitcherState nextState;
@property(nonatomic, copy) void (^transitionCompletionBlock)
    (BOOL completed, BOOL finished);
@end

@implementation FakeLayoutSwitcher
- (instancetype)init {
  self = [super init];
  if (self) {
    self.state = LayoutSwitcherState::Horizontal;
  }
  return self;
}

- (void)willTransitionToLayout:(LayoutSwitcherState)nextState
                    completion:
                        (void (^)(BOOL completed, BOOL finished))completion {
  self.nextState = nextState;
  self.transitionCompletionBlock = completion;
}

- (void)didUpdateTransitionLayoutProgress:(CGFloat)progress {
}

- (void)didTransitionToLayoutSuccessfully:(BOOL)success {
  if (success) {
    self.state = self.nextState;
  }
  self.transitionCompletionBlock(YES, success);
}
@end

// A fake gesture recognizer that allows the translation and velocity to be set.
@interface FakeGestureRecognizer : UIPanGestureRecognizer
@property(nonatomic, assign) CGPoint translation;
@property(nonatomic, assign) CGPoint velocity;
@end

@implementation FakeGestureRecognizer
- (CGPoint)translationInView:(UIView*)view {
  return self.translation;
}
- (CGPoint)velocityInView:(UIView*)view {
  return self.velocity;
}
@end

namespace {
// The distance between the Hidden and Peeked states.
const CGFloat kThumbStripHeight = 212.0f;
// The distance between te Peeked and Revealed states is |kBaseViewHeight| -
// |kBVCHeightTabGrid|.
const CGFloat kBVCHeightTabGrid = 108.0f;
const CGFloat kBaseViewHeight = 800.0f;
// The percentage of the total distance traveled by a gesture required to
// trigger a transition.
const CGFloat kRevealThreshold = 1 / 3.0f;
// A small extra distance to guarantee that the minimum distance has been
// traveled during a pan gesture simulation.
const CGFloat kSmallOffset = 10.0f;
// The delay in ms after the gesture ends before starting a new one.
const int kAnimationDelay = 1;

// The test class, passed as argument to TEST_F().
using ViewRevealingVerticalPanHandlerTest = PlatformTest;

// Simulates a fake vertical pan gesture from beginning, to change, to end.
// |translation_y| is by how much the gesture translates vertically
void SimulatePanGesture(ViewRevealingVerticalPanHandler* pan_handler,
                        double translation_y) {
  // A small offset in the same direction as the translation to guarantee that
  // the gesture's translation is greater than the reveal threshold.
  double offset = translation_y > 0 ? kSmallOffset : -kSmallOffset;
  FakeGestureRecognizer* fake_gesture_recognizer =
      [[FakeGestureRecognizer alloc]
          initWithTarget:pan_handler
                  action:@selector(handlePanGesture:)];

  fake_gesture_recognizer.state = UIGestureRecognizerStateBegan;
  [pan_handler handlePanGesture:fake_gesture_recognizer];
  fake_gesture_recognizer.state = UIGestureRecognizerStateChanged;
  fake_gesture_recognizer.translation = CGPointMake(0, translation_y);
  [pan_handler handlePanGesture:fake_gesture_recognizer];
  fake_gesture_recognizer.translation = CGPointMake(0, translation_y + offset);
  fake_gesture_recognizer.state = UIGestureRecognizerStateEnded;
  [pan_handler handlePanGesture:fake_gesture_recognizer];
  // The runloop needs to be spun between the end of a gesture and the beggining
  // of another one, because the current state of the pan_handler needs to be
  // updated to its next state before starting a new transition.
  base::test::ios::SpinRunLoopWithMinDelay(
      base::TimeDelta::FromMilliseconds(kAnimationDelay));
}

// Simulates 4 transitions of state in a ViewRevealingVerticalPanHandler (Hidden
// -> Peeked -> Revealed -> Peeked -> Hiiden), and observes the resulting change
// of state in a fake animatee.
TEST_F(ViewRevealingVerticalPanHandlerTest, DetectPan) {
  double remaining_height = kBaseViewHeight - kBVCHeightTabGrid;

  // Create a view revealing vertical pan handler.
  ViewRevealingVerticalPanHandler* pan_handler =
      [[ViewRevealingVerticalPanHandler alloc]
          initWithPeekedHeight:kThumbStripHeight
           revealedCoverHeight:kBVCHeightTabGrid
                baseViewHeight:kBaseViewHeight];

  // Create a fake layout switcher and a provider.
  FakeLayoutSwitcher* fake_layout_switcher = [[FakeLayoutSwitcher alloc] init];
  FakeLayoutSwitcherProvider* fake_layout_switcher_provider =
      [[FakeLayoutSwitcherProvider alloc]
          initWithLayoutSwitcher:fake_layout_switcher];
  pan_handler.layoutSwitcherProvider = fake_layout_switcher_provider;
  EXPECT_EQ(LayoutSwitcherState::Horizontal, fake_layout_switcher.state);

  // Create a fake animatee.
  FakeAnimatee* fake_animatee = [[FakeAnimatee alloc] init];
  [pan_handler addAnimatee:fake_animatee];
  EXPECT_EQ(ViewRevealState::Hidden, fake_animatee.state);

  // Simulate a pan gesture from Hidden state to Peeked state.
  SimulatePanGesture(pan_handler, kThumbStripHeight * kRevealThreshold);
  EXPECT_EQ(ViewRevealState::Peeked, fake_animatee.state);

  // Simulate a pan gesture from Peeked state to Revealed state. The layout
  // should transition to full state.
  SimulatePanGesture(pan_handler, remaining_height * kRevealThreshold);
  EXPECT_EQ(ViewRevealState::Revealed, fake_animatee.state);
  EXPECT_EQ(LayoutSwitcherState::Full, fake_layout_switcher.state);

  // Simulate a pan gesture from Revealed state to Peeked state. The layout
  // should transition back to horizontal state.
  SimulatePanGesture(pan_handler, -(remaining_height * kRevealThreshold));
  EXPECT_EQ(ViewRevealState::Peeked, fake_animatee.state);
  EXPECT_EQ(LayoutSwitcherState::Horizontal, fake_layout_switcher.state);

  // Simulate a pan gesture from Peeked state to Hidden state.
  SimulatePanGesture(pan_handler, -(kThumbStripHeight * kRevealThreshold));
  EXPECT_EQ(ViewRevealState::Hidden, fake_animatee.state);
}
}  // namespace
