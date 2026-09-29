/// User-provided content for the widgets that show user data.
class WidgetContent {
  const WidgetContent({
    this.note = 'Less, but better.',
    this.noteAuthor = 'Dieter Rams',
    this.countdownTitle = 'New Year',
    this.countdownDate,
    this.city,
  });

  final String note;
  final String noteAuthor;
  final String countdownTitle;
  final DateTime? countdownDate;
  final City? city;

  /// Countdown target when the user has not picked one: next January 1st.
  DateTime effectiveCountdownDate(DateTime now) =>
      countdownDate ?? DateTime(now.year + 1);

  WidgetContent copyWith({
    String? note,
    String? noteAuthor,
    String? countdownTitle,
    DateTime? countdownDate,
    City? city,
  }) => WidgetContent(
    note: note ?? this.note,
    noteAuthor: noteAuthor ?? this.noteAuthor,
    countdownTitle: countdownTitle ?? this.countdownTitle,
    countdownDate: countdownDate ?? this.countdownDate,
    city: city ?? this.city,
  );

  Map<String, Object?> toJson() => {
    'note': note,
    'noteAuthor': noteAuthor,
    'countdownTitle': countdownTitle,
    'countdownDate': countdownDate?.toIso8601String(),
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
    city: switch (json['city']) {
      final Map<String, dynamic> m => City.fromJson(m),
      _ => null,
    },
  );
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
