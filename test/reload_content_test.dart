import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:tessera/src/features/widgets/data/tessera_store.dart';
import 'package:tessera/src/features/widgets/models/widget_content.dart';
import 'package:tessera/src/features/widgets/providers/widget_providers.dart';

import 'support/fake_app.dart';

void main() {
  test('content written by the configure engine is read on resume', () async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final docs = MemoryDocuments();
    final container = ProviderContainer(
      overrides: [
        sharedPreferencesProvider.overrideWithValue(prefs),
        storeProvider.overrideWithValue(await TesseraStore.open(prefs, docs)),
      ],
    );
    addTearDown(container.dispose);
    expect(container.read(contentProvider).note, 'Less, but better.');

    // The configure screen's engine writes the same document behind this
    // engine's in-memory copy.
    await docs.write('content', const WidgetContent(note: 'Hi').toJson());
    await reloadContent(container);

    expect(container.read(contentProvider).note, 'Hi');
  });
}
