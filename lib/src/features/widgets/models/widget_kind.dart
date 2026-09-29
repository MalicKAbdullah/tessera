/// Every home-screen widget Tessera ships. [id] is the storage key prefix
/// shared with the native providers, so it must never change once released.
enum WidgetKind {
  clock('clock', 'Clock', 'Time and date, always live', 'ClockWidget'),
  calendar('calendar', 'Calendar', 'Today at a glance', 'CalendarWidget'),
  battery('battery', 'Battery', 'Charge level and state', 'BatteryWidget'),
  weather(
    'weather',
    'Weather',
    'Current conditions for your city',
    'WeatherWidget',
  ),
  countdown(
    'countdown',
    'Countdown',
    'Days until what matters',
    'CountdownWidget',
  ),
  note('note', 'Note', 'A line worth keeping in view', 'NoteWidget');

  const WidgetKind(this.id, this.label, this.tagline, this.providerClass);

  final String id;
  final String label;
  final String tagline;
  final String providerClass;

  String get qualifiedAndroidName =>
      'com.malickabdullah.tessera.widgets.$providerClass';

  static WidgetKind fromId(String id) =>
      WidgetKind.values.firstWhere((k) => k.id == id);
}
