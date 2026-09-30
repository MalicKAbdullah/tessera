import 'package:flutter/painting.dart';

/// How the widget surface is painted natively (engine/Renderer.kt).
enum BgKind {
  solid('Solid'),
  gradient('Gradient'),
  dots('Dot grid'),
  grain('Grain'),
  transparent('Clear'),
  photo('Photo');

  const BgKind(this.label);
  final String label;
}

class Background {
  const Background({
    required this.kind,
    required this.color,
    required this.color2,
    this.angle = 135,
    this.photo,
  });

  final BgKind kind;
  final Color color;
  final Color color2;
  final double angle;

  /// Absolute path of an image in app storage; set when [kind] is photo.
  final String? photo;

  Background copyWith({BgKind? kind, Color? color, Color? color2}) =>
      Background(
        kind: kind ?? this.kind,
        color: color ?? this.color,
        color2: color2 ?? this.color2,
        angle: angle,
        photo: photo,
      );

  Map<String, Object?> toJson() => {
    'kind': kind.name,
    'color': color.toARGB32(),
    'color2': color2.toARGB32(),
    'angle': angle,
    'photo': photo,
  };

  factory Background.fromJson(Map<String, dynamic> json) => Background(
    kind: BgKind.values.byName(json['kind'] as String),
    color: Color(json['color'] as int),
    color2: Color(json['color2'] as int),
    angle: (json['angle'] as num).toDouble(),
    photo: json['photo'] as String?,
  );

  @override
  bool operator ==(Object other) =>
      other is Background &&
      other.kind == kind &&
      other.color == color &&
      other.color2 == color2 &&
      other.angle == angle &&
      other.photo == photo;

  @override
  int get hashCode => Object.hash(kind, color, color2, angle, photo);
}

/// Visual style of one design or placed widget. Serialized as JSON and read
/// verbatim by the native engine (engine/Style.kt), so keys and ranges are a
/// cross-language contract. [version] 2; v0.1 styles migrate on read.
class WidgetStyle {
  const WidgetStyle({
    required this.font,
    required this.weight,
    required this.scale,
    required this.tracking,
    required this.text,
    required this.accent,
    required this.background,
    required this.opacity,
    required this.radius,
    required this.padding,
    this.toggles = const {},
  });

  static const version = 2;
  static const double minRadius = 0;
  static const double maxRadius = 48;
  static const double minScale = 0.8;
  static const double maxScale = 1.3;
  static const double minTracking = -0.05;
  static const double maxTracking = 0.2;
  static const double minPadding = 8;
  static const double maxPadding = 28;

  final String font;
  final int weight;
  final double scale;

  /// Letter spacing in em for bitmap-drawn text.
  final double tracking;
  final Color text;
  final Color accent;
  final Background background;
  final double opacity;
  final double radius;
  final double padding;

  /// Per-design options keyed by the catalog's toggle keys (bool or String).
  final Map<String, Object> toggles;

  WidgetStyle copyWith({
    String? font,
    int? weight,
    double? scale,
    double? tracking,
    Color? text,
    Color? accent,
    Background? background,
    double? opacity,
    double? radius,
    double? padding,
    Map<String, Object>? toggles,
  }) => WidgetStyle(
    font: font ?? this.font,
    weight: weight ?? this.weight,
    scale: scale ?? this.scale,
    tracking: tracking ?? this.tracking,
    text: text ?? this.text,
    accent: accent ?? this.accent,
    background: background ?? this.background,
    opacity: opacity ?? this.opacity,
    radius: radius ?? this.radius,
    padding: padding ?? this.padding,
    toggles: toggles ?? this.toggles,
  );

  WidgetStyle withToggle(String key, Object value) =>
      copyWith(toggles: {...toggles, key: value});

  Map<String, Object?> toJson() => {
    'v': version,
    'font': font,
    'weight': weight,
    'scale': scale,
    'tracking': tracking,
    'text': text.toARGB32(),
    'accent': accent.toARGB32(),
    'bg': background.toJson(),
    'opacity': opacity,
    'radius': radius,
    'padding': padding,
    'toggles': toggles,
  };

