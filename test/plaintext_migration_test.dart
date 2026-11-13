import 'dart:convert';

import 'package:flutter/painting.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:tessera/src/features/widgets/data/tessera_store.dart';
import 'package:tessera/src/features/widgets/models/widget_content.dart';
import 'package:tessera/src/features/widgets/models/widget_style.dart';
import 'package:tessera/src/features/widgets/services/migration.dart';

import 'support/fake_app.dart';

void main() {
  const photoStyle = WidgetStyle(
    font: 'sans',
    weight: 400,
    scale: 1,
    tracking: 0,
    text: Color(0xFFFFFFFF),
    accent: Color(0xFFD4FF3A),
    background: Background(
      kind: BgKind.photo,
      color: Color(0xFF000000),
      color2: Color(0xFF000000),
      photo:
          '/data/user/0/com.malickabdullah.tessera/files/backgrounds/'
          '0123456789abcdef0123456789abcdef.jpg',
    ),
    opacity: 1,
    radius: 24,
    padding: 16,
  );

  test(
    'v0.3.0 plaintext moves into the encrypted store and is removed',
    () async {
      SharedPreferences.setMockInitialValues({
        'content': jsonEncode(
          const WidgetContent(
            note: 'Buy milk',
            countdownTitle: 'Lisbon',
          ).toJson(),
        ),
        'draft.clock.minimal': jsonEncode(style.toJson()),
        'draft.photo.full': jsonEncode(photoStyle.toJson()),
        'updateAutoCheck': false,
        'introSeen': true,
        'schema': WidgetStyle.version,
      });
      final prefs = await SharedPreferences.getInstance();
      final docs = MemoryDocuments();

      await migratePlaintext(prefs, await TesseraStore.open(prefs, docs));

      // A fresh store sees only what reached the documents.
      final store = await TesseraStore.open(prefs, docs);
      expect(store.content().note, 'Buy milk');
      expect(store.content().countdownTitle, 'Lisbon');
      expect(store.draft('clock.minimal'), style);
      expect(
        store.draft('photo.full')!.background.photo,
        '0123456789abcdef0123456789abcdef',
      );
      expect(store.updateAutoCheck, isFalse);
      expect(prefs.getKeys(), {'introSeen', 'schema'});
      expect(store.introSeen, isTrue);
    },
  );

  test('a second start finds nothing left to move', () async {
    SharedPreferences.setMockInitialValues({'introSeen': true});
    final prefs = await SharedPreferences.getInstance();
    final docs = MemoryDocuments();
    final store = await TesseraStore.open(prefs, docs);
    await store.saveContent(const WidgetContent(note: 'Kept'));

    await migratePlaintext(prefs, store);

    expect((await TesseraStore.open(prefs, docs)).content().note, 'Kept');
  });
}
