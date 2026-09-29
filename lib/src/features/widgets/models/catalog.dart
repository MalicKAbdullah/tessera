import 'widget_style.dart';

/// The design catalog, served by the native registry (engine/Registry.kt) so
/// a design added in Kotlin appears here with no Dart change.
class Catalog {
  const Catalog({
    required this.fonts,
    required this.categories,
    required this.designs,
  });

  final List<FontInfo> fonts;
  final List<CategoryInfo> categories;
  final List<DesignInfo> designs;

  DesignInfo design(String id) => designs.firstWhere((d) => d.id == id);
  FontInfo font(String key) => fonts.firstWhere((f) => f.key == key);
  List<DesignInfo> inCategory(String category) =>
      designs.where((d) => d.category == category).toList();

  factory Catalog.fromJson(Map<String, dynamic> json) => Catalog(
    fonts: [
      for (final f in json['fonts'] as List<dynamic>)
        FontInfo.fromJson(f as Map<String, dynamic>),
    ],
    categories: [
      for (final c in json['categories'] as List<dynamic>)
        CategoryInfo(
          (c as Map<String, dynamic>)['id'] as String,
          c['label'] as String,
        ),
    ],
    designs: [
      for (final d in json['designs'] as List<dynamic>)
        DesignInfo.fromJson(d as Map<String, dynamic>),
    ],
  );
}

class CategoryInfo {
  const CategoryInfo(this.id, this.label);
  final String id;
  final String label;
}

class FontInfo {
  const FontInfo(this.key, this.label, this.weights);
  final String key;
  final String label;
  final List<int> weights;

  factory FontInfo.fromJson(Map<String, dynamic> json) => FontInfo(
    json['key'] as String,
    json['label'] as String,
    (json['weights'] as List<dynamic>).cast<int>(),
  );
}

class SizeInfo {
  const SizeInfo(this.id, this.cols, this.rows, this.widthDp, this.heightDp);
  final String id;
  final int cols;
  final int rows;
  final double widthDp;
  final double heightDp;

  String get label => '$cols×$rows';

  factory SizeInfo.fromJson(Map<String, dynamic> json) => SizeInfo(
    json['id'] as String,
    json['cols'] as int,
    json['rows'] as int,
    (json['widthDp'] as num).toDouble(),
    (json['heightDp'] as num).toDouble(),
  );
}

sealed class ToggleInfo {
  const ToggleInfo(this.key, this.label);
  final String key;
  final String label;

  factory ToggleInfo.fromJson(Map<String, dynamic> json) => switch (json['type']
      as String) {
    'switch' => SwitchToggle(
      json['key'] as String,
      json['label'] as String,
      json['default'] as bool,
    ),
    'choice' => ChoiceToggle(json['key'] as String, json['label'] as String, {
      for (final o in json['options'] as List<dynamic>)
        (o as Map<String, dynamic>)['value'] as String: o['label'] as String,
    }, json['default'] as String),
    final t => throw FormatException('Unknown toggle type $t'),
  };
}

class SwitchToggle extends ToggleInfo {
  const SwitchToggle(super.key, super.label, this.defaultValue);
  final bool defaultValue;
}

class ChoiceToggle extends ToggleInfo {
  const ChoiceToggle(super.key, super.label, this.options, this.defaultValue);

  /// value -> label, in display order.
  final Map<String, String> options;
  final String defaultValue;
}

class DesignInfo {
  const DesignInfo({
    required this.id,
    required this.category,
    required this.name,
    required this.blurb,
    required this.motion,
    required this.sizes,
    required this.defaults,
    required this.toggles,
  });

  final String id;
  final String category;
  final String name;
  final String blurb;
  final String? motion;
  final List<SizeInfo> sizes;
  final WidgetStyle defaults;
  final List<ToggleInfo> toggles;

  SizeInfo size(String id) => sizes.firstWhere((s) => s.id == id);

  factory DesignInfo.fromJson(Map<String, dynamic> json) => DesignInfo(
    id: json['id'] as String,
    category: json['category'] as String,
    name: json['name'] as String,
    blurb: json['blurb'] as String,
    motion: json['motion'] as String?,
    sizes: [
      for (final s in json['sizes'] as List<dynamic>)
        SizeInfo.fromJson(s as Map<String, dynamic>),
    ],
    defaults: WidgetStyle.fromJson(json['defaults'] as Map<String, dynamic>),
    toggles: [
      for (final t in json['toggles'] as List<dynamic>)
        ToggleInfo.fromJson(t as Map<String, dynamic>),
    ],
  );
}

/// A widget on the home screen and what it is bound to.
class PlacedWidget {
  const PlacedWidget({
    required this.id,
    required this.category,
    required this.size,
    required this.bound,
    required this.design,
    required this.style,
  });

  final int id;
  final String category;
  final String size;

  /// False until the pin callback, configure screen or migration binds it.
  final bool bound;
  final String design;
  final WidgetStyle style;

  factory PlacedWidget.fromJson(Map<String, dynamic> json) => PlacedWidget(
    id: json['id'] as int,
    category: json['category'] as String,
    size: json['size'] as String,
    bound: json['bound'] as bool,
    design: json['design'] as String,
    style: WidgetStyle.fromJson(json['style'] as Map<String, dynamic>),
  );
}
