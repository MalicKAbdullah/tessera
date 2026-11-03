import 'package:core_ui/core_ui.dart';
import 'package:core_update/core_update.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tessera/src/features/gallery/screens/gallery_screen.dart';
import 'package:tessera/src/features/settings/settings_providers.dart';
import 'package:tessera/src/features/settings/settings_screen.dart';

import 'support/fake_app.dart';

const _update = UpdateInfo(
  version: '9.0.0',
  notes: '',
  downloadUrl: 'https://example.invalid/app.apk',
  releaseUrl: 'https://example.invalid/release',
);

void main() {
  testWidgets('appearance comes first and applies at once', (tester) async {
    final container = await fakeContainer(FakeEngine());
    await tester.pumpWidget(themed(container, const SettingsScreen()));
    await tester.pumpAndSettle();

    final appearance = tester.getTopLeft(find.text('Appearance'));
    final updates = tester.getTopLeft(find.text('Updates'));
    expect(appearance.dy, lessThan(updates.dy));
    expect(container.read(appearanceProvider).mode, ThemeMode.system);

    await tester.tap(find.text('Dark'));
    await tester.pump();
    expect(container.read(appearanceProvider).mode, ThemeMode.dark);
  });

  testWidgets('about carries the Offlyne endorsement', (tester) async {
    final container = await fakeContainer(FakeEngine());
    await tester.pumpWidget(themed(container, const SettingsScreen()));
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(find.text('Licences'), 200);
    expect(find.byType(SuiteEndorsement), findsOneWidget);
    expect(find.text('v0.2.1'), findsOneWidget);
  });

  testWidgets('turning off the update check stops it', (tester) async {
    final container = await fakeContainer(
      FakeEngine(),
      updates: FakeUpdateService()..next = _update,
    );
    await tester.pumpWidget(themed(container, const SettingsScreen()));
    await tester.pumpAndSettle();
    expect(await container.read(updateCheckProvider.future), isNotNull);

    await tester.tap(find.byType(Switch));
    await tester.pumpAndSettle();
    expect(container.read(updateAutoCheckProvider), isFalse);
    expect(await container.read(updateCheckProvider.future), isNull);
  });

  testWidgets('the gallery offers a newer release until dismissed', (
    tester,
  ) async {
    final container = await fakeContainer(
      FakeEngine(),
      prefs: {'introSeen': true},
      updates: FakeUpdateService()..next = _update,
    );
    await tester.pumpWidget(themed(container, const GalleryScreen()));
    await tester.pumpAndSettle();
    expect(find.byType(UpdateBanner), findsOneWidget);

    await tester.tap(find.text('Remind later'));
    await tester.pumpAndSettle();
    expect(find.byType(UpdateBanner), findsNothing);
  });

  testWidgets('the launcher configure flow shows no update banner', (
    tester,
  ) async {
    final container = await fakeContainer(
      FakeEngine(),
      updates: FakeUpdateService()..next = _update,
    );
    await tester.pumpWidget(
      themed(container, const GalleryScreen(configuring: 7)),
    );
    await tester.pumpAndSettle();
    expect(find.byType(UpdateBanner), findsNothing);
  });
}
