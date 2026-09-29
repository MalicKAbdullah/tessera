<div align="center">

# ◼️ Tessera

### Widgets, quietly yours.

Six calm home-screen widgets — clock, calendar, battery, weather, countdown, and a note — each styled exactly the way you like, with a live preview as you tune it.

![License](https://img.shields.io/badge/License-MIT-C9A77C?style=flat-square)
![Platform](https://img.shields.io/badge/Platform-Android-C9A77C?style=flat-square&logo=android)
![Built with Flutter](https://img.shields.io/badge/Built%20with-Flutter-027DFD?style=flat-square&logo=flutter)
![Permissions](https://img.shields.io/badge/Location%20permission-None-8FA8D8?style=flat-square)
![Trackers](https://img.shields.io/badge/Trackers-0-8FA8D8?style=flat-square)

</div>

> ### ◼️ Simple, but effective
> No accounts, no ads, no tracking. Tessera stores your styles on your phone and only talks to the network for one thing: the weather for the city **you** type in.

Most widget apps drown you in skins. Tessera gives you a small set of widgets that look finished out of the box, and just enough controls to make them yours.

## ✨ Features

**The widgets**
- **Clock** — time and date, driven by Android's own `TextClock`, so it's always on the minute with zero background work
- **Calendar** — month, day, and weekday at a glance, also native and always current
- **Battery** — charge level with a hairline meter, and whether you're plugged in
- **Weather** — current temperature and condition for a city you pick, via [Open-Meteo](https://open-meteo.com) (free, no API key)
- **Countdown** — days until a date that matters, recomputed on every redraw
- **Note** — a line you want to keep in view, with optional attribution

**Make it yours**
- **Nine curated presets** (Ink, Paper, Sand, Sage, Mist, Clay, Midnight, Moss, Plum) — each a background, text, and accent that belong together
- Per-widget **background colour**, **opacity**, and **corner radius**
- **Text** and **accent** colours from a curated palette, or any custom colour
- **Light / Regular / Medium** type weight and a **size** scale
- A **live preview** that glides between settings as you change them

**Place it**
- **Add to home screen** straight from the editor on launchers that support pinning; otherwise long-press your home screen → Widgets → Tessera
- Widgets resize freely; the background is redrawn to fit every size

## 🧠 How it works

1. The Flutter app keeps each widget's style and your content (note, countdown, city) in local storage.
2. On every change, it pushes a copy into [`home_widget`](https://pub.dev/packages/home_widget)'s shared preferences and asks the widget to redraw.
3. The Kotlin widget providers read that copy and build `RemoteViews`. The rounded, translucent surface is drawn as a bitmap sized to the widget; text colour, size and weight are applied directly so type stays crisp.
4. Clock and Calendar use `TextClock` and tick on their own. Battery reads the system's battery state at draw time. Countdown recomputes days at draw time.
5. A [`workmanager`](https://pub.dev/packages/workmanager) job runs every 30 minutes to refresh the weather and nudge every widget to redraw.

## 🔑 Permissions — and why

| Permission | Why |
| --- | --- |
| **Internet** | City search and current weather from Open-Meteo. Nothing else is sent. |

That's it. Weather uses the city you type, not your location — **no location permission**.

## 🚀 Getting Started

**Prerequisites:** [Flutter SDK](https://docs.flutter.dev/get-started/install) and Android Studio.

```sh
flutter pub get
flutter run
```

**Build a debug/release APK:**

```sh
flutter build apk --debug
flutter build apk --release
```

Run the checks:

```sh
flutter analyze
flutter test
```

> Home-screen widgets are **Android-only** for now. The app runs on iOS, but there are no iOS widget extensions yet.

## 🧱 Built With

- **Flutter** & **Dart** — the gallery, the editor, and the live previews
- **Kotlin** — the six `AppWidgetProvider`s and the surface renderer
- **Riverpod** (state) · **go_router** (navigation) · **home_widget** (app ↔ widget bridge) · **workmanager** (periodic refresh) · **battery_plus** · **Open-Meteo** (weather & geocoding)

## 📄 License

[MIT](LICENSE) © 2026 Abdullah Malik — part of the [Secure Suite](https://github.com/MalicKAbdullah/secure-suite-core).
