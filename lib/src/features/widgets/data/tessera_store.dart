import 'dart:convert';

import 'package:shared_preferences/shared_preferences.dart';

import '../models/widget_content.dart';
import '../models/widget_style.dart';

/// App-side persistence: the style being drafted for each design and the
/// user's content. Placed widgets keep their own binding natively.
class TesseraStore {
  TesseraStore(this._prefs);

  final SharedPreferences _prefs;

  static const _contentKey = 'content';
  static const _schemaKey = 'schema';
  static String _draftKey(String design) => 'draft.$design';

  /// v0.1 kept one style per widget kind under `style.<kind>`.
  static const legacyKinds = [
    'clock',
    'calendar',
    'battery',
    'weather',
    'countdown',
    'note',
  ];

  WidgetStyle? draft(String design) =>
      switch (_prefs.getString(_draftKey(design))) {
        final String raw => WidgetStyle.fromJson(
          jsonDecode(raw) as Map<String, dynamic>,
        ),
        _ => null,
      };

  Future<void> saveDraft(String design, WidgetStyle style) =>
      _prefs.setString(_draftKey(design), jsonEncode(style.toJson()));

  WidgetContent content() => switch (_prefs.getString(_contentKey)) {
    final String raw => WidgetContent.fromJson(
      jsonDecode(raw) as Map<String, dynamic>,
    ),
    _ => const WidgetContent(),
  };

  Future<void> saveContent(WidgetContent content) =>
      _prefs.setString(_contentKey, jsonEncode(content.toJson()));

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
