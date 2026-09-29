import 'dart:async';

import 'package:battery_plus/battery_plus.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../data/tessera_store.dart';
import '../models/widget_content.dart';
import '../models/widget_kind.dart';
import '../models/widget_style.dart';
import '../services/weather_service.dart';
import '../services/widget_sync.dart';

/// Overridden in main() with the instance loaded before runApp.
final sharedPreferencesProvider = Provider<SharedPreferences>(
    (ref) => throw StateError('sharedPreferencesProvider not overridden'));

final storeProvider =
    Provider((ref) => TesseraStore(ref.watch(sharedPreferencesProvider)));
final widgetSyncProvider = Provider((ref) => const WidgetSync());
final weatherServiceProvider = Provider((ref) => WeatherService());

final styleProvider =
    NotifierProvider.family<StyleNotifier, WidgetStyle, WidgetKind>(
        StyleNotifier.new);

class StyleNotifier extends FamilyNotifier<WidgetStyle, WidgetKind> {
  Timer? _push;

  @override
  WidgetStyle build(WidgetKind kind) {
    ref.onDispose(() => _push?.cancel());
    return ref.read(storeProvider).style(kind);
  }

  void update(WidgetStyle Function(WidgetStyle) change) {
    state = change(state);
    ref.read(storeProvider).saveStyle(arg, state);
    // Sliders emit many values per second; the native widget only needs the
    // one the user settles on.
    _push?.cancel();
    _push = Timer(const Duration(milliseconds: 350),
        () => ref.read(widgetSyncProvider).pushStyle(arg, state));
  }
}

final contentProvider =
    NotifierProvider<ContentNotifier, WidgetContent>(ContentNotifier.new);

class ContentNotifier extends Notifier<WidgetContent> {
  Timer? _push;

  @override
  WidgetContent build() {
    ref.onDispose(() => _push?.cancel());
    return ref.read(storeProvider).content();
  }

  void update(WidgetContent Function(WidgetContent) change) {
    state = change(state);
    ref.read(storeProvider).saveContent(state);
    _push?.cancel();
    _push = Timer(const Duration(milliseconds: 350),
        () => ref.read(widgetSyncProvider).pushContent(state));
  }

  Future<void> setCity(City city) async {
    update((c) => c.copyWith(city: city));
    await ref.read(weatherProvider.notifier).refresh();
  }
}

final weatherProvider =
    AsyncNotifierProvider<WeatherNotifier, WeatherSnapshot?>(
        WeatherNotifier.new);

class WeatherNotifier extends AsyncNotifier<WeatherSnapshot?> {
  static const _staleAfter = Duration(minutes: 30);

  @override
  Future<WeatherSnapshot?> build() async {
    final cached = ref.read(storeProvider).weather();
    final fresh = cached != null &&
        DateTime.now().difference(cached.fetchedAt) < _staleAfter;
    if (fresh || ref.read(contentProvider).city == null) return cached;
    return _fetch();
  }

  Future<void> refresh() async {
    state = const AsyncLoading<WeatherSnapshot?>().copyWithPrevious(state);
    state = await AsyncValue.guard(_fetch);
  }

  Future<WeatherSnapshot?> _fetch() async {
    final city = ref.read(contentProvider).city;
    if (city == null) return null;
    final snapshot = await ref.read(weatherServiceProvider).current(city);
    await ref.read(storeProvider).saveWeather(snapshot);
    await ref.read(widgetSyncProvider).pushWeather(snapshot);
    return snapshot;
  }
}

class BatteryReading {
  const BatteryReading(this.level, this.charging);
  final int level;
  final bool charging;
}

final batteryProvider = StreamProvider<BatteryReading>((ref) async* {
  final battery = Battery();
  Future<BatteryReading> read() async {
    final state = await battery.batteryState;
    return BatteryReading(await battery.batteryLevel,
        state == BatteryState.charging || state == BatteryState.full);
  }

  yield await read();
  await for (final _ in battery.onBatteryStateChanged) {
    yield await read();
  }
});

/// Ticks on each minute boundary so previews change exactly when the
/// home-screen TextClock does.
final nowProvider = StreamProvider<DateTime>((ref) async* {
  while (true) {
    final now = DateTime.now();
    yield now;
    await Future<void>.delayed(
        Duration(seconds: 60 - now.second, milliseconds: -now.millisecond));
  }
});
