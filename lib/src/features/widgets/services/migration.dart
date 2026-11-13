import 'dart:convert';

import 'package:shared_preferences/shared_preferences.dart';

import '../data/tessera_store.dart';
import '../models/catalog.dart';
import '../models/widget_content.dart';
import '../models/widget_style.dart';
import 'engine.dart';

/// The design each v0.1 widget kind most resembles; v0.1 widgets bind to it.
const legacyDesigns = {
  'clock': 'clock.minimal',
  'calendar': 'calendar.classic',
  'battery': 'battery.segments',
  'weather': 'weather.classic',
  'countdown': 'countdown.classic',
  'note': 'note.classic',
};

/// One-time move from v0.1: each kind's style becomes the draft of its
/// successor design, and widgets placed under v0.1 (still unbound) bind to
/// that design and style so they keep their look.
Future<void> migrateFromV1(
  TesseraStore store,
  Engine engine,
  Catalog catalog,
) async {
  if (store.migrated) return;
  final placed = await engine.placed();
  for (final kind in TesseraStore.legacyKinds) {
    final design = catalog.design(legacyDesigns[kind]!);
    final style = store.legacyStyle(kind);
    if (style != null) await store.saveDraft(design.id, style);
    for (final widget in placed.where((p) => p.category == kind && !p.bound)) {
      if (design.sizes.any((s) => s.id == widget.size)) {
        await engine.bind(widget.id, design.id, style ?? design.defaults);
      }
    }
    await store.removeLegacyStyle(kind);
  }
  await store.markMigrated();
}

/// Up to v0.3.0 the user's content, style drafts and the update setting were
/// plain shared preferences. Each is written to the encrypted [store] and
/// only then removed, so an interrupted move repeats on the next start.
Future<void> migratePlaintext(
  SharedPreferences prefs,
  TesseraStore store,
) async {
  final content = prefs.getString('content');
  if (content != null) {
    await store.saveContent(
      WidgetContent.fromJson(jsonDecode(content) as Map<String, dynamic>),
    );
    await prefs.remove('content');
  }
  for (final key in prefs.getKeys().where((k) => k.startsWith('draft.'))) {
    final style = WidgetStyle.fromJson(
      jsonDecode(prefs.getString(key)!) as Map<String, dynamic>,
    );
    await store.saveDraft(
      key.substring('draft.'.length),
      withBackgroundId(style),
    );
    await prefs.remove(key);
  }
  final updateAutoCheck = prefs.getBool('updateAutoCheck');
  if (updateAutoCheck != null) {
    await store.setUpdateAutoCheck(updateAutoCheck);
    await prefs.remove('updateAutoCheck');
  }
}

/// Styles referred to a photo surface by its path in `files/backgrounds`;
/// they now refer to it by the content hash that named the file, as the
/// engine's own migration does (android/.../security/LegacyPlaintext.kt).
WidgetStyle withBackgroundId(WidgetStyle style) {
  final path = style.background.photo;
  if (path == null) return style;
  final name = path.split('/').last;
  final id = name.substring(0, name.lastIndexOf('.'));
  return style.copyWith(background: style.background.copyWith(photo: id));
}
