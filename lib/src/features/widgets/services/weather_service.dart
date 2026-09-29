import 'dart:convert';

import 'package:http/http.dart' as http;

import '../models/widget_content.dart';

class WeatherSnapshot {
  const WeatherSnapshot({
    required this.temperature,
    required this.code,
    required this.fetchedAt,
  });

  final double temperature;
  final int code;
  final DateTime fetchedAt;

  String get condition => describeWeatherCode(code);
  String get temperatureLabel => '${temperature.round()}°';

  Map<String, Object> toJson() => {
        'temperature': temperature,
        'code': code,
        'fetchedAt': fetchedAt.toIso8601String(),
      };

  factory WeatherSnapshot.fromJson(Map<String, dynamic> json) =>
      WeatherSnapshot(
        temperature: (json['temperature'] as num).toDouble(),
        code: json['code'] as int,
        fetchedAt: DateTime.parse(json['fetchedAt'] as String),
      );
}

/// Open-Meteo: free, keyless, and the city is typed by the user, so Tessera
/// never needs the location permission.
class WeatherService {
  WeatherService([http.Client? client]) : _client = client ?? http.Client();

  final http.Client _client;

  Future<List<City>> searchCities(String query) async {
    final uri = Uri.https('geocoding-api.open-meteo.com', '/v1/search', {
      'name': query,
      'count': '6',
      'format': 'json',
    });
    final res = await _client.get(uri);
    if (res.statusCode != 200) {
      throw WeatherException('City search failed (${res.statusCode})');
    }
    final body = jsonDecode(res.body) as Map<String, dynamic>;
    final results = (body['results'] as List<dynamic>?) ?? const [];
    return [
      for (final r in results.cast<Map<String, dynamic>>())
        City(
          name: r['name'] as String,
          region: [r['admin1'], r['country']].whereType<String>().join(', '),
          latitude: (r['latitude'] as num).toDouble(),
          longitude: (r['longitude'] as num).toDouble(),
        ),
    ];
  }

  Future<WeatherSnapshot> current(City city) async {
    final uri = Uri.https('api.open-meteo.com', '/v1/forecast', {
      'latitude': '${city.latitude}',
      'longitude': '${city.longitude}',
      'current': 'temperature_2m,weather_code',
      'timezone': 'auto',
    });
    final res = await _client.get(uri);
    if (res.statusCode != 200) {
      throw WeatherException('Weather fetch failed (${res.statusCode})');
    }
    final current = (jsonDecode(res.body) as Map<String, dynamic>)['current']
        as Map<String, dynamic>;
    return WeatherSnapshot(
      temperature: (current['temperature_2m'] as num).toDouble(),
      code: current['weather_code'] as int,
      fetchedAt: DateTime.now(),
    );
  }
}

class WeatherException implements Exception {
  WeatherException(this.message);
  final String message;
  @override
  String toString() => message;
}

/// WMO weather interpretation codes, as documented by Open-Meteo.
String describeWeatherCode(int code) => switch (code) {
      0 => 'Clear',
      1 => 'Mostly clear',
      2 => 'Partly cloudy',
      3 => 'Overcast',
      45 || 48 => 'Fog',
      51 || 53 || 55 || 56 || 57 => 'Drizzle',
      61 || 63 || 65 || 66 || 67 => 'Rain',
      71 || 73 || 75 || 77 => 'Snow',
      80 || 81 || 82 => 'Showers',
      85 || 86 => 'Snow showers',
      95 || 96 || 99 => 'Thunderstorm',
      _ => 'Unknown',
    };