  /// Reads v2 JSON, or migrates a v0.1 style (which has no `v` key).
  factory WidgetStyle.fromJson(Map<String, dynamic> json) {
    if (json['v'] == null) return WidgetStyle.fromV1(json);
    assert(json['v'] == version, 'Unknown style schema ${json['v']}');
    return WidgetStyle(
      font: json['font'] as String,
      weight: json['weight'] as int,
      scale: (json['scale'] as num).toDouble().clamp(minScale, maxScale),
      tracking: (json['tracking'] as num).toDouble().clamp(
        minTracking,
        maxTracking,
      ),
      text: Color(json['text'] as int),
      accent: Color(json['accent'] as int),
      background: Background.fromJson(json['bg'] as Map<String, dynamic>),
      opacity: (json['opacity'] as num).toDouble().clamp(0, 1),
      radius: (json['radius'] as num).toDouble().clamp(minRadius, maxRadius),
      padding: (json['padding'] as num).toDouble().clamp(
        minPadding,
        maxPadding,
      ),
      toggles: (json['toggles'] as Map<String, dynamic>).cast<String, Object>(),
    );
  }

  /// v0.1 styles: one solid colour, system sans in light/regular/medium.
  /// They become Inter Tight (the bundled face closest to the old system
  /// sans) at the same weight, with the v0.1 tile's 20dp padding.
  factory WidgetStyle.fromV1(Map<String, dynamic> json) {
    final background = Color(json['background'] as int);
    return WidgetStyle(
      font: 'sans',
      weight: switch (json['weight'] as String) {
        'light' => 300,
        'regular' => 400,
        'medium' => 500,
        final w => throw FormatException('Unknown v0.1 weight $w'),
      },
      scale: (json['scale'] as num).toDouble().clamp(minScale, maxScale),
      tracking: 0,
      text: Color(json['text'] as int),
      accent: Color(json['accent'] as int),
      background: Background(
        kind: BgKind.solid,
        color: background,
        color2: background,
      ),
      opacity: (json['opacity'] as num).toDouble().clamp(0, 1),
      radius: (json['radius'] as num).toDouble().clamp(minRadius, maxRadius),
      padding: 20,
    );
  }

  @override
  bool operator ==(Object other) =>
      other is WidgetStyle &&
      other.font == font &&
      other.weight == weight &&
      other.scale == scale &&
      other.tracking == tracking &&
      other.text == text &&
      other.accent == accent &&
      other.background == background &&
      other.opacity == opacity &&
      other.radius == radius &&
      other.padding == padding &&
      _mapEquals(other.toggles, toggles);

  @override
  int get hashCode => Object.hash(
    font,
    weight,
    scale,
    tracking,
    text,
    accent,
    background,
    opacity,
    radius,
    padding,
    Object.hashAllUnordered(toggles.entries.map((e) => '${e.key}=${e.value}')),
  );
}

bool _mapEquals(Map<String, Object> a, Map<String, Object> b) =>
    a.length == b.length && a.entries.every((e) => b[e.key] == e.value);

/// Snaps a requested weight to the nearest face a family ships, matching
/// the native `Fonts.face` rule so the editor never promises a weight the
/// widget cannot draw.
int snapWeight(int requested, List<int> available) => available.reduce(
  (best, w) => (w - requested).abs() < (best - requested).abs() ? w : best,
);

const swatches = <Color>[
  Color(0xFFFFFFFF),
  Color(0xFFF3F1EC),
  Color(0xFFEDE9E1),
  Color(0xFF9A9A9A),
  Color(0xFF1F2023),
  Color(0xFF0C0C0D),
  Color(0xFFC9A77C),
  Color(0xFFFFB020),
  Color(0xFFFF5A1F),
  Color(0xFFC6F432),
  Color(0xFF9AD0C2),
  Color(0xFF8FB3FF),
  Color(0xFF2F6BFF),
  Color(0xFFD7A6C9),
];
