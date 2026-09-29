import 'dart:convert';

import 'package:flutter/painting.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tessera/src/features/widgets/models/widget_content.dart';
import 'package:tessera/src/features/widgets/models/widget_style.dart';

void main() {
  group('WidgetStyle', () {
    test('round-trips through JSON', () {
      const style = WidgetStyle(
        background: Color(0xFF16213A),
        opacity: 0.4,
        radius: 12,
        text: Color(0xFFE8ECF4),
        accent: Color(0x808FA8D8),
        scale: 1.2,
        weight: TextWeight.medium,
      );
      final decoded = WidgetStyle.fromJson(
        jsonDecode(jsonEncode(style.toJson())) as Map<String, dynamic>,
      );
      expect(decoded, style);
    });

    test('encodes colours as unsigned ARGB for the Kotlin reader', () {
      final json = WidgetStyle.fallback.toJson();
      expect(json['background'], 0xFF1C1D20);
      expect(json['weight'], 'light');
    });

    test('clamps out-of-range values', () {
      final json = WidgetStyle.fallback.toJson()
        ..['opacity'] = 3
        ..['radius'] = -4
        ..['scale'] = 9;
      final style = WidgetStyle.fromJson(json);
      expect(style.opacity, 1);
      expect(style.radius, WidgetStyle.minRadius);
      expect(style.scale, WidgetStyle.maxScale);
    });
  });

  group('WidgetContent', () {
    test('round-trips with city and date', () {
      final content = WidgetContent(
        note: 'Hi',
        noteAuthor: '',
        countdownTitle: 'Trip',
        countdownDate: DateTime(2027, 3, 14),
        city: const City(
          name: 'Lahore',
          region: 'Punjab, Pakistan',
          latitude: 31.5,
          longitude: 74.3,
        ),
      );
      final decoded = WidgetContent.fromJson(
        jsonDecode(jsonEncode(content.toJson())) as Map<String, dynamic>,
      );
      expect(decoded.countdownDate, DateTime(2027, 3, 14));
      expect(decoded.city!.name, 'Lahore');
      expect(decoded.city!.longitude, 74.3);
    });

    test('daysUntil counts calendar days, not 24h spans', () {
      expect(
        daysUntil(DateTime(2026, 10, 1), DateTime(2026, 9, 30, 23, 59)),
        1,
      );
      expect(daysUntil(DateTime(2026, 9, 30), DateTime(2026, 9, 30, 8)), 0);
      expect(daysUntil(DateTime(2026, 9, 1), DateTime(2026, 9, 30)), -29);
    });
  });
}
