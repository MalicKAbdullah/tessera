import 'package:core_theme/core_theme.dart';
import 'package:flutter/material.dart';

/// Spacing on the suite's 4dp grid.
abstract final class Gap {
  static const double xs = AppSpacing.xs;
  static const double s = AppSpacing.sm;
  static const double m = AppSpacing.smd;
  static const double l = AppSpacing.md;
  static const double xl = AppSpacing.lg;
  static const double xxl = AppSpacing.xl;
  static const double page = AppSpacing.gutter;
}

abstract final class Radii {
  static const double s = AppSpacing.radiusMd;
  static const double m = AppSpacing.radiusLg;
  static const double l = AppSpacing.radiusXl;
  static const double pill = AppSpacing.radiusPill;
}

/// The wallpaper stand-in behind widget previews, top-left to bottom-right.
/// Previews show the user's widgets, which are drawn for a home screen, so
/// they sit on something wallpaper-like rather than on a suite surface.
List<Color> stageColors(Brightness b) => b == Brightness.dark
    ? const [Color(0xFF16181D), Color(0xFF22262E), Color(0xFF191C21)]
    : const [Color(0xFFD7DBE1), Color(0xFFC6CCD5), Color(0xFFDDE0E5)];

/// Motion on the suite's [AppMotion] tokens. Direct manipulation (presses,
/// the editor stage) answers with springs that land like [AppMotion.settle].
abstract final class Motion {
  static const quick = AppMotion.fast;
  static const base = AppMotion.base;
  static const slow = AppMotion.slow;
  static const page = AppMotion.slow;

  /// Anything that settles.
  static const Curve ease = AppMotion.standard;

  /// Entrances.
  static const Curve enter = AppMotion.emphasized;

  /// Pressed surfaces: stiff and critically damped, so a press never wobbles.
  static const press = SpringDescription(mass: 1, stiffness: 900, damping: 60);

  /// The editor stage: one small overshoot, so a change lands physically.
  static const stage = SpringDescription(mass: 1, stiffness: 320, damping: 18);

  /// Scale of a pressed surface.
  static const double pressScale = 0.97;

  /// The system "remove animations" setting.
  static bool reduced(BuildContext context) =>
      MediaQuery.disableAnimationsOf(context);

  /// [d], or zero when the system asks for reduced motion.
  static Duration of(BuildContext context, Duration d) =>
      reduced(context) ? Duration.zero : d;
}
