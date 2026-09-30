import 'dart:convert';

import 'package:http/http.dart' as http;

import '../models/widget_content.dart';

/// City search via Open-Meteo geocoding. Forecasts are fetched natively
/// (data/Weather.kt); the city is typed by the user, so Tessera never needs
/// the location permission.
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
}

class WeatherException implements Exception {
  WeatherException(this.message);
  final String message;
  @override
  String toString() => message;
}
