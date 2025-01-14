// Copyright 2020 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "third_party/blink/public/common/web_preferences/web_preferences_mojom_traits.h"

#include "base/strings/string_util.h"
#include "build/build_config.h"
#include "mojo/public/cpp/base/string16_mojom_traits.h"
#include "third_party/blink/public/mojom/widget/screen_info.mojom.h"
#include "url/mojom/url_gurl_mojom_traits.h"

namespace mojo {

// static
blink::mojom::PointerType
EnumTraits<blink::mojom::PointerType, ui::PointerType>::ToMojom(
    ui::PointerType scheme) {
  switch (scheme) {
    case ui::PointerType::POINTER_TYPE_FIRST:
      return blink::mojom::PointerType::kPointerFirstType;
    case ui::PointerType::POINTER_TYPE_COARSE:
      return blink::mojom::PointerType::kPointerCoarseType;
    case ui::PointerType::POINTER_TYPE_FINE:
      return blink::mojom::PointerType::kPointerFineType;
  }
  NOTREACHED();
  return blink::mojom::PointerType::kMinValue;
}

// static
bool EnumTraits<blink::mojom::PointerType, ui::PointerType>::FromMojom(
    blink::mojom::PointerType input,
    ui::PointerType* out) {
  switch (input) {
    case blink::mojom::PointerType::kPointerFirstType:
      *out = ui::PointerType::POINTER_TYPE_FIRST;
      return true;
    case blink::mojom::PointerType::kPointerCoarseType:
      *out = ui::PointerType::POINTER_TYPE_COARSE;
      return true;
    case blink::mojom::PointerType::kPointerFineType:
      *out = ui::PointerType::POINTER_TYPE_FINE;
      return true;
  }
  NOTREACHED();
  return false;
}

// static
blink::mojom::HoverType EnumTraits<blink::mojom::HoverType,
                                   ui::HoverType>::ToMojom(ui::HoverType type) {
  switch (type) {
    case ui::HoverType::HOVER_TYPE_FIRST:
      return blink::mojom::HoverType::kHoverFirstType;
    case ui::HoverType::HOVER_TYPE_HOVER:
      return blink::mojom::HoverType::kHoverHoverType;
  }
  NOTREACHED();
  return blink::mojom::HoverType::kMinValue;
}

// static
bool EnumTraits<blink::mojom::HoverType, ui::HoverType>::FromMojom(
    blink::mojom::HoverType input,
    ui::HoverType* out) {
  switch (input) {
    case blink::mojom::HoverType::kHoverFirstType:
      *out = ui::HoverType::HOVER_TYPE_FIRST;
      return true;
    case blink::mojom::HoverType::kHoverHoverType:
      *out = ui::HoverType::HOVER_TYPE_HOVER;
      return true;
  }
  NOTREACHED();
  return false;
}

// static
blink::mojom::EditingBehavior EnumTraits<blink::mojom::EditingBehavior,
                                         blink::web_pref::EditingBehaviorType>::
    ToMojom(blink::web_pref::EditingBehaviorType behavior) {
  switch (behavior) {
    case blink::web_pref::EditingBehaviorType::kEditingMacBehavior:
      return blink::mojom::EditingBehavior::kEditingMacBehavior;
    case blink::web_pref::EditingBehaviorType::kEditingWindowsBehavior:
      return blink::mojom::EditingBehavior::kEditingWindowsBehavior;
    case blink::web_pref::EditingBehaviorType::kEditingUnixBehavior:
      return blink::mojom::EditingBehavior::kEditingUnixBehavior;
    case blink::web_pref::EditingBehaviorType::kEditingAndroidBehavior:
      return blink::mojom::EditingBehavior::kEditingAndroidBehavior;
    case blink::web_pref::EditingBehaviorType::kEditingChromeOSBehavior:
      return blink::mojom::EditingBehavior::kEditingChromeOSBehavior;
  }
  NOTREACHED();
  return blink::mojom::EditingBehavior::kMaxValue;
}

// static
bool EnumTraits<blink::mojom::EditingBehavior,
                blink::web_pref::EditingBehaviorType>::
    FromMojom(blink::mojom::EditingBehavior input,
              blink::web_pref::EditingBehaviorType* out) {
  switch (input) {
    case blink::mojom::EditingBehavior::kEditingMacBehavior:
      *out = blink::web_pref::EditingBehaviorType::kEditingMacBehavior;
      return true;
    case blink::mojom::EditingBehavior::kEditingWindowsBehavior:
      *out = blink::web_pref::EditingBehaviorType::kEditingWindowsBehavior;
      return true;
    case blink::mojom::EditingBehavior::kEditingUnixBehavior:
      *out = blink::web_pref::EditingBehaviorType::kEditingUnixBehavior;
      return true;
    case blink::mojom::EditingBehavior::kEditingAndroidBehavior:
      *out = blink::web_pref::EditingBehaviorType::kEditingAndroidBehavior;
      return true;
    case blink::mojom::EditingBehavior::kEditingChromeOSBehavior:
      *out = blink::web_pref::EditingBehaviorType::kEditingChromeOSBehavior;
      return true;
  }
  NOTREACHED();
  return false;
}

// static
blink::mojom::ImageAnimationPolicy
EnumTraits<blink::mojom::ImageAnimationPolicy,
           blink::web_pref::ImageAnimationPolicy>::
    ToMojom(blink::web_pref::ImageAnimationPolicy policy) {
  switch (policy) {
    case blink::web_pref::ImageAnimationPolicy::kImageAnimationPolicyAllowed:
      return blink::mojom::ImageAnimationPolicy::kImageAnimationPolicyAllowed;
    case blink::web_pref::ImageAnimationPolicy::
        kImageAnimationPolicyAnimateOnce:
      return blink::mojom::ImageAnimationPolicy::
          kImageAnimationPolicyAnimateOnce;
    case blink::web_pref::ImageAnimationPolicy::
        kImageAnimationPolicyNoAnimation:
      return blink::mojom::ImageAnimationPolicy::
          kImageAnimationPolicyNoAnimation;
  }
  NOTREACHED();
  return blink::mojom::ImageAnimationPolicy::kImageAnimationPolicyNoAnimation;
}

// static
bool EnumTraits<blink::mojom::ImageAnimationPolicy,
                blink::web_pref::ImageAnimationPolicy>::
    FromMojom(blink::mojom::ImageAnimationPolicy input,
              blink::web_pref::ImageAnimationPolicy* out) {
  switch (input) {
    case blink::mojom::ImageAnimationPolicy::kImageAnimationPolicyAllowed:
      *out =
          blink::web_pref::ImageAnimationPolicy::kImageAnimationPolicyAllowed;
      return true;
    case blink::mojom::ImageAnimationPolicy::kImageAnimationPolicyAnimateOnce:
      *out = blink::web_pref::ImageAnimationPolicy::
          kImageAnimationPolicyAnimateOnce;
      return true;
    case blink::mojom::ImageAnimationPolicy::kImageAnimationPolicyNoAnimation:
      *out = blink::web_pref::ImageAnimationPolicy::
          kImageAnimationPolicyNoAnimation;
      return true;
  }
  NOTREACHED();
  return false;
}

// static
blink::mojom::ViewportStyle
EnumTraits<blink::mojom::ViewportStyle, blink::web_pref::ViewportStyle>::
    ToMojom(blink::web_pref::ViewportStyle style) {
  switch (style) {
    case blink::web_pref::ViewportStyle::kDefault:
      return blink::mojom::ViewportStyle::kDefault;
    case blink::web_pref::ViewportStyle::kMobile:
      return blink::mojom::ViewportStyle::kMobile;
    case blink::web_pref::ViewportStyle::kTelevision:
      return blink::mojom::ViewportStyle::kTelevision;
  }
  NOTREACHED();
  return blink::mojom::ViewportStyle::kLast;
}

// static
bool EnumTraits<blink::mojom::ViewportStyle, blink::web_pref::ViewportStyle>::
    FromMojom(blink::mojom::ViewportStyle input,
              blink::web_pref::ViewportStyle* out) {
  switch (input) {
    case blink::mojom::ViewportStyle::kDefault:
      *out = blink::web_pref::ViewportStyle::kDefault;
      return true;
    case blink::mojom::ViewportStyle::kMobile:
      *out = blink::web_pref::ViewportStyle::kMobile;
      return true;
    case blink::mojom::ViewportStyle::kTelevision:
      *out = blink::web_pref::ViewportStyle::kTelevision;
      return true;
  }
  NOTREACHED();
  return false;
}

// static
blink::mojom::AutoplayPolicy
EnumTraits<blink::mojom::AutoplayPolicy, blink::web_pref::AutoplayPolicy>::
    ToMojom(blink::web_pref::AutoplayPolicy style) {
  switch (style) {
    case blink::web_pref::AutoplayPolicy::kNoUserGestureRequired:
      return blink::mojom::AutoplayPolicy::kNoUserGestureRequired;
    case blink::web_pref::AutoplayPolicy::kUserGestureRequired:
      return blink::mojom::AutoplayPolicy::kUserGestureRequired;
    case blink::web_pref::AutoplayPolicy::kDocumentUserActivationRequired:
      return blink::mojom::AutoplayPolicy::kDocumentUserActivationRequired;
  }
  NOTREACHED();
  return blink::mojom::AutoplayPolicy::kNoUserGestureRequired;
}

// static
bool EnumTraits<blink::mojom::AutoplayPolicy, blink::web_pref::AutoplayPolicy>::
    FromMojom(blink::mojom::AutoplayPolicy input,
              blink::web_pref::AutoplayPolicy* out) {
  switch (input) {
    case blink::mojom::AutoplayPolicy::kNoUserGestureRequired:
      *out = blink::web_pref::AutoplayPolicy::kNoUserGestureRequired;
      return true;
    case blink::mojom::AutoplayPolicy::kUserGestureRequired:
      *out = blink::web_pref::AutoplayPolicy::kUserGestureRequired;
      return true;
    case blink::mojom::AutoplayPolicy::kDocumentUserActivationRequired:
      *out = blink::web_pref::AutoplayPolicy::kDocumentUserActivationRequired;
      return true;
  }
  NOTREACHED();
  return false;
}

// static
blink::mojom::PreferredColorScheme EnumTraits<
    blink::mojom::PreferredColorScheme,
    blink::PreferredColorScheme>::ToMojom(blink::PreferredColorScheme scheme) {
  switch (scheme) {
    case blink::PreferredColorScheme::kDark:
      return blink::mojom::PreferredColorScheme::kDark;
    case blink::PreferredColorScheme::kLight:
      return blink::mojom::PreferredColorScheme::kLight;
  }
  NOTREACHED();
  return blink::mojom::PreferredColorScheme::kLight;
}

// static
bool EnumTraits<blink::mojom::PreferredColorScheme,
                blink::PreferredColorScheme>::
    FromMojom(blink::mojom::PreferredColorScheme input,
              blink::PreferredColorScheme* out) {
  switch (input) {
    case blink::mojom::PreferredColorScheme::kDark:
      *out = blink::PreferredColorScheme::kDark;
      return true;
    case blink::mojom::PreferredColorScheme::kLight:
      *out = blink::PreferredColorScheme::kLight;
      return true;
  }
  NOTREACHED();
  return false;
}

// static
blink::mojom::EffectiveConnectionType EnumTraits<
    blink::mojom::EffectiveConnectionType,
    net::EffectiveConnectionType>::ToMojom(net::EffectiveConnectionType type) {
  switch (type) {
    case net::EffectiveConnectionType::EFFECTIVE_CONNECTION_TYPE_UNKNOWN:
      return blink::mojom::EffectiveConnectionType::
          kEffectiveConnectionUnknownType;
    case net::EffectiveConnectionType::EFFECTIVE_CONNECTION_TYPE_OFFLINE:
      return blink::mojom::EffectiveConnectionType::
          kEffectiveConnectionOfflineType;
    case net::EffectiveConnectionType::EFFECTIVE_CONNECTION_TYPE_SLOW_2G:
      return blink::mojom::EffectiveConnectionType::
          kEffectiveConnectionSlow2GType;
    case net::EffectiveConnectionType::EFFECTIVE_CONNECTION_TYPE_2G:
      return blink::mojom::EffectiveConnectionType::kEffectiveConnection2GType;
    case net::EffectiveConnectionType::EFFECTIVE_CONNECTION_TYPE_3G:
      return blink::mojom::EffectiveConnectionType::kEffectiveConnection3GType;
    case net::EffectiveConnectionType::EFFECTIVE_CONNECTION_TYPE_4G:
      return blink::mojom::EffectiveConnectionType::kEffectiveConnection4GType;
    case net::EffectiveConnectionType::EFFECTIVE_CONNECTION_TYPE_LAST:
      break;
  }
  NOTREACHED();
  return blink::mojom::EffectiveConnectionType::kMaxValue;
}

// static
bool EnumTraits<blink::mojom::EffectiveConnectionType,
                net::EffectiveConnectionType>::
    FromMojom(blink::mojom::EffectiveConnectionType input,
              net::EffectiveConnectionType* out) {
  switch (input) {
    case blink::mojom::EffectiveConnectionType::kEffectiveConnectionUnknownType:
      *out = net::EffectiveConnectionType::EFFECTIVE_CONNECTION_TYPE_UNKNOWN;
      return true;
    case blink::mojom::EffectiveConnectionType::kEffectiveConnectionOfflineType:
      *out = net::EffectiveConnectionType::EFFECTIVE_CONNECTION_TYPE_OFFLINE;
      return true;
    case blink::mojom::EffectiveConnectionType::kEffectiveConnectionSlow2GType:
      *out = net::EffectiveConnectionType::EFFECTIVE_CONNECTION_TYPE_SLOW_2G;
      return true;
    case blink::mojom::EffectiveConnectionType::kEffectiveConnection2GType:
      *out = net::EffectiveConnectionType::EFFECTIVE_CONNECTION_TYPE_2G;
      return true;
    case blink::mojom::EffectiveConnectionType::kEffectiveConnection3GType:
      *out = net::EffectiveConnectionType::EFFECTIVE_CONNECTION_TYPE_3G;
      return true;
    case blink::mojom::EffectiveConnectionType::kEffectiveConnection4GType:
      *out = net::EffectiveConnectionType::EFFECTIVE_CONNECTION_TYPE_4G;
      return true;
  }
  NOTREACHED();
  return false;
}

// static
bool StructTraits<blink::mojom::WebPreferencesDataView,
                  blink::web_pref::WebPreferences>::
    Read(blink::mojom::WebPreferencesDataView data,
         blink::web_pref::WebPreferences* out) {
  if (!data.ReadStandardFontFamilyMap(&out->standard_font_family_map) ||
      !data.ReadFixedFontFamilyMap(&out->fixed_font_family_map) ||
      !data.ReadSerifFontFamilyMap(&out->serif_font_family_map) ||
      !data.ReadSansSerifFontFamilyMap(&out->sans_serif_font_family_map) ||
      !data.ReadCursiveFontFamilyMap(&out->cursive_font_family_map) ||
      !data.ReadFantasyFontFamilyMap(&out->fantasy_font_family_map) ||
      !data.ReadPictographFontFamilyMap(&out->pictograph_font_family_map) ||
      !data.ReadLazyFrameLoadingDistanceThresholdsPx(
          &out->lazy_frame_loading_distance_thresholds_px) ||
      !data.ReadLazyImageLoadingDistanceThresholdsPx(
          &out->lazy_image_loading_distance_thresholds_px) ||
      !data.ReadLazyImageFirstKFullyLoad(&out->lazy_image_first_k_fully_load) ||
      !data.ReadDefaultEncoding(&out->default_encoding) ||
      !data.ReadTextTrackBackgroundColor(&out->text_track_background_color) ||
      !data.ReadDefaultEncoding(&out->text_track_text_color) ||
      !data.ReadTextTrackTextSize(&out->text_track_text_size) ||
      !data.ReadTextTrackTextShadow(&out->text_track_text_shadow) ||
      !data.ReadTextTrackFontFamily(&out->text_track_font_family) ||
      !data.ReadTextTrackFontStyle(&out->text_track_font_style) ||
      !data.ReadTextTrackFontVariant(&out->text_track_font_variant) ||
      !data.ReadTextTrackWindowColor(&out->text_track_window_color) ||
      !data.ReadTextTrackWindowPadding(&out->text_track_window_padding) ||
      !data.ReadTextTrackWindowRadius(&out->text_track_window_radius) ||
      !data.ReadPrimaryPointerType(&out->primary_pointer_type) ||
      !data.ReadPrimaryHoverType(&out->primary_hover_type) ||
      !data.ReadEditingBehavior(&out->editing_behavior) ||
      !data.ReadViewportStyle(&out->viewport_style) ||
      !data.ReadAnimationPolicy(&out->animation_policy) ||
      !data.ReadAutoplayPolicy(&out->autoplay_policy) ||
      !data.ReadPreferredColorScheme(&out->preferred_color_scheme) ||
      !data.ReadLowPriorityIframesThreshold(
          &out->low_priority_iframes_threshold) ||
      !data.ReadNetworkQualityEstimatorWebHoldback(
          &out->network_quality_estimator_web_holdback) ||
      !data.ReadWebAppScope(&out->web_app_scope)
#if defined(OS_ANDROID)
      || !data.ReadDefaultVideoPosterUrl(&out->default_video_poster_url)
#endif
  )
    return false;

  out->default_font_size = data.default_font_size();
  out->default_fixed_font_size = data.default_fixed_font_size();
  out->minimum_font_size = data.minimum_font_size();
  out->minimum_logical_font_size = data.minimum_logical_font_size();
  out->context_menu_on_mouse_up = data.context_menu_on_mouse_up();
  out->javascript_enabled = data.javascript_enabled();
  out->web_security_enabled = data.web_security_enabled();
  out->loads_images_automatically = data.loads_images_automatically();
  out->images_enabled = data.images_enabled();
  out->plugins_enabled = data.plugins_enabled();
  out->dom_paste_enabled = data.dom_paste_enabled();
  out->shrinks_standalone_images_to_fit =
      data.shrinks_standalone_images_to_fit();
  out->text_areas_are_resizable = data.text_areas_are_resizable();
  out->allow_scripts_to_close_windows = data.allow_scripts_to_close_windows();
  out->remote_fonts_enabled = data.remote_fonts_enabled();
  out->javascript_can_access_clipboard = data.javascript_can_access_clipboard();
  out->xslt_enabled = data.xslt_enabled();
  out->dns_prefetching_enabled = data.dns_prefetching_enabled();
  out->data_saver_enabled = data.data_saver_enabled();
  out->data_saver_holdback_web_api_enabled =
      data.data_saver_holdback_web_api_enabled();
  out->local_storage_enabled = data.local_storage_enabled();
  out->databases_enabled = data.databases_enabled();
  out->application_cache_enabled = data.application_cache_enabled();
  out->tabs_to_links = data.tabs_to_links();
  out->disable_ipc_flooding_protection = data.disable_ipc_flooding_protection();
  out->hyperlink_auditing_enabled = data.hyperlink_auditing_enabled();
  out->allow_universal_access_from_file_urls =
      data.allow_universal_access_from_file_urls();
  out->allow_file_access_from_file_urls =
      data.allow_file_access_from_file_urls();
  out->webgl1_enabled = data.webgl1_enabled();
  out->webgl2_enabled = data.webgl2_enabled();
  out->pepper_3d_enabled = data.pepper_3d_enabled();
  out->flash_3d_enabled = data.flash_3d_enabled();
  out->flash_stage3d_enabled = data.flash_stage3d_enabled();
  out->flash_stage3d_baseline_enabled = data.flash_stage3d_baseline_enabled();
  out->privileged_webgl_extensions_enabled =
      data.privileged_webgl_extensions_enabled();
  out->webgl_errors_to_console_enabled = data.webgl_errors_to_console_enabled();
  out->hide_scrollbars = data.hide_scrollbars();
  out->accelerated_2d_canvas_enabled = data.accelerated_2d_canvas_enabled();
  out->new_canvas_2d_api_enabled = data.new_canvas_2d_api_enabled();
  out->antialiased_2d_canvas_disabled = data.antialiased_2d_canvas_disabled();
  out->antialiased_clips_2d_canvas_enabled =
      data.antialiased_clips_2d_canvas_enabled();
  out->accelerated_filters_enabled = data.accelerated_filters_enabled();
  out->deferred_filters_enabled = data.deferred_filters_enabled();
  out->container_culling_enabled = data.container_culling_enabled();
  out->allow_running_insecure_content = data.allow_running_insecure_content();
  out->disable_reading_from_canvas = data.disable_reading_from_canvas();
  out->strict_mixed_content_checking = data.strict_mixed_content_checking();
  out->strict_powerful_feature_restrictions =
      data.strict_powerful_feature_restrictions();
  out->allow_geolocation_on_insecure_origins =
      data.allow_geolocation_on_insecure_origins();
  out->strictly_block_blockable_mixed_content =
      data.strictly_block_blockable_mixed_content();
  out->block_mixed_plugin_content = data.block_mixed_plugin_content();
  out->password_echo_enabled = data.password_echo_enabled();
  out->disable_reading_from_canvas = data.disable_reading_from_canvas();
  out->should_clear_document_background =
      data.should_clear_document_background();
  out->enable_scroll_animator = data.enable_scroll_animator();
  out->prefers_reduced_motion = data.prefers_reduced_motion();
  out->touch_event_feature_detection_enabled =
      data.touch_event_feature_detection_enabled();
  out->pointer_events_max_touch_points = data.pointer_events_max_touch_points();
  out->available_pointer_types = data.available_pointer_types();
  out->available_hover_types = data.available_hover_types();
  out->dont_send_key_events_to_javascript =
      data.dont_send_key_events_to_javascript();
  out->barrel_button_for_drag_enabled = data.barrel_button_for_drag_enabled();
  out->sync_xhr_in_documents_enabled = data.sync_xhr_in_documents_enabled();
  out->number_of_cpu_cores = data.number_of_cpu_cores();
  out->supports_multiple_windows = data.supports_multiple_windows();
  out->viewport_enabled = data.viewport_enabled();
  out->viewport_meta_enabled = data.viewport_meta_enabled();
  out->shrinks_viewport_contents_to_fit =
      data.shrinks_viewport_contents_to_fit();
  out->smooth_scroll_for_find_enabled = data.smooth_scroll_for_find_enabled();
  out->main_frame_resizes_are_orientation_changes =
      data.main_frame_resizes_are_orientation_changes();
  out->initialize_at_minimum_page_scale =
      data.initialize_at_minimum_page_scale();
  out->smart_insert_delete_enabled = data.smart_insert_delete_enabled();
  out->spatial_navigation_enabled = data.spatial_navigation_enabled();
  out->navigate_on_drag_drop = data.navigate_on_drag_drop();
  out->v8_cache_options = data.v8_cache_options();
  out->record_whole_document = data.record_whole_document();
  out->cookie_enabled = data.cookie_enabled();
  out->accelerated_video_decode_enabled =
      data.accelerated_video_decode_enabled();
  out->user_gesture_required_for_presentation =
      data.user_gesture_required_for_presentation();
  out->text_tracks_enabled = data.text_tracks_enabled();
  out->text_track_margin_percentage = data.text_track_margin_percentage();
  out->immersive_mode_enabled = data.immersive_mode_enabled();
  out->double_tap_to_zoom_enabled = data.double_tap_to_zoom_enabled();
  out->fullscreen_supported = data.fullscreen_supported();
  out->text_autosizing_enabled = data.text_autosizing_enabled();
#if defined(OS_ANDROID)
  out->font_scale_factor = data.font_scale_factor();
  out->device_scale_adjustment = data.device_scale_adjustment();
  out->force_enable_zoom = data.force_enable_zoom();
  out->support_deprecated_target_density_dpi =
      data.support_deprecated_target_density_dpi();
  out->use_legacy_background_size_shorthand_behavior =
      data.use_legacy_background_size_shorthand_behavior();
  out->wide_viewport_quirk = data.wide_viewport_quirk();
  out->use_wide_viewport = data.use_wide_viewport();
  out->force_zero_layout_height = data.force_zero_layout_height();
  out->viewport_meta_merge_content_quirk =
      data.viewport_meta_merge_content_quirk();
  out->viewport_meta_non_user_scalable_quirk =
      data.viewport_meta_non_user_scalable_quirk();
  out->viewport_meta_zero_values_quirk = data.viewport_meta_zero_values_quirk();
  out->clobber_user_agent_initial_scale_quirk =
      data.clobber_user_agent_initial_scale_quirk();
  out->ignore_main_frame_overflow_hidden_quirk =
      data.ignore_main_frame_overflow_hidden_quirk();
  out->report_screen_size_in_physical_pixels_quirk =
      data.report_screen_size_in_physical_pixels_quirk();
  out->reuse_global_for_unowned_main_frame =
      data.reuse_global_for_unowned_main_frame();
  out->spellcheck_enabled_by_default = data.spellcheck_enabled_by_default();
  out->video_fullscreen_orientation_lock_enabled =
      data.video_fullscreen_orientation_lock_enabled();
  out->video_rotate_to_fullscreen_enabled =
      data.video_rotate_to_fullscreen_enabled();
  out->embedded_media_experience_enabled =
      data.embedded_media_experience_enabled();
  out->css_hex_alpha_color_enabled = data.css_hex_alpha_color_enabled();
  out->scroll_top_left_interop_enabled = data.scroll_top_left_interop_enabled();
  out->disable_features_depending_on_viz =
      data.disable_features_depending_on_viz();
  out->disable_accelerated_small_canvases =
      data.disable_accelerated_small_canvases();
  out->reenable_web_components_v0 = data.reenable_web_components_v0();
#endif
  out->force_dark_mode_enabled = data.force_dark_mode_enabled();
  out->default_minimum_page_scale_factor =
      data.default_minimum_page_scale_factor();
  out->default_maximum_page_scale_factor =
      data.default_maximum_page_scale_factor();
  out->hide_download_ui = data.hide_download_ui();
  out->presentation_receiver = data.presentation_receiver();
  out->media_controls_enabled = data.media_controls_enabled();
  out->do_not_update_selection_on_mutating_selection_range =
      data.do_not_update_selection_on_mutating_selection_range();
  out->picture_in_picture_enabled = data.picture_in_picture_enabled();
  out->translate_service_available = data.translate_service_available();
  out->lazy_load_enabled = data.lazy_load_enabled();
  out->allow_mixed_content_upgrades = data.allow_mixed_content_upgrades();
  out->always_show_focus = data.always_show_focus();
  out->touch_drag_drop_enabled = data.touch_drag_drop_enabled();
  return true;
}

}  // namespace mojo
