import 'dart:convert';

import 'package:flutter/services.dart';

import '../models/catalog.dart';
import '../models/photo_album.dart';
import '../models/widget_content.dart';
import '../models/widget_style.dart';

/// Where the app was opened from.
sealed class LaunchTarget {
  const LaunchTarget(this.widgetId);
  final int widgetId;

  static LaunchTarget? fromMap(Map<Object?, Object?>? map) =>
      switch (map?['mode']) {
        'edit' => LaunchEdit(map!['id']! as int),
        'configure' => LaunchConfigure(map!['id']! as int),
        _ => null,
      };
}

/// A placed widget was tapped.
class LaunchEdit extends LaunchTarget {
  const LaunchEdit(super.widgetId);
}

/// The launcher is placing or reconfiguring a widget.
class LaunchConfigure extends LaunchTarget {
  const LaunchConfigure(super.widgetId);
}

/// The single bridge to the Kotlin widget engine (android/.../EngineChannel.kt).
class Engine {
  Engine({this.onDataChanged, this.onLaunchTarget}) {
    _channel.setMethodCallHandler((call) async {
      switch (call.method) {
        case 'dataChanged':
          onDataChanged?.call();
        case 'launchTarget':
          final target = LaunchTarget.fromMap(
            call.arguments as Map<Object?, Object?>?,
          );
          if (target != null) onLaunchTarget?.call(target);
      }
    });
  }

  static const _channel = MethodChannel('tessera/engine');

  void Function()? onDataChanged;
  void Function(LaunchTarget)? onLaunchTarget;

  Future<Catalog> catalog() async => Catalog.fromJson(
    jsonDecode((await _channel.invokeMethod<String>('catalog'))!)
        as Map<String, dynamic>,
  );

  /// The design drawn by the same RemoteViews the launcher receives.
  Future<Uint8List> render(
    String design,
    WidgetStyle style,
    SizeInfo size,
  ) async => (await _channel.invokeMethod<Uint8List>('render', {
    'design': design,
    'style': jsonEncode(style.toJson()),
    'widthDp': size.widthDp,
    'heightDp': size.heightDp,
  }))!;

  Future<Uint8List> specimen(
    String font,
    int weight,
    String text,
    double size,
    Color color,
  ) async => (await _channel.invokeMethod<Uint8List>('specimen', {
    'font': font,
    'weight': weight,
    'text': text,
    'size': size,
    'color': color.toARGB32(),
  }))!;

  Future<List<PlacedWidget>> placed() async => [
    for (final p
        in jsonDecode((await _channel.invokeMethod<String>('placed'))!)
            as List<dynamic>)
      PlacedWidget.fromJson(p as Map<String, dynamic>),
  ];

  Future<void> bind(int id, String design, WidgetStyle style) =>
      _channel.invokeMethod('bind', {
        'id': id,
        'design': design,
        'style': jsonEncode(style.toJson()),
      });

  Future<bool> canPin() async => (await _channel.invokeMethod<bool>('canPin'))!;

  Future<bool> pin(String design, WidgetStyle style, SizeInfo size) async =>
      (await _channel.invokeMethod<bool>('pin', {
        'design': design,
        'style': jsonEncode(style.toJson()),
        'size': size.id,
      }))!;

  Future<void> finishConfigure(String design, WidgetStyle style) =>
      _channel.invokeMethod('finishConfigure', {
        'design': design,
        'style': jsonEncode(style.toJson()),
      });

  Future<void> setContent(WidgetContent content) => _channel.invokeMethod(
    'setContent',
    {'json': jsonEncode(content.toJson())},
  );

  Future<PhotoAlbum> photos() => _album(_channel.invokeMethod('photos'));

  /// Opens the system photo picker for the album's free places and imports
  /// the picks; a cancelled pick returns the album unchanged.
  Future<PhotoAlbum> pickPhotos() =>
      _album(_channel.invokeMethod('pickPhotos'));

  Future<PhotoAlbum> removePhoto(String id) =>
      _album(_channel.invokeMethod('removePhoto', {'id': id}));

  Future<PhotoAlbum> setPhotoCaption(String caption) =>
      _album(_channel.invokeMethod('setPhotoCaption', {'caption': caption}));

  static Future<PhotoAlbum> _album(Future<String?> json) async =>
      PhotoAlbum.fromJson(jsonDecode((await json)!) as Map<String, dynamic>);

  Future<void> refreshWeather() => _channel.invokeMethod('refreshWeather');

  Future<LaunchTarget?> launchTarget() async => LaunchTarget.fromMap(
    await _channel.invokeMethod<Map<Object?, Object?>>('launchTarget'),
  );
}
