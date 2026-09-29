import '../data/tessera_store.dart';
import '../models/catalog.dart';
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
