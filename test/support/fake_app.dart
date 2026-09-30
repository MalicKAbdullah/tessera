import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:tessera/src/core/theme.dart';
import 'package:tessera/src/features/widgets/models/catalog.dart';
import 'package:tessera/src/features/widgets/models/widget_style.dart';
import 'package:tessera/src/features/widgets/providers/widget_providers.dart';
import 'package:tessera/src/features/widgets/services/engine.dart';

/// A 1×1 transparent PNG.
final pixel = base64Decode(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==',
);

const style = WidgetStyle(
  font: 'sans',
  weight: 400,
  scale: 1,
  tracking: 0,
  text: Color(0xFFFFFFFF),
  accent: Color(0xFFFF5A1F),
  background: Background(
    kind: BgKind.solid,
    color: Color(0xFF0C0C0D),
    color2: Color(0xFF1F2023),
  ),
  opacity: 1,
  radius: 24,
  padding: 16,
);

const catalog = Catalog(
  fonts: [
    FontInfo('sans', 'Inter Tight', [100, 400, 700]),
    FontInfo('mono', 'JetBrains Mono', [400, 700]),
  ],
  categories: [CategoryInfo('clock', 'Clock'), CategoryInfo('note', 'Note')],
  designs: [
    DesignInfo(
      id: 'clock.minimal',
      category: 'clock',
      name: 'Minimal',
      blurb: 'Time and date.',
      motion: null,
      sizes: [
        SizeInfo('wide', 4, 2, 350, 170),
        SizeInfo('small', 2, 2, 170, 170),
      ],
      defaults: style,
      toggles: [SwitchToggle('seconds', 'Seconds', false)],
    ),
    DesignInfo(
      id: 'note.classic',
      category: 'note',
      name: 'Note',
      blurb: 'A line worth keeping.',
      motion: null,
      sizes: [SizeInfo('wide', 4, 2, 350, 170)],
      defaults: style,
      toggles: [],
    ),
  ],
);

class FakeEngine extends Engine {
  bool pinAccepted = true;
  final List<PlacedWidget> placedWidgets = [];

  @override
  Future<Uint8List> render(String design, WidgetStyle style, SizeInfo size) =>
      Future.value(pixel);

  @override
  Future<Uint8List> specimen(
    String font,
    int weight,
    String text,
    double size,
    Color color,
  ) => Future.value(pixel);

  @override
  Future<List<PlacedWidget>> placed() => Future.value(List.of(placedWidgets));

  @override
  Future<bool> canPin() => Future.value(true);

  @override
  Future<bool> pin(String design, WidgetStyle style, SizeInfo size) =>
      Future.value(pinAccepted);
}

Future<ProviderContainer> fakeContainer(
  FakeEngine engine, {
  Map<String, Object> prefs = const {},
}) async {
  SharedPreferences.setMockInitialValues(prefs);
  final sp = await SharedPreferences.getInstance();
  return ProviderContainer(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(sp),
      engineProvider.overrideWithValue(engine),
      catalogProvider.overrideWithValue(catalog),
      minuteProvider.overrideWith((ref) => Stream.value(DateTime(2026))),
    ],
  );
}

Widget themed(ProviderContainer container, Widget child) =>
    UncontrolledProviderScope(
      container: container,
      child: MaterialApp(theme: TesseraTheme.light(), home: child),
    );
