import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:tessera/src/core/router/app_router.dart';
import 'package:tessera/src/core/theme.dart';
import 'package:tessera/src/features/editor/screens/editor_screen.dart';
import 'package:tessera/src/features/widgets/models/catalog.dart';
import 'package:tessera/src/features/widgets/models/widget_style.dart';
import 'package:tessera/src/features/widgets/providers/widget_providers.dart';
import 'package:tessera/src/features/widgets/services/engine.dart';

import 'support/fake_app.dart';

PlacedWidget _placed(int id, WidgetStyle style) => PlacedWidget(
  id: id,
  category: 'clock',
  size: 'wide',
  bound: true,
  design: 'clock.minimal',
  style: style,
);

const _target = PlacedTarget('clock.minimal', 5, configuring: false);

late GoRouter _router;

Future<ProviderContainer> _open(
  WidgetTester tester,
  FakeEngine engine,
  LaunchTarget? target, {
  Future<void> Function(ProviderContainer)? before,
}) async {
  tester.view
    ..physicalSize = const Size(420, 3200)
    ..devicePixelRatio = 1;
  addTearDown(tester.view.reset);
  final container = await fakeContainer(engine, prefs: {'introSeen': true});
  await before?.call(container);
  await tester.pumpWidget(
    UncontrolledProviderScope(
      container: container,
      child: MaterialApp.router(
        theme: tesseraTheme(Brightness.light),
        routerConfig: _router = createRouter(
          initialLocation: launchLocation(target),
        ),
      ),
    ),
  );
  await tester.pumpAndSettle();
  return container;
}

void main() {
  group('launchLocation', () {
    test('a tapped widget opens its own editor', () {
      expect(launchLocation(const LaunchEdit(5)), '/widget/5');
      expect(launchLocation(const LaunchEdit(812)), '/widget/812');
    });

    test('the launcher configure flow and a plain start', () {
      expect(launchLocation(const LaunchConfigure(9)), '/configure/9');
      expect(launchLocation(null), '/');
    });

    test('reads the native launch target', () {
      expect(
        launchLocation(LaunchTarget.fromMap({'mode': 'edit', 'id': 7})),
        '/widget/7',
      );
      expect(LaunchTarget.fromMap(null), isNull);
    });
  });

  testWidgets('a widget tap opens the editor on that widget’s binding', (
    tester,
  ) async {
    final mine = style.copyWith(radius: 8, opacity: 0.6);
    final engine = FakeEngine()
      ..placedWidgets.addAll([_placed(4, style), _placed(5, mine)]);
    final container = await _open(tester, engine, const LaunchEdit(5));

    expect(find.byType(PlacedEditorScreen), findsOneWidget);
    expect(container.read(styleProvider(_target)), mine);
  });

  testWidgets('the editor starts from the binding now, not an earlier read', (
    tester,
  ) async {
    final engine = FakeEngine()..placedWidgets.add(_placed(5, style));
    final edited = style.copyWith(radius: 4);
    final container = await _open(
      tester,
      engine,
      const LaunchEdit(5),
      before: (c) async {
        // The gallery read the widget before it was edited elsewhere.
        await c.read(placedProvider.future);
        engine.placedWidgets[0] = _placed(5, edited);
      },
    );

    expect(container.read(styleProvider(_target)), edited);
  });

  testWidgets('edits apply back to the tapped widget', (tester) async {
    final engine = FakeEngine()..placedWidgets.add(_placed(5, style));
    final container = await _open(tester, engine, const LaunchEdit(5));

    container
        .read(styleProvider(_target).notifier)
        .update((s) => s.copyWith(radius: 12));
    await tester.pump(const Duration(milliseconds: 400));

    expect(engine.binds.single.$1, 5);
    expect(engine.binds.single.$2, 'clock.minimal');
    expect(engine.binds.single.$3.radius, 12);
  });

  testWidgets('leaving the editor right after an edit still applies it', (
    tester,
  ) async {
    final engine = FakeEngine()
      ..placedWidgets.addAll([_placed(5, style), _placed(6, style)]);
    final container = await _open(tester, engine, const LaunchEdit(5));

    container
        .read(styleProvider(_target).notifier)
        .update((s) => s.copyWith(radius: 20));
    // Another widget tapped before the edit settled.
    _router.go('/widget/6');
    await tester.pumpAndSettle();

    expect(engine.binds.single.$3.radius, 20);
  });

  testWidgets('a widget removed before the tap shows that it is gone', (
    tester,
  ) async {
    final engine = FakeEngine()..placedWidgets.add(_placed(4, style));
    await _open(tester, engine, const LaunchEdit(5));

    expect(find.text('This widget is gone'), findsOneWidget);
  });

  testWidgets('a widget removed while its editor is open shows that it is '
      'gone', (tester) async {
    final engine = FakeEngine()..placedWidgets.add(_placed(5, style));
    final container = await _open(tester, engine, const LaunchEdit(5));

    engine.placedWidgets.clear();
    container
        .read(styleProvider(_target).notifier)
        .update((s) => s.copyWith(radius: 12));
    await tester.pump(const Duration(milliseconds: 400));
    await tester.pumpAndSettle();

    expect(engine.binds, isEmpty);
    expect(find.text('This widget is gone'), findsOneWidget);
  });
}
