import 'dart:convert';

import 'package:shared_preferences/shared_preferences.dart';

import '../models/widget_content.dart';
import '../models/widget_style.dart';
import 'vault.dart';

/// App-side persistence: the style being drafted for each design, the
/// user's content and settings. Placed widgets keep their own binding
/// natively.
///
/// Everything the user typed or chose is in encrypted [Documents], held in
/// memory so the editor reads it synchronously. Shared preferences keep only
/// the app's own progress flags.
class TesseraStore {
  TesseraStore._(this._prefs, this._docs);

  /// Opens the store with every document loaded.
  static Future<TesseraStore> open(
    SharedPreferences prefs,
    Documents docs,
  ) async {
    final store = TesseraStore._(prefs, docs);
    await store.reload();
    return store;
  }

  final SharedPreferences _prefs;
  final Documents _docs;

  static const _contentDoc = 'content';
  static const _draftsDoc = 'drafts';
  static const _settingsDoc = 'settings';
  static const _updateAutoCheck = 'updateAutoCheck';
  static const _schemaKey = 'schema';
  static const _introKey = 'introSeen';

  /// v0.1 kept one style per widget kind under `style.<kind>`.
  static const legacyKinds = [
    'clock',
    'calendar',
    'battery',
    'weather',
    'countdown',
    'note',
  ];

  WidgetContent _content = const WidgetContent();
  Map<String, WidgetStyle> _drafts = {};
  Map<String, Object?> _settings = {};

  /// Re-reads every document. The launcher's configure screen runs a second
  /// Flutter engine over the same files with its own in-memory copy.
  Future<void> reload() async {
    final [content, drafts, settings] = await Future.wait([
      _docs.read(_contentDoc),
      _docs.read(_draftsDoc),
      _docs.read(_settingsDoc),
    ]);
    // A document that was never written reads as null.
    _content = content == null
        ? const WidgetContent()
        : WidgetContent.fromJson(content as Map<String, dynamic>);
    _drafts = {
      if (drafts != null)
        for (final MapEntry(:key, :value)
            in (drafts as Map<String, dynamic>).entries)
          key: WidgetStyle.fromJson(value as Map<String, dynamic>),
    };
    _settings = {...?settings as Map<String, dynamic>?};
  }

  WidgetStyle? draft(String design) => _drafts[design];

  Future<void> saveDraft(String design, WidgetStyle style) {
    _drafts[design] = style;
    return _docs.write(_draftsDoc, {
      for (final MapEntry(:key, :value) in _drafts.entries) key: value.toJson(),
    });
  }

  WidgetContent content() => _content;

  Future<void> saveContent(WidgetContent content) {
    _content = content;
    return _docs.write(_contentDoc, content.toJson());
  }

  /// Whether the gallery asks GitHub for a newer release on open.
  bool get updateAutoCheck => _settings[_updateAutoCheck] as bool? ?? true;

  Future<void> setUpdateAutoCheck(bool on) {
    _settings[_updateAutoCheck] = on;
    return _docs.write(_settingsDoc, _settings);
  }

  /// The first-launch intro plays until it has been watched or skipped once.
  bool get introSeen => _prefs.getBool(_introKey) ?? false;

  Future<void> markIntroSeen() => _prefs.setBool(_introKey, true);

  bool get migrated => _prefs.getInt(_schemaKey) == WidgetStyle.version;

  Future<void> markMigrated() => _prefs.setInt(_schemaKey, WidgetStyle.version);

  WidgetStyle? legacyStyle(String kind) =>
      switch (_prefs.getString('style.$kind')) {
        final String raw => WidgetStyle.fromV1(
          jsonDecode(raw) as Map<String, dynamic>,
        ),
        _ => null,
      };

  Future<void> removeLegacyStyle(String kind) => _prefs.remove('style.$kind');
}
