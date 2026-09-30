import 'dart:convert';

import 'package:flutter/painting.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tessera/src/features/widgets/models/catalog.dart';
import 'package:tessera/src/features/widgets/models/widget_content.dart';
import 'package:tessera/src/features/widgets/models/widget_style.dart';

const _style = WidgetStyle(
  font: 'dot',
  weight: 700,
  scale: 1.2,
  tracking: 0.05,
  text: Color(0xFFE8ECF4),
  accent: Color(0x808FA8D8),
  background: Background(
    kind: BgKind.gradient,
    color: Color(0xFF16213A),
    color2: Color(0xFF22262B),
  ),
  opacity: 0.4,
  radius: 12,
  padding: 16,
  toggles: {'seconds': true, 'hours': '24'},
);

WidgetStyle _roundTrip(WidgetStyle s) => WidgetStyle.fromJson(
  jsonDecode(jsonEncode(s.toJson())) as Map<String, dynamic>,
);

void main() {
  group('WidgetStyle v2', () {
    test('round-trips through JSON, toggles included', () {
      expect(_roundTrip(_style), _style);
    });

    test('writes the schema version and unsigned ARGB for Kotlin', () {
      final json = _style.toJson();
      expect(json['v'], 2);
      expect((json['bg']! as Map)['color'], 0xFF16213A);
      expect((json['bg']! as Map)['kind'], 'gradient');
      expect(json['accent'], 0x808FA8D8);
    });

    test('clamps out-of-range values', () {
      final json = _style.toJson()
        ..['opacity'] = 3
        ..['radius'] = -4
        ..['scale'] = 9
        ..['tracking'] = 1
        ..['padding'] = 0;
      final style = WidgetStyle.fromJson(json);
      expect(style.opacity, 1);
      expect(style.radius, WidgetStyle.minRadius);
      expect(style.scale, WidgetStyle.maxScale);
      expect(style.tracking, WidgetStyle.maxTracking);
      expect(style.padding, WidgetStyle.minPadding);
    });

    test('toggle order does not affect equality', () {
      final a = _style.copyWith(toggles: {'a': true, 'b': 'x'});
      final b = _style.copyWith(toggles: {'b': 'x', 'a': true});
      expect(a, b);
      expect(a.hashCode, b.hashCode);
    });
  });

  group('v0.1 migration', () {
    final v1 = {
      'background': 0xFF16213A,
      'opacity': 0.8,
      'radius': 18.0,
      'text': 0xFFE8ECF4,
      'accent': 0xFF8FA8D8,
      'scale': 1.1,
      'weight': 'medium',
    };

    test('fromJson migrates a style without a schema version', () {
      final style = WidgetStyle.fromJson(v1);
      expect(style.font, 'sans');
      expect(style.weight, 500);
      expect(style.background.kind, BgKind.solid);
      expect(style.background.color, const Color(0xFF16213A));
      expect(style.opacity, 0.8);
      expect(style.radius, 18);
      expect(style.scale, 1.1);
      expect(style.padding, 20);
      expect(style.toggles, isEmpty);
    });

    test('maps each v0.1 weight to its numeric weight', () {
      for (final (name, weight) in [
        ('light', 300),
        ('regular', 400),
        ('medium', 500),
      ]) {
        expect(WidgetStyle.fromV1({...v1, 'weight': name}).weight, weight);
      }
    });

    test('a migrated style survives a v2 round trip', () {
      final migrated = WidgetStyle.fromV1(v1);
      expect(_roundTrip(migrated), migrated);
    });
  });

  test('snapWeight picks the nearest available face', () {
    expect(snapWeight(650, [300, 400, 500, 700]), 700);
    expect(snapWeight(100, [300, 400]), 300);
    expect(snapWeight(450, [400, 500]), 400);
  });

  test('catalog parses designs, sizes and toggles', () {
    final catalog = Catalog.fromJson({
      'fonts': [
        {
          'key': 'dot',
          'label': 'Doto',
          'weights': [300, 700],
        },
      ],
      'categories': [
        {'id': 'clock', 'label': 'Clock'},
      ],
      'designs': [
        {
          'id': 'clock.matrix',
          'category': 'clock',
          'name': 'Dot Matrix',
          'blurb': 'b',
          'motion': null,
          'sizes': [
            {
              'id': 'small',
              'cols': 2,
              'rows': 2,
              'widthDp': 170,
              'heightDp': 170,
            },
          ],
          'defaults': _style.toJson(),
          'toggles': [
            {
              'key': 'pulse',
              'label': 'Pulse',
              'type': 'switch',
              'default': true,
            },
            {
              'key': 'hours',
              'label': 'Hours',
              'type': 'choice',
              'default': 'system',
              'options': [
                {'value': 'system', 'label': 'System'},
                {'value': '24', 'label': '24-hour'},
              ],
            },
          ],
        },
      ],
    });
    final design = catalog.design('clock.matrix');
    expect(design.size('small').label, '2×2');
    expect(design.defaults, _style);
    expect(design.toggles.first, isA<SwitchToggle>());
    expect((design.toggles.last as ChoiceToggle).options.keys, [
      'system',
      '24',
    ]);
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
    });
  });
}
