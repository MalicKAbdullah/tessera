import 'dart:async';
import 'dart:typed_data';

import 'package:flutter/painting.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../data/tessera_store.dart';
import '../models/catalog.dart';
import '../models/widget_content.dart';
import '../models/widget_style.dart';
import '../services/engine.dart';
import '../services/weather_service.dart';

/// Overridden in main() with the instance loaded before runApp.
final sharedPreferencesProvider = Provider<SharedPreferences>(
  (ref) => throw StateError('sharedPreferencesProvider not overridden'),
);

/// Overridden in main() so the channel handlers exist before the first frame.
final engineProvider = Provider<Engine>(
  (ref) => throw StateError('engineProvider not overridden'),
);

/// Overridden in main() with the catalog read before runApp.
final catalogProvider = Provider<Catalog>(
  (ref) => throw StateError('catalogProvider not overridden'),
);

final storeProvider = Provider(
  (ref) => TesseraStore(ref.watch(sharedPreferencesProvider)),
);
final weatherServiceProvider = Provider((ref) => WeatherService());

/// Bumped whenever native live data (battery, weather) changes, so previews
/// redraw with it.
final dataRevisionProvider = StateProvider<int>((ref) => 0);

/// Ticks on each minute boundary so previews show the time the home-screen
/// TextClocks show.
final minuteProvider = StreamProvider<DateTime>((ref) async* {
  while (true) {
    final now = DateTime.now();
    yield now;
    await Future<void>.delayed(
      Duration(seconds: 60 - now.second, milliseconds: -now.millisecond),
    );
  }
});

/// What an editor is editing: a design's draft, or a placed widget.
sealed class EditTarget {
  const EditTarget(this.design);
  final String design;
}

class DraftTarget extends EditTarget {
  const DraftTarget(super.design);
  @override
  bool operator ==(Object other) =>
      other is DraftTarget && other.design == design;
  @override
  int get hashCode => design.hashCode;
}

class PlacedTarget extends EditTarget {
  const PlacedTarget(super.design, this.widgetId, {required this.configuring});
  final int widgetId;

  /// True when the launcher opened Tessera to configure this widget.
  final bool configuring;

  @override
  bool operator ==(Object other) =>
      other is PlacedTarget &&
      other.design == design &&
      other.widgetId == widgetId &&
      other.configuring == configuring;
  @override
  int get hashCode => Object.hash(design, widgetId, configuring);
}

final styleProvider =
    NotifierProvider.family<StyleNotifier, WidgetStyle, EditTarget>(
      StyleNotifier.new,
    );

class StyleNotifier extends FamilyNotifier<WidgetStyle, EditTarget> {
  Timer? _push;

  @override
  WidgetStyle build(EditTarget target) {
    ref.onDispose(() => _push?.cancel());
    final defaults = ref.read(catalogProvider).design(target.design).defaults;
    return switch (target) {
      DraftTarget() => ref.read(storeProvider).draft(target.design) ?? defaults,
      PlacedTarget() => defaults,
    };
  }

  /// Starts a placed widget's editor from its current binding.
  void load(WidgetStyle style) => state = style;

  void update(WidgetStyle Function(WidgetStyle) change) {
    state = change(state);
    switch (arg) {
      case DraftTarget(:final design):
        ref.read(storeProvider).saveDraft(design, state);
      case PlacedTarget(:final widgetId, :final design, configuring: false):
        // Sliders emit many values per second; the widget only needs the
        // one the user settles on.
        _push?.cancel();
        _push = Timer(
          const Duration(milliseconds: 350),
          () => ref.read(engineProvider).bind(widgetId, design, state),
        );
      case PlacedTarget(configuring: true):
        break;
    }
  }

  void reset() =>
      update((_) => ref.read(catalogProvider).design(arg.design).defaults);
}

final contentProvider = NotifierProvider<ContentNotifier, WidgetContent>(
  ContentNotifier.new,
);

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
    _push = Timer(
      const Duration(milliseconds: 350),
      () => ref.read(engineProvider).setContent(state),
    );
  }
}

final placedProvider = FutureProvider<List<PlacedWidget>>((ref) {
  ref.watch(dataRevisionProvider);
  return ref.read(engineProvider).placed();
});

class PreviewRequest {
  const PreviewRequest(this.design, this.style, this.size);
  final String design;
  final WidgetStyle style;
  final SizeInfo size;

  @override
  bool operator ==(Object other) =>
      other is PreviewRequest &&
      other.design == design &&
      other.style == style &&
      other.size.id == size.id;

  @override
  int get hashCode => Object.hash(design, style, size.id);
}

/// A PNG of the exact RemoteViews the launcher would draw.
final previewProvider = FutureProvider.autoDispose
    .family<Uint8List, PreviewRequest>((ref, request) {
      ref.watch(minuteProvider);
      ref.watch(dataRevisionProvider);
      return ref
          .read(engineProvider)
          .render(request.design, request.style, request.size);
    });

class SpecimenRequest {
  const SpecimenRequest(this.font, this.weight, this.argb);
  final String font;
  final int weight;
  final int argb;

  @override
  bool operator ==(Object other) =>
      other is SpecimenRequest &&
      other.font == font &&
      other.weight == weight &&
      other.argb == argb;

  @override
  int get hashCode => Object.hash(font, weight, argb);
}

final specimenProvider = FutureProvider.family<Uint8List, SpecimenRequest>(
  (ref, r) => ref
      .read(engineProvider)
      .specimen(r.font, r.weight, 'Aa 12:45', 22, Color(r.argb)),
);
