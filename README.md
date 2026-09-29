<div align="center">

# ◼️ Tessera

### Widgets, quietly yours.

A growing library of home-screen widgets — dot-matrix and analog clocks, word and world time, live battery gauges and history — each drawn natively, alive where Android allows, and styled exactly the way you like.

</div>

> ### ◼️ Simple, but effective
> No accounts, no ads, no tracking. Tessera stores your styles on your phone and only talks to the network for one thing: the weather for the city **you** type in.

Every widget is drawn by a native Kotlin engine at the widget's real size, and the app's previews are that same rendering — what you tune is what lands on your home screen.

## ✨ Features

**Clock** — Dot Matrix · Bold Stack · Chronograph (live analog hands) · Word Clock · Dual Time · Minimal with next alarm

**Battery** — Dot Cell · Ring Gauge with time to full · Segments with temperature, health and voltage · Big Numeric · 24-hour History

**Also** — Calendar, Weather, Countdown and Note tiles

**Live, without draining you**
- Time and dates are Android `TextClock`s in Tessera's own typefaces — always on the minute, no alarms
- Battery reads the system's state at every draw and refreshes the moment you plug in and as soon as the screen wakes
- Subtle motion where Android supports it: pulsing LEDs, charging fills, a rippling level
- Weather from [Open-Meteo](https://open-meteo.com), with how fresh the reading is

**Make it yours**
- Six bundled families — Doto (dot matrix), Space Grotesk, Inter Tight, JetBrains Mono, Instrument Serif, Oswald — with a weight slider from 100 to 900
- Size, letter spacing, text and accent colours
- Solid, gradient, dot-grid, grain or clear surfaces; opacity, corners and padding
- Per-design options: 12/24-hour, seconds, numerals, second city, and more
- Every widget on your home screen keeps its own design and style; tap one to edit it in place

**Place it**
- **Add to home screen** from the editor at the size you picked, or long-press your home screen → Widgets → Tessera

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for how the engine works and how to add a design.

## 🔑 Permissions — and why

| Permission | Why |
| --- | --- |
| **Internet** | City search and weather from Open-Meteo. Nothing else is sent. |
| **Run at startup** | Redraws your widgets and restarts their refresh after the phone reboots. |

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

- **Kotlin** — the widget engine: Canvas rendering, RemoteViews overlays, WorkManager refresh
- **Flutter** & **Dart** — the gallery and editor
- **Riverpod** (state) · **go_router** (navigation) · **Open-Meteo** (weather & geocoding)
- Fonts under the SIL Open Font License — see `android/app/src/main/assets/licenses/`

## 📄 License

[MIT](LICENSE) © 2026 Abdullah Malik — part of the [Secure Suite](https://github.com/MalicKAbdullah/secure-suite-core).
