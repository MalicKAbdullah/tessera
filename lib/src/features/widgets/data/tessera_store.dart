import 'dart:convert';

import 'package:shared_preferences/shared_preferences.dart';

import '../models/widget_content.dart';
import '../models/widget_kind.dart';
import '../models/widget_style.dart';
import '../services/weather_service.dart';

/// App-side persistence. The native widgets never read this store; they read
/// the copies [WidgetSync] pushes into home_widget's preferences.
class TesseraStore {
  TesseraStore(this._prefs);

  final SharedPreferences _prefs;

  static const _contentKey = 'content';
  static const _weatherKey = 'weather';
  static String _styleKey(WidgetKind kind) => 'style.${kind.id}';

  WidgetStyle style(WidgetKind kind) =>
      switch (_prefs.getString(_styleKey(kind))) {
        final String raw => WidgetStyle.fromJson(
          jsonDecode(raw) as Map<String, dynamic>,
        ),
        _ => WidgetStyle.fallback,
      };

  Future<void> saveStyle(WidgetKind kind, WidgetStyle style) =>
      _prefs.setString(_styleKey(kind), jsonEncode(style.toJson()));

  WidgetContent content() => switch (_prefs.getString(_contentKey)) {
    final String raw => WidgetContent.fromJson(
      jsonDecode(raw) as Map<String, dynamic>,
    ),
    _ => const WidgetContent(),
  };

  Future<void> saveContent(WidgetContent content) =>
      _prefs.setString(_contentKey, jsonEncode(content.toJson()));

  WeatherSnapshot? weather() => switch (_prefs.getString(_weatherKey)) {
    final String raw => WeatherSnapshot.fromJson(
      jsonDecode(raw) as Map<String, dynamic>,
    ),
    _ => null,
  };

  Future<void> saveWeather(WeatherSnapshot snapshot) =>
      _prefs.setString(_weatherKey, jsonEncode(snapshot.toJson()));
}
