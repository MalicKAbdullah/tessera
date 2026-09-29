import 'package:flutter/material.dart';

/// Quiet, neutral chrome so the widgets themselves carry the colour.
class TesseraTheme {
  static const _ease = Cubic(0.2, 0.0, 0.0, 1.0);
  static const Curve ease = _ease;
  static const Duration motion = Duration(milliseconds: 420);

  static ThemeData light() => _build(
        brightness: Brightness.light,
        background: const Color(0xFFF6F5F2),
        surface: const Color(0xFFFFFFFF),
        onSurface: const Color(0xFF1C1D20),
        muted: const Color(0xFF7B7A76),
      );

  static ThemeData dark() => _build(
        brightness: Brightness.dark,
        background: const Color(0xFF0F1012),
        surface: const Color(0xFF18191C),
        onSurface: const Color(0xFFEDEBE6),
        muted: const Color(0xFF8C8B87),
      );

  static ThemeData _build({
    required Brightness brightness,
    required Color background,
    required Color surface,
    required Color onSurface,
    required Color muted,
  }) {
    final scheme = ColorScheme.fromSeed(
      seedColor: const Color(0xFFC9A77C),
      brightness: brightness,
    ).copyWith(
      surface: surface,
      onSurface: onSurface,
      onSurfaceVariant: muted,
      primary: onSurface,
      onPrimary: background,
      outlineVariant: onSurface.withValues(alpha: 0.08),
    );
    final base = ThemeData(
      useMaterial3: true,
      colorScheme: scheme,
      scaffoldBackgroundColor: background,
      splashFactory: InkSparkle.splashFactory,
    );
    return base.copyWith(
      appBarTheme: AppBarTheme(
        backgroundColor: background,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        scrolledUnderElevation: 0,
        centerTitle: false,
        foregroundColor: onSurface,
      ),
      textTheme: base.textTheme.apply(
        bodyColor: onSurface,
        displayColor: onSurface,
      ),
      sliderTheme: SliderThemeData(
        trackHeight: 2,
        activeTrackColor: onSurface,
        inactiveTrackColor: onSurface.withValues(alpha: 0.12),
        thumbColor: onSurface,
        overlayColor: onSurface.withValues(alpha: 0.06),
        thumbShape: const RoundSliderThumbShape(enabledThumbRadius: 7),
      ),
      pageTransitionsTheme: const PageTransitionsTheme(builders: {
        TargetPlatform.android: FadeForwardsPageTransitionsBuilder(),
        TargetPlatform.iOS: CupertinoPageTransitionsBuilder(),
      }),
    );
  }
}
