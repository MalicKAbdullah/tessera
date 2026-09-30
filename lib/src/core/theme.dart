import 'package:flutter/material.dart';

import 'design/tokens.dart';

/// Monochrome chrome on the bundled faces. Taps answer with scale and haptics
/// (see Pressable), so Material's ink splashes are switched off.
class TesseraTheme {
  static ThemeData light() => _build(Brightness.light, Palette.light);
  static ThemeData dark() => _build(Brightness.dark, Palette.dark);

  static TextTheme _type(Color ink, Color muted) {
    TextStyle display(double size, FontWeight w, double tracking) => TextStyle(
      fontFamily: Fonts.display,
      fontSize: size,
      fontWeight: w,
      letterSpacing: tracking,
      height: 1.08,
      color: ink,
    );
    TextStyle body(double size, FontWeight w, {double height = 1.4}) =>
        TextStyle(
          fontFamily: Fonts.body,
          fontSize: size,
          fontWeight: w,
          height: height,
          color: ink,
        );
    TextStyle mono(double size, double tracking) => TextStyle(
      fontFamily: Fonts.mono,
      fontSize: size,
      fontWeight: FontWeight.w500,
      letterSpacing: tracking,
      color: muted,
    );
    return TextTheme(
      displayLarge: display(48, FontWeight.w300, -1.8),
      displayMedium: display(40, FontWeight.w300, -1.4),
      displaySmall: display(34, FontWeight.w300, -1.1),
      headlineMedium: display(28, FontWeight.w400, -0.8),
      headlineSmall: display(24, FontWeight.w400, -0.6),
      titleLarge: display(21, FontWeight.w500, -0.4),
      titleMedium: display(17, FontWeight.w500, -0.2),
      titleSmall: body(15, FontWeight.w500),
      bodyLarge: body(16, FontWeight.w400),
      bodyMedium: body(14.5, FontWeight.w400),
      bodySmall: body(13, FontWeight.w400).copyWith(color: muted),
      labelLarge: body(15, FontWeight.w500, height: 1.2),
      labelMedium: mono(11.5, 0.4),
      labelSmall: mono(10.5, 1.6),
    );
  }

  static ThemeData _build(Brightness brightness, Palette p) {
    final scheme = ColorScheme(
      brightness: brightness,
      primary: p.ink,
      onPrimary: p.canvas,
      secondary: p.accent,
      onSecondary: p.onAccent,
      error: p.accent,
      onError: p.onAccent,
      surface: p.canvas,
      onSurface: p.ink,
      onSurfaceVariant: p.muted,
      surfaceContainerLowest: p.surface,
      surfaceContainerLow: p.surface,
      surfaceContainer: p.surface,
      surfaceContainerHigh: p.surface,
      surfaceContainerHighest: p.raised,
      outline: p.faint,
      outlineVariant: p.hairline,
    );
    final text = _type(p.ink, p.muted);
    return ThemeData(
      useMaterial3: true,
      brightness: brightness,
      colorScheme: scheme,
      extensions: [p],
      fontFamily: Fonts.body,
      textTheme: text,
      scaffoldBackgroundColor: p.canvas,
      canvasColor: p.canvas,
      splashFactory: NoSplash.splashFactory,
      splashColor: Colors.transparent,
      highlightColor: Colors.transparent,
      hoverColor: Colors.transparent,
      dividerColor: p.hairline,
      textSelectionTheme: TextSelectionThemeData(
        cursorColor: p.accent,
        selectionColor: p.accent.withValues(alpha: 0.25),
        selectionHandleColor: p.accent,
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: p.surface,
        labelStyle: text.bodyMedium?.copyWith(color: p.muted),
        floatingLabelStyle: text.labelMedium?.copyWith(color: p.muted),
        helperStyle: text.bodySmall,
        counterStyle: text.labelMedium,
        contentPadding: const EdgeInsets.symmetric(
          horizontal: Gap.l,
          vertical: Gap.m,
        ),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(Radii.s),
          borderSide: BorderSide(color: p.hairline),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(Radii.s),
          borderSide: BorderSide(color: p.hairline),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(Radii.s),
          borderSide: BorderSide(color: p.ink, width: 1.2),
        ),
      ),
      sliderTheme: SliderThemeData(
        trackHeight: 3,
        activeTrackColor: p.ink,
        inactiveTrackColor: p.hairline,
        thumbColor: p.ink,
        overlayColor: p.ink.withValues(alpha: 0.06),
        overlayShape: const RoundSliderOverlayShape(overlayRadius: 18),
        thumbShape: const RoundSliderThumbShape(
          enabledThumbRadius: 8,
          elevation: 0,
          pressedElevation: 0,
        ),
        trackShape: const RoundedRectSliderTrackShape(),
      ),
      switchTheme: SwitchThemeData(
        thumbColor: WidgetStateProperty.resolveWith(
          (s) => s.contains(WidgetState.selected) ? p.canvas : p.muted,
        ),
        trackColor: WidgetStateProperty.resolveWith(
          (s) => s.contains(WidgetState.selected) ? p.ink : p.surface,
        ),
        trackOutlineColor: WidgetStateProperty.resolveWith(
          (s) => s.contains(WidgetState.selected) ? p.ink : p.faint,
        ),
      ),
      datePickerTheme: DatePickerThemeData(
        backgroundColor: p.surface,
        headerHeadlineStyle: text.headlineSmall,
      ),
      bottomSheetTheme: BottomSheetThemeData(
        backgroundColor: p.surface,
        dragHandleColor: p.faint,
        showDragHandle: true,
      ),
      progressIndicatorTheme: ProgressIndicatorThemeData(color: p.ink),
    );
  }
}
