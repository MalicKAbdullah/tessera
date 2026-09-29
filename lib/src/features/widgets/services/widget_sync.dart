import 'dart:convert';

import 'package:home_widget/home_widget.dart';

import '../data/tessera_store.dart';
import '../models/widget_content.dart';
import '../models/widget_kind.dart';
import '../models/widget_style.dart';
import '../services/weather_service.dart';

/// Pushes styles and content into home_widget's preferences and asks the
/// native providers to redraw. Every value is written as a String so the
/// Kotlin side has exactly one decoding path.
class WidgetSync {
  const WidgetSync();

  Future<void> pushStyle(WidgetKind kind, WidgetStyle style) async {
    await HomeWidget.saveWidgetData<String>(
      'style_${kind.id}',
      jsonEncode(style.toJson()),
    );
    await refresh(kind);
  }

  Future<void> pushContent(WidgetContent content) async {
    await HomeWidget.saveWidgetData<String>('note_text', content.note);
    await HomeWidget.saveWidgetData<String>('note_author', content.noteAuthor);
    await HomeWidget.saveWidgetData<String>(
      'countdown_title',
      content.countdownTitle,
    );
    final target = content.effectiveCountdownDate(DateTime.now());
    await HomeWidget.saveWidgetData<String>(
      'countdown_target',
      '${target.year}-${target.month}-${target.day}',
    );
    await HomeWidget.saveWidgetData<String>(
      'weather_city',
      content.city?.name ?? '',
    );
    await refresh(WidgetKind.note);
    await refresh(WidgetKind.countdown);
    await refresh(WidgetKind.weather);
  }

  Future<void> pushWeather(WeatherSnapshot snapshot) async {
    await HomeWidget.saveWidgetData<String>(
      'weather_temp',
      snapshot.temperatureLabel,
    );
    await HomeWidget.saveWidgetData<String>(
      'weather_condition',
      snapshot.condition,
    );
    await refresh(WidgetKind.weather);
  }

  Future<void> pushAll(TesseraStore store) async {
    for (final kind in WidgetKind.values) {
      await HomeWidget.saveWidgetData<String>(
        'style_${kind.id}',
        jsonEncode(store.style(kind).toJson()),
      );
    }
    await pushContent(store.content());
    final weather = store.weather();
    if (weather != null) await pushWeather(weather);
    for (final kind in WidgetKind.values) {
      await refresh(kind);
    }
  }

  Future<void> refresh(WidgetKind kind) =>
      HomeWidget.updateWidget(qualifiedAndroidName: kind.qualifiedAndroidName);

  Future<bool> canPin() async =>
      await HomeWidget.isRequestPinWidgetSupported() ?? false;

  Future<void> pin(WidgetKind kind) => HomeWidget.requestPinWidget(
    qualifiedAndroidName: kind.qualifiedAndroidName,
  );
}
