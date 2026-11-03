import 'package:flutter_test/flutter_test.dart';
import 'package:tessera/src/features/gallery/screens/gallery_screen.dart';
import 'package:tessera/src/features/widgets/models/catalog.dart';

import 'support/fake_app.dart';

void main() {
  testWidgets('placed widgets show on the home-screen strip', (tester) async {
    final engine = FakeEngine()
      ..placedWidgets.add(
        const PlacedWidget(
          id: 3,
          category: 'clock',
          size: 'wide',
          bound: true,
          design: 'clock.minimal',
          style: style,
        ),
      );
    final container = await fakeContainer(engine, prefs: {'introSeen': true});
    await tester.pumpWidget(themed(container, const GalleryScreen()));
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(find.text('ON YOUR HOME SCREEN'), findsOneWidget);
    expect(find.bySemanticsLabel('Edit Minimal'), findsOneWidget);
  });
}
