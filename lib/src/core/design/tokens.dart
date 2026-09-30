import 'package:flutter/material.dart';

/// The app's colour roles. Chrome stays monochrome so the widgets carry the
/// colour; [accent] marks the one thing on screen that is active or live.
@immutable
class Palette extends ThemeExtension<Palette> {
  const Palette({
    required this.canvas,
    required this.surface,
    required this.raised,
    required this.ink,
    required this.muted,
    required this.faint,
    required this.hairline,
    required this.accent,
    required this.onAccent,
    required this.stage,
  });

  /// Page background.
  final Color canvas;

  /// Cards and tracks.
  final Color surface;

  /// Selected pills, pressed surfaces.
  final Color raised;
  final Color ink;
  final Color muted;
  final Color faint;
  final Color hairline;
  final Color accent;
  final Color onAccent;

  /// Wallpaper stand-in behind previews, top-left to bottom-right.
  final List<Color> stage;

  static const light = Palette(
    canvas: Color(0xFFF3F2EE),
    surface: Color(0xFFFFFFFF),
    raised: Color(0xFFE7E5DF),
    ink: Color(0xFF111113),
    muted: Color(0xFF6C6B67),
    faint: Color(0xFFB9B7B1),
    hairline: Color(0x14111113),
    accent: Color(0xFFFF5A36),
    onAccent: Color(0xFFFFFFFF),
    stage: [Color(0xFFDCD7CE), Color(0xFFCBCFD7), Color(0xFFD9D1C8)],
  );

  static const dark = Palette(
    canvas: Color(0xFF0A0A0B),
    surface: Color(0xFF151517),
    raised: Color(0xFF232326),
    ink: Color(0xFFF2F1EC),
    muted: Color(0xFF8E8D89),
    faint: Color(0xFF45454A),
    hairline: Color(0x1AF2F1EC),
    accent: Color(0xFFFF6A47),
    onAccent: Color(0xFF0A0A0B),
    stage: [Color(0xFF1F2229), Color(0xFF34303B), Color(0xFF27302D)],
  );

  /// The theme always installs a palette (see TesseraTheme).
  static Palette of(BuildContext context) =>
      Theme.of(context).extension<Palette>()!;

  @override
  Palette copyWith() => this;

  @override
  Palette lerp(Palette? other, double t) {
    if (other == null) return this;
    Color c(Color a, Color b) => Color.lerp(a, b, t)!;
    return Palette(
      canvas: c(canvas, other.canvas),
      surface: c(surface, other.surface),
      raised: c(raised, other.raised),
      ink: c(ink, other.ink),
      muted: c(muted, other.muted),
      faint: c(faint, other.faint),
      hairline: c(hairline, other.hairline),
      accent: c(accent, other.accent),
      onAccent: c(onAccent, other.onAccent),
      stage: [for (var i = 0; i < 3; i++) c(stage[i], other.stage[i])],
    );
  }
}

/// Spacing on a 4dp grid.
abstract final class Gap {
  static const double xs = 4;
  static const double s = 8;
  static const double m = 12;
  static const double l = 16;
  static const double xl = 24;
  static const double xxl = 32;
  static const double page = 20;
}

abstract final class Radii {
  static const double s = 12;
  static const double m = 20;
  static const double l = 28;
  static const double pill = 999;
}

/// Type families bundled in pubspec.yaml.
abstract final class Fonts {
  /// Headlines and names.
  static const display = 'Space Grotesk';

  /// Running text and controls.
  static const body = 'Inter Tight';

  /// Overlines, sizes and numeric readouts.
  static const mono = 'JetBrains Mono';
}

abstract final class Motion {
  static const quick = Duration(milliseconds: 160);
  static const base = Duration(milliseconds: 280);
  static const slow = Duration(milliseconds: 480);
  static const page = Duration(milliseconds: 440);

  /// Standard ease-out for anything that settles.
  static const Curve ease = Cubic(0.2, 0.0, 0.0, 1.0);

  /// Entrances: fast start, long soft landing.
  static const Curve enter = Cubic(0.05, 0.7, 0.1, 1.0);

  /// Pressed surfaces: stiff and critically damped, so a press never wobbles.
  static const press = SpringDescription(mass: 1, stiffness: 900, damping: 60);

  /// The editor stage: soft and slightly underdamped, so a change lands with
  /// one small overshoot.
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
