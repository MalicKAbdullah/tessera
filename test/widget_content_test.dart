import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:tessera/src/features/widgets/models/widget_content.dart';

void main() {
  test('events, checklist and countdown start round-trip', () {
    final content = WidgetContent(
      countdownDate: DateTime(2027, 3, 14),
      countdownStart: DateTime(2026, 9, 30),
      events: [CountdownEvent(title: 'Trip', date: DateTime(2026, 12, 1))],
      checklist: const [
        ChecklistItem(text: 'Passport', done: true),
        ChecklistItem(text: 'Charger'),
      ],
    );
    final decoded = WidgetContent.fromJson(
      jsonDecode(jsonEncode(content.toJson())) as Map<String, dynamic>,
    );
    expect(decoded.countdownStart, DateTime(2026, 9, 30));
    expect(decoded.events.single.title, 'Trip');
    expect(decoded.events.single.date, DateTime(2026, 12, 1));
    expect(decoded.checklist.map((i) => i.done), [true, false]);
  });

  test('content saved before these fields existed still loads', () {
    final decoded = WidgetContent.fromJson({
      'note': 'Hi',
      'noteAuthor': '',
      'countdownTitle': 'Trip',
      'countdownDate': null,
      'city': null,
    });
    expect(decoded.events, isEmpty);
    expect(decoded.checklist, isEmpty);
    expect(decoded.countdownStart, isNull);
  });

  test('choosing a date restarts the ring from today, time of day dropped', () {
    final content = const WidgetContent().withCountdownDate(
      DateTime(2027, 1, 1),
      DateTime(2026, 9, 30, 17, 45),
    );
    expect(content.countdownDate, DateTime(2027, 1, 1));
    expect(content.countdownStart, DateTime(2026, 9, 30));
  });

  test('toggling an item flips only its checked state', () {
    const item = ChecklistItem(text: 'Milk');
    expect(item.toggled().done, isTrue);
    expect(item.toggled().toggled().done, isFalse);
    expect(item.toggled().text, 'Milk');
  });
}
