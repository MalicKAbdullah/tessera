@Tags(['golden'])
library;

import 'dart:convert';
import 'dart:ui' as ui;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:tessera/src/core/theme.dart';
import 'package:tessera/src/features/editor/screens/editor_screen.dart';
import 'package:tessera/src/features/gallery/screens/gallery_screen.dart';

import '../support/fake_app.dart';

/// Golden images of the app chrome (gallery, editor, intro) at phone size in
/// both themes. The engine is faked with a placeholder preview, so these
/// cover the Flutter chrome only; widget pixels are covered by the
/// Roborazzi screenshots under android/.
///
/// Update with `flutter test --update-goldens test/goldens`.
void main() {
  const phone = Size(412, 915);
  final themes = {'light': TesseraTheme.light(), 'dark': TesseraTheme.dark()};
  late Map<String, Uint8List> previews;
  late Map<String, Uint8List> specimens;

  setUpAll(() async {
    goldenFileComparator = _TolerantComparator(
      (goldenFileComparator as LocalFileComparator).basedir.resolve('x.png'),
    );
    await _loadFonts();
    previews = {
      'wide': await _placeholder(350, 170),
      'small': await _placeholder(170, 170),
    };
    specimens = {
      'sans': await _specimen('Inter Tight'),
      'mono': await _specimen('JetBrains Mono'),
    };
  });

  Future<FakeEngine> engine() async => FakeEngine()
    ..previews.addAll(previews)
    ..specimens.addAll(specimens);

  void atPhoneSize(WidgetTester tester) {
    tester.view
      ..physicalSize = phone * 2
      ..devicePixelRatio = 2;
    addTearDown(tester.view.reset);
  }

  /// Image.memory decodes on a real isolate, which fake async never waits for.
  Future<void> settleImages(WidgetTester tester) async {
    await tester.runAsync(() async {
      for (final element in find.byType(Image).evaluate()) {
        final image = (element.widget as Image).image;
        await precacheImage(image, element);
      }
    });
    await tester.pumpAndSettle();
  }

  for (final MapEntry(key: name, value: theme) in themes.entries) {
    testWidgets('gallery $name', (tester) async {
      atPhoneSize(tester);
      final container = await fakeContainer(
        await engine(),
        prefs: {'introSeen': true},
      );
      await tester.pumpWidget(
        themed(container, const GalleryScreen(), theme: theme),
      );
      await tester.pumpAndSettle();
      await settleImages(tester);
      await expectLater(
        find.byType(GalleryScreen),
        matchesGoldenFile('gallery_$name.png'),
      );
    });

    testWidgets('intro $name', (tester) async {
      atPhoneSize(tester);
      final container = await fakeContainer(await engine());
      await tester.pumpWidget(
        themed(container, const GalleryScreen(), theme: theme),
      );
      // Mark assembled and wordmark risen, before the veil lifts.
      for (var i = 0; i < 17; i++) {
        await tester.pump(const Duration(milliseconds: 100));
      }
      await expectLater(
        find.byType(GalleryScreen),
        matchesGoldenFile('intro_$name.png'),
      );
      await tester.pumpAndSettle();
    });

    testWidgets('editor $name', (tester) async {
      atPhoneSize(tester);
      final container = await fakeContainer(await engine());
      final router = GoRouter(
        initialLocation: '/design/clock.minimal',
        routes: [
          GoRoute(
            path: '/',
            builder: (_, _) => const Scaffold(),
            routes: [
              GoRoute(
                path: 'design/:design',
                builder: (_, state) =>
                    DraftEditorScreen(design: state.pathParameters['design']!),
              ),
            ],
          ),
        ],
      );
      await tester.pumpWidget(
        UncontrolledProviderScope(
          container: container,
          child: MaterialApp.router(theme: theme, routerConfig: router),
        ),
      );
      await tester.pumpAndSettle();
      await settleImages(tester);
      await expectLater(
        find.byType(DraftEditorScreen),
        matchesGoldenFile('editor_$name.png'),
      );
    });
  }
}

/// Loads every family in the font manifest (the bundled faces and Material
/// Icons) so goldens show real text instead of the test font's boxes.
Future<void> _loadFonts() async {
  final manifest =
      jsonDecode(await rootBundle.loadString('FontManifest.json')) as List;
  for (final entry in manifest.cast<Map<String, dynamic>>()) {
    final loader = FontLoader(entry['family'] as String);
    for (final font in (entry['fonts'] as List).cast<Map<String, dynamic>>()) {
      loader.addFont(rootBundle.load(font['asset'] as String));
    }
    await loader.load();
  }
}

/// A stand-in widget preview: a graphite tile with a lime dot and two bars.
Future<Uint8List> _placeholder(double w, double h) async {
  const scale = 2.0;
  final recorder = ui.PictureRecorder();
  final canvas = Canvas(recorder)..scale(scale);
  final rect = RRect.fromRectAndRadius(
    Offset.zero & Size(w, h),
    const Radius.circular(24),
  );
  canvas
    ..drawRRect(rect, Paint()..color = const Color(0xFF16171A))
    ..drawCircle(
      const Offset(28, 28),
      6,
      Paint()..color = const Color(0xFFD4FF3A),
    )
    ..drawRRect(
      RRect.fromLTRBR(20, h - 58, w * 0.62, h - 36, const Radius.circular(6)),
      Paint()..color = const Color(0xFFF2F3F5),
    )
    ..drawRRect(
      RRect.fromLTRBR(20, h - 28, w * 0.4, h - 20, const Radius.circular(4)),
      Paint()..color = const Color(0x66F2F3F5),
    );
  final image = await recorder.endRecording().toImage(
    (w * scale).round(),
    (h * scale).round(),
  );
  final bytes = await image.toByteData(format: ui.ImageByteFormat.png);
  return bytes!.buffer.asUint8List();
}

/// A stand-in font specimen: 'Aa 12:45' in a mid grey that reads on both themes.
Future<Uint8List> _specimen(String family) async {
  const scale = 2.0;
  final builder =
      ui.ParagraphBuilder(ui.ParagraphStyle(fontFamily: family, fontSize: 22))
        ..pushStyle(ui.TextStyle(color: const Color(0xFF8A8F98)))
        ..addText('Aa 12:45');
  final paragraph = builder.build()
    ..layout(const ui.ParagraphConstraints(width: 400));
  final w = paragraph.maxIntrinsicWidth.ceilToDouble();
  final h = paragraph.height.ceilToDouble();
  final recorder = ui.PictureRecorder();
  Canvas(recorder)
    ..scale(scale)
    ..drawParagraph(paragraph, Offset.zero);
  final image = await recorder.endRecording().toImage(
    (w * scale).round(),
    (h * scale).round(),
  );
  final bytes = await image.toByteData(format: ui.ImageByteFormat.png);
  return bytes!.buffer.asUint8List();
}

/// Font rasterisation differs slightly between macOS and the Linux CI
/// runner, so a golden passes while under 3% of its pixels differ.
class _TolerantComparator extends LocalFileComparator {
  _TolerantComparator(super.testFile);

  static const _tolerance = 0.03;

  @override
  Future<bool> compare(Uint8List imageBytes, Uri golden) async {
    final result = await GoldenFileComparator.compareLists(
      imageBytes,
      await getGoldenBytes(golden),
    );
    if (result.passed || result.diffPercent <= _tolerance) {
      result.dispose();
      return true;
    }
    final error = await generateFailureOutput(result, golden, basedir);
    result.dispose();
    throw FlutterError(error);
  }
}
