import 'package:flutter/painting.dart';

enum TextWeight {
  light(FontWeight.w300),
  regular(FontWeight.w400),
  medium(FontWeight.w500);

  const TextWeight(this.fontWeight);
  final FontWeight fontWeight;
}

/// Visual style of one widget. Serialized as JSON and read verbatim by the
/// native Kotlin renderer, so keys and value ranges are a cross-language
/// contract.
class WidgetStyle {
  const WidgetStyle({
    required this.background,
    required this.opacity,
    required this.radius,
    required this.text,
    required this.accent,
    required this.scale,
    required this.weight,
  });

  static const WidgetStyle fallback = WidgetStyle(
    background: Color(0xFF1C1D20),
    opacity: 0.92,
    radius: 24,
    text: Color(0xFFF3F1EC),
    accent: Color(0xFFC9A77C),
    scale: 1,
    weight: TextWeight.light,
  );

  static const double minRadius = 0;
  static const double maxRadius = 36;
  static const double minScale = 0.8;
  static const double maxScale = 1.3;

  final Color background;
  final double opacity;
  final double radius;
  final Color text;
  final Color accent;
  final double scale;
  final TextWeight weight;

  Color get surface => background.withValues(alpha: opacity);

  WidgetStyle copyWith({
    Color? background,
    double? opacity,
    double? radius,
    Color? text,
    Color? accent,
    double? scale,
    TextWeight? weight,
  }) =>
      WidgetStyle(
        background: background ?? this.background,
        opacity: opacity ?? this.opacity,
        radius: radius ?? this.radius,
        text: text ?? this.text,
        accent: accent ?? this.accent,
        scale: scale ?? this.scale,
        weight: weight ?? this.weight,
      );

  Map<String, Object> toJson() => {
        'background': background.toARGB32(),
        'opacity': opacity,
        'radius': radius,
        'text': text.toARGB32(),
        'accent': accent.toARGB32(),
        'scale': scale,
        'weight': weight.name,
      };

  factory WidgetStyle.fromJson(Map<String, dynamic> json) => WidgetStyle(
        background: Color(json['background'] as int),
        opacity: (json['opacity'] as num).toDouble().clamp(0, 1),
        radius:
            (json['radius'] as num).toDouble().clamp(minRadius, maxRadius),
        text: Color(json['text'] as int),
        accent: Color(json['accent'] as int),
        scale: (json['scale'] as num).toDouble().clamp(minScale, maxScale),
        weight: TextWeight.values.byName(json['weight'] as String),
      );

  @override
  bool operator ==(Object other) =>
      other is WidgetStyle &&
      other.background == background &&
      other.opacity == opacity &&
      other.radius == radius &&
      other.text == text &&
      other.accent == accent &&
      other.scale == scale &&
      other.weight == weight;

  @override
  int get hashCode =>
      Object.hash(background, opacity, radius, text, accent, scale, weight);
}

/// Curated surfaces, each paired with the text and accent that read well on
/// it, so one tap gives a finished look.
class Preset {
  const Preset(this.name, this.background, this.text, this.accent);
  final String name;
  final Color background;
  final Color text;
  final Color accent;
}

const presets = <Preset>[
  Preset('Ink', Color(0xFF1C1D20), Color(0xFFF3F1EC), Color(0xFFC9A77C)),
  Preset('Paper', Color(0xFFF5F3EE), Color(0xFF1F2023), Color(0xFFB0643F)),
  Preset('Sand', Color(0xFFE6DCCB), Color(0xFF2E2A24), Color(0xFF7A6A52)),
  Preset('Sage', Color(0xFFA9B8A3), Color(0xFF1E261D), Color(0xFF3D4E3A)),
  Preset('Mist', Color(0xFFD3DCE2), Color(0xFF1B2530), Color(0xFF47627A)),
  Preset('Clay', Color(0xFFC4805F), Color(0xFFFFF7F0), Color(0xFF3A2016)),
  Preset('Midnight', Color(0xFF16213A), Color(0xFFE8ECF4), Color(0xFF8FA8D8)),
  Preset('Moss', Color(0xFF34402F), Color(0xFFECEFE6), Color(0xFFC3D08E)),
  Preset('Plum', Color(0xFF3F3143), Color(0xFFF2EAF3), Color(0xFFD7A6C9)),
];

const swatches = <Color>[
  Color(0xFFFFFFFF),
  Color(0xFFF3F1EC),
  Color(0xFF9A9A9A),
  Color(0xFF1F2023),
  Color(0xFF000000),
  Color(0xFFC9A77C),
  Color(0xFFB0643F),
  Color(0xFFC3D08E),
  Color(0xFF8FA8D8),
  Color(0xFFD7A6C9),
];
