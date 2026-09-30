import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tessera/src/features/gallery/screens/gallery_screen.dart';
import 'package:tessera/src/features/widgets/providers/widget_providers.dart';

import 'support/fake_app.dart';

void main() {
  final intro = find.byKey(const Key('intro'));

  testWidgets('first launch plays the intro and remembers it', (tester) async {
    final container = await fakeContainer(FakeEngine());
    await tester.pumpWidget(themed(container, const GalleryScreen()));
    await tester.pump();
    expect(intro, findsOneWidget);
    expect(container.read(storeProvider).introSeen, isFalse);

    await tester.pumpAndSettle();
    expect(intro, findsNothing);
    expect(container.read(storeProvider).introSeen, isTrue);
    expect(find.text('Minimal'), findsOneWidget);
  });

  testWidgets('skipping ends the intro early and remembers it', (tester) async {
    final container = await fakeContainer(FakeEngine());
    await tester.pumpWidget(themed(container, const GalleryScreen()));
    await tester.pump(const Duration(milliseconds: 300));
    await tester.tap(find.text('SKIP'));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 600));
    await tester.pump();
    expect(intro, findsNothing);
    expect(container.read(storeProvider).introSeen, isTrue);
  });

  testWidgets('later launches go straight to the gallery', (tester) async {
    final container = await fakeContainer(
      FakeEngine(),
      prefs: {'introSeen': true},
    );
    await tester.pumpWidget(themed(container, const GalleryScreen()));
    await tester.pump();
    expect(intro, findsNothing);
    await tester.pumpAndSettle();
    expect(find.text('Minimal'), findsOneWidget);
  });

  testWidgets('the launcher configure flow never shows the intro', (
    tester,
  ) async {
    final container = await fakeContainer(FakeEngine());
    await tester.pumpWidget(
      themed(container, const GalleryScreen(configuring: 7)),
    );
    await tester.pump();
    expect(intro, findsNothing);
    expect(container.read(storeProvider).introSeen, isFalse);
  });
}
