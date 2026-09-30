package com.malickabdullah.tessera.data

import android.content.Context
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

data class HourForecast(val time: LocalDateTime, val temperature: Float, val code: Int, val precipitationChance: Int)

data class DayForecast(
    val date: LocalDate,
    val code: Int,
    val high: Float,
    val low: Float,
    val sunrise: LocalDateTime,
    val sunset: LocalDateTime,
    val uvMax: Float,
    val precipitationChance: Int,
)

data class WeatherState(
    val city: String,
    val fetchedAt: Instant,
    val temperature: Float,
    val feelsLike: Float,
    val humidity: Int,
    val windKmh: Float,
    val windDirection: Int,
    val gustsKmh: Float,
    val pressureHpa: Float,
    val code: Int,
    val isDay: Boolean,
    val uv: Float,
    val hourly: List<HourForecast>,
    val daily: List<DayForecast>,
    /** Offset of the city's clock; hourly and daily times are in city-local time. */
    val utcOffsetSeconds: Int,
) {
    val condition: String get() = describeWeatherCode(code)

    /** The city's wall-clock time at [at], comparable with forecast times. */
    fun localTime(at: Instant): LocalDateTime = LocalDateTime.ofInstant(at, ZoneOffset.ofTotalSeconds(utcOffsetSeconds))

    /** Hours from the current city-local hour onward; the cache holds 48 so a day ahead survives a late refresh. */
    fun hoursFrom(at: Instant): List<HourForecast> {
        val hour = localTime(at).withMinute(0).withSecond(0).withNano(0)
        return hourly.filter { !it.time.isBefore(hour) }
    }

    /** Days from the city-local today onward. */
    fun daysFrom(at: Instant): List<DayForecast> {
        val today = localTime(at).toLocalDate()
        return daily.filter { !it.date.isBefore(today) }
    }
}

/** Reads the forecast cached by [WeatherFetch]; null until the first fetch for a city succeeds. */
object WeatherSource : DataSource<WeatherState?> {
    internal const val PREFS = "tessera.weather"

    /** Bumped whenever the requested fields change; a cache of another version is refetched, never parsed. */
    internal const val CACHE_VERSION = 2

    override fun read(context: Context): WeatherState? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString("forecast", null) ?: return null
        val city = ContentSource.read(context).city ?: return null
        if (prefs.getString("city", null) != city.key || prefs.getInt("version", 0) != CACHE_VERSION) return null
        return parse(city.name, Instant.ofEpochMilli(prefs.getLong("fetchedAt", 0)), JSONObject(raw))
    }

    internal fun parse(city: String, fetchedAt: Instant, json: JSONObject): WeatherState {
        val current = json.getJSONObject("current")
        val hourly = json.getJSONObject("hourly")
        val daily = json.getJSONObject("daily")
        val hourTimes = hourly.getJSONArray("time")
        val dayTimes = daily.getJSONArray("time")
        return WeatherState(
            city = city,
            fetchedAt = fetchedAt,
            temperature = current.getDouble("temperature_2m").toFloat(),
            feelsLike = current.getDouble("apparent_temperature").toFloat(),
            humidity = current.getInt("relative_humidity_2m"),
            windKmh = current.getDouble("wind_speed_10m").toFloat(),
            windDirection = current.getInt("wind_direction_10m"),
            gustsKmh = current.getDouble("wind_gusts_10m").toFloat(),
            pressureHpa = current.getDouble("surface_pressure").toFloat(),
            code = current.getInt("weather_code"),
            isDay = current.getInt("is_day") == 1,
            uv = current.getDouble("uv_index").toFloat(),
            hourly = (0 until hourTimes.length()).map { i ->
                HourForecast(
                    LocalDateTime.parse(hourTimes.getString(i)),
                    hourly.getJSONArray("temperature_2m").getDouble(i).toFloat(),
                    hourly.getJSONArray("weather_code").getInt(i),
                    hourly.getJSONArray("precipitation_probability").optInt(i, 0),
                )
            },
            daily = (0 until dayTimes.length()).map { i ->
                DayForecast(
                    LocalDate.parse(dayTimes.getString(i)),
                    daily.getJSONArray("weather_code").getInt(i),
                    daily.getJSONArray("temperature_2m_max").getDouble(i).toFloat(),
                    daily.getJSONArray("temperature_2m_min").getDouble(i).toFloat(),
                    LocalDateTime.parse(daily.getJSONArray("sunrise").getString(i)),
                    LocalDateTime.parse(daily.getJSONArray("sunset").getString(i)),
                    daily.getJSONArray("uv_index_max").optDouble(i, 0.0).toFloat(),
                    daily.getJSONArray("precipitation_probability_max").optInt(i, 0),
                )
            },
            utcOffsetSeconds = json.getInt("utc_offset_seconds"),
        )
    }
}

/** Open-Meteo: free and keyless; the city is typed by the user, so no location permission. */
object WeatherFetch {
    private const val STALE_AFTER_MS = 30 * 60 * 1000L

    fun isStale(context: Context): Boolean {
        val city = ContentSource.read(context).city ?: return false
        val prefs = context.getSharedPreferences(WeatherSource.PREFS, Context.MODE_PRIVATE)
        return prefs.getString("city", null) != city.key ||
            prefs.getInt("version", 0) != WeatherSource.CACHE_VERSION ||
            System.currentTimeMillis() - prefs.getLong("fetchedAt", 0) > STALE_AFTER_MS
    }

    /**
     * Blocking; call from a worker. Returns false when no city is set. A
     * non-200 answer or a body that does not parse throws [IOException], so the
     * worker retries and the previous good forecast stays cached.
     */
    fun fetch(context: Context): Boolean {
        val city = ContentSource.read(context).city ?: return false
        val url = URL(
            "https://api.open-meteo.com/v1/forecast?latitude=${city.latitude}&longitude=${city.longitude}" +
                "&current=temperature_2m,apparent_temperature,relative_humidity_2m,wind_speed_10m," +
                "wind_direction_10m,wind_gusts_10m,surface_pressure,weather_code,is_day,uv_index" +
                "&hourly=temperature_2m,weather_code,precipitation_probability" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,uv_index_max," +
                "precipitation_probability_max&timezone=auto&forecast_days=7&forecast_hours=48",
        )
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        try {
            if (connection.responseCode != 200) throw IOException("Open-Meteo returned ${connection.responseCode}")
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            try {
                WeatherSource.parse(city.name, Instant.now(), JSONObject(body))
            } catch (e: JSONException) {
                throw IOException("Open-Meteo returned an unreadable forecast", e)
            }
            context.getSharedPreferences(WeatherSource.PREFS, Context.MODE_PRIVATE).edit()
                .putString("forecast", body)
                .putString("city", city.key)
                .putInt("version", WeatherSource.CACHE_VERSION)
                .putLong("fetchedAt", System.currentTimeMillis())
                .apply()
        } finally {
            connection.disconnect()
        }
        return true
    }
}

/** WMO weather interpretation codes, as documented by Open-Meteo. */
fun describeWeatherCode(code: Int): String = when (code) {
    0 -> "Clear"
    1 -> "Mostly clear"
    2 -> "Partly cloudy"
    3 -> "Overcast"
    45, 48 -> "Fog"
    51, 53, 55, 56, 57 -> "Drizzle"
    61, 63, 65, 66, 67 -> "Rain"
    71, 73, 75, 77 -> "Snow"
    80, 81, 82 -> "Showers"
    85, 86 -> "Snow showers"
    95, 96, 99 -> "Thunderstorm"
    else -> "Unknown"
}
