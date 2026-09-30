import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:tessera/src/core/theme.dart';
import 'package:tessera/src/features/editor/screens/editor_screen.dart';
import 'package:tessera/src/features/editor/widgets/calendar_access.dart';
import 'package:tessera/src/features/editor/widgets/content_controls.dart';
import 'package:tessera/src/features/editor/widgets/photo_controls.dart';
import 'package:tessera/src/features/widgets/models/catalog.dart';
import 'package:tessera/src/features/widgets/models/widget_style.dart';
import 'package:tessera/src/features/widgets/providers/widget_providers.dart';

import 'support/fake_app.dart';

const _target = DraftTarget('clock.minimal');

Future<ProviderContainer> _pumpEditor(
  WidgetTester tester,
  FakeEngine engine,
) async {
  tester.view
    ..physicalSize = const Size(420, 3200)
    ..devicePixelRatio = 1;
  addTearDown(tester.view.reset);
  final container = await fakeContainer(engine);
  final router = GoRouter(
    initialLocation: '/design/clock.minimal',
    routes: [
      GoRoute(
        path: '/',
        builder: (_, _) => const Scaffold(body: Text('gallery')),
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
      child: MaterialApp.router(
        theme: TesseraTheme.light(),
        routerConfig: router,
      ),
    ),
  );
  await tester.pumpAndSettle();
  return container;
}

void main() {
  testWidgets('a surface choice updates the preview style and the draft', (
    tester,
  ) async {
    final container = await _pumpEditor(tester, FakeEngine());
    await tester.tap(find.text('Gradient'));
    await tester.pumpAndSettle();

    expect(
      container.read(styleProvider(_target)).background.kind,
      BgKind.gradient,
    );
    expect(
      container.read(storeProvider).draft(_target.design)!.background.kind,
      BgKind.gradient,
    );
    expect(find.text('Blends into'), findsOneWidget);
  });

  testWidgets('a design toggle flips from its default', (tester) async {
    final container = await _pumpEditor(tester, FakeEngine());
    await tester.tap(find.text('Seconds'));
    await tester.pumpAndSettle();
    expect(container.read(styleProvider(_target)).toggles['seconds'], true);
  });

  testWidgets('switching size re-targets the pin action', (tester) async {
    await _pumpEditor(tester, FakeEngine());
    expect(find.text('Add 4×2 to home screen'), findsOneWidget);
    await tester.tap(find.text('2×2'));
    await tester.pumpAndSettle();
    expect(find.text('Add 2×2 to home screen'), findsOneWidget);
  });

  testWidgets('reset restores the design defaults', (tester) async {
    final container = await _pumpEditor(tester, FakeEngine());
    await tester.tap(find.text('Gradient'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Reset to design defaults'));
    await tester.pumpAndSettle();
    expect(container.read(styleProvider(_target)), style);
  });

  testWidgets('a declined pin says so and offers another try', (tester) async {
    await _pumpEditor(tester, FakeEngine()..pinAccepted = false);
    await tester.tap(find.text('Add 4×2 to home screen'));
    await tester.pumpAndSettle();
    expect(find.textContaining('Your launcher declined'), findsOneWidget);
    expect(find.text('Try again'), findsOneWidget);
  });

  testWidgets('a pin counts as placed only when the widget arrives', (
    tester,
  ) async {
    final engine = FakeEngine();
    final container = await _pumpEditor(tester, engine);
    await tester.tap(find.text('Add 4×2 to home screen'));
    await tester.pumpAndSettle();
    expect(find.text('Confirm in the launcher prompt'), findsOneWidget);

    engine.placedWidgets.add(
      const PlacedWidget(
        id: 12,
        category: 'clock',
        size: 'wide',
        bound: true,
        design: 'clock.minimal',
        style: style,
      ),
    );
    container.read(dataRevisionProvider.notifier).state++;
    await tester.pump();
    await tester.pump();
    expect(find.text('On your home screen'), findsOneWidget);

    await tester.pumpAndSettle(const Duration(seconds: 1));
    await tester.pump(const Duration(seconds: 2));
    await tester.pumpAndSettle();
    expect(find.text('gallery'), findsOneWidget);
  });

  test('content controls plug in per category', () {
    expect(contentControls('note'), [
      isA<NoteControls>(),
      isA<ChecklistControls>(),
    ]);
    expect(contentControls('weather').single, isA<WeatherControls>());
    expect(contentControls('countdown'), [
      isA<CountdownControls>(),
      isA<EventsControls>(),
    ]);
    expect(contentControls('photo').single, isA<PhotoControls>());
    expect(contentControls('calendar').single, isA<CalendarAccessControls>());
    expect(contentControls('clock'), isEmpty);
  });
}
