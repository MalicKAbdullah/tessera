import 'dart:ui';

import 'package:shared_preferences/shared_preferences.dart';
import 'package:workmanager/workmanager.dart';

import '../data/tessera_store.dart';
import '../models/widget_kind.dart';
import 'weather_service.dart';
import 'widget_sync.dart';

const refreshTask = 'tessera.refresh';

/// Clock and Calendar tick natively via TextClock and Battery/Countdown are
/// computed by the providers on every redraw, so the periodic job only has to
/// fetch weather and nudge each provider to redraw.
@pragma('vm:entry-point')
void backgroundDispatcher() {
  Workmanager().executeTask((task, _) async {
    DartPluginRegistrant.ensureInitialized();
    final prefs = await SharedPreferences.getInstance();
    await prefs.reload();
    final store = TesseraStore(prefs);
    const sync = WidgetSync();
    final city = store.content().city;
    if (city != null) {
      try {
        final snapshot = await WeatherService().current(city);
        await store.saveWeather(snapshot);
        await sync.pushWeather(snapshot);
      } on Exception {
        // Offline: keep the last reading; the next run retries.
      }
    }
    for (final kind in WidgetKind.values) {
      await sync.refresh(kind);
    }
    return true;
  });
}

Future<void> scheduleBackgroundRefresh() async {
  await Workmanager().initialize(backgroundDispatcher);
  await Workmanager().registerPeriodicTask(
    refreshTask,
    refreshTask,
    frequency: const Duration(minutes: 30),
    existingWorkPolicy: ExistingPeriodicWorkPolicy.keep,
    constraints: Constraints(networkType: NetworkType.notRequired),
  );
}
