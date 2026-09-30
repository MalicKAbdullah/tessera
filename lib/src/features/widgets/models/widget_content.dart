/// User-provided content for the widgets that show user data.
class WidgetContent {
  const WidgetContent({
    this.note = 'Less, but better.',
    this.noteAuthor = 'Dieter Rams',
    this.countdownTitle = 'New Year',
    this.countdownDate,
    this.countdownStart,
    this.events = const [],
    this.checklist = const [],
    this.city,
  });

  final String note;
  final String noteAuthor;
  final String countdownTitle;
  final DateTime? countdownDate;

  /// The day [countdownDate] was chosen: where a progress ring starts.
  final DateTime? countdownStart;

  /// Further events for the multi-countdown list.
  final List<CountdownEvent> events;
  final List<ChecklistItem> checklist;
  final City? city;

  /// Countdown target when the user has not picked one: next January 1st.
  DateTime effectiveCountdownDate(DateTime now) =>
      countdownDate ?? DateTime(now.year + 1);

  WidgetContent copyWith({
    String? note,
    String? noteAuthor,
    String? countdownTitle,
    DateTime? countdownDate,
    DateTime? countdownStart,
    List<CountdownEvent>? events,
    List<ChecklistItem>? checklist,
    City? city,
  }) => WidgetContent(
    note: note ?? this.note,
    noteAuthor: noteAuthor ?? this.noteAuthor,
    countdownTitle: countdownTitle ?? this.countdownTitle,
    countdownDate: countdownDate ?? this.countdownDate,
    countdownStart: countdownStart ?? this.countdownStart,
    events: events ?? this.events,
    checklist: checklist ?? this.checklist,
    city: city ?? this.city,
  );

  /// Picks the target and restarts the progress ring from [today].
  WidgetContent withCountdownDate(DateTime picked, DateTime today) => copyWith(
    countdownDate: picked,
    countdownStart: DateTime(today.year, today.month, today.day),
  );

  Map<String, Object?> toJson() => {
    'note': note,
    'noteAuthor': noteAuthor,
    'countdownTitle': countdownTitle,
    'countdownDate': countdownDate?.toIso8601String(),
    'countdownStart': countdownStart?.toIso8601String(),
    'events': [for (final e in events) e.toJson()],
    'checklist': [for (final i in checklist) i.toJson()],
    'city': city?.toJson(),
  };

  factory WidgetContent.fromJson(Map<String, dynamic> json) => WidgetContent(
    note: json['note'] as String,
    noteAuthor: json['noteAuthor'] as String,
    countdownTitle: json['countdownTitle'] as String,
    countdownDate: switch (json['countdownDate']) {
      final String s => DateTime.parse(s),
      _ => null,
    },
    // Content saved before these fields existed has none of them.
    countdownStart: switch (json['countdownStart']) {
      final String s => DateTime.parse(s),
      _ => null,
    },
    events: [
      for (final e in (json['events'] as List<dynamic>? ?? const []))
        CountdownEvent.fromJson(e as Map<String, dynamic>),
    ],
    checklist: [
      for (final i in (json['checklist'] as List<dynamic>? ?? const []))
        ChecklistItem.fromJson(i as Map<String, dynamic>),
    ],
    city: switch (json['city']) {
      final Map<String, dynamic> m => City.fromJson(m),
      _ => null,
    },
  );
}

class CountdownEvent {
  const CountdownEvent({required this.title, required this.date});
  final String title;
  final DateTime date;

  Map<String, Object> toJson() => {
    'title': title,
    'date': date.toIso8601String(),
  };

  factory CountdownEvent.fromJson(Map<String, dynamic> json) => CountdownEvent(
    title: json['title'] as String,
    date: DateTime.parse(json['date'] as String),
  );
}

class ChecklistItem {
  const ChecklistItem({required this.text, this.done = false});
  final String text;
  final bool done;

  ChecklistItem toggled() => ChecklistItem(text: text, done: !done);

  Map<String, Object> toJson() => {'text': text, 'done': done};

  factory ChecklistItem.fromJson(Map<String, dynamic> json) =>
      ChecklistItem(text: json['text'] as String, done: json['done'] as bool);
}

class City {
  const City({
    required this.name,
    required this.region,
    required this.latitude,
    required this.longitude,
  });

  final String name;
  final String region;
  final double latitude;
  final double longitude;

  Map<String, Object> toJson() => {
    'name': name,
    'region': region,
    'latitude': latitude,
    'longitude': longitude,
  };

  factory City.fromJson(Map<String, dynamic> json) => City(
    name: json['name'] as String,
    region: json['region'] as String,
    latitude: (json['latitude'] as num).toDouble(),
    longitude: (json['longitude'] as num).toDouble(),
  );
}

/// Whole days from [now]'s date to [target]'s date; negative once passed.
int daysUntil(DateTime target, DateTime now) {
  final a = DateTime.utc(now.year, now.month, now.day);
  final b = DateTime.utc(target.year, target.month, target.day);
  return b.difference(a).inDays;
}
