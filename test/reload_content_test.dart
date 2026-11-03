import 'dart:convert';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:tessera/src/features/widgets/models/widget_content.dart';
import 'package:tessera/src/features/widgets/providers/widget_providers.dart';

void main() {
  test('content written by the configure engine is read on resume', () async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final container = ProviderContainer(
      overrides: [sharedPreferencesProvider.overrideWithValue(prefs)],
    );
    addTearDown(container.dispose);
    expect(container.read(contentProvider).note, 'Less, but better.');

    // The configure screen's engine writes the same file behind this
    // engine's in-memory copy.
    SharedPreferences.setMockInitialValues({
      'flutter.content': jsonEncode(const WidgetContent(note: 'Hi').toJson()),
    });
    await reloadContent(container);

    expect(container.read(contentProvider).note, 'Hi');
  });
}
