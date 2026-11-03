# Tessera in the Offlyne family

Tessera joins Offlyne as an endorsed app: "Tessera, by Offlyne." The app
chrome (gallery, editor, settings, launcher icon, splash) moves onto the
shared core, [secure-suite-core](https://github.com/MalicKAbdullah/secure-suite-core),
and follows its `docs/DESIGN.md` and `docs/BRAND.md`. The widgets are content,
not chrome: the Kotlin engine and every design stay as they are.

## What changes and what stays

| Area | Joins Offlyne (replaced) | Stays Tessera |
|---|---|---|
| Colour | `Palette` (cool monochrome + lime) becomes `AppTheme.build(accent: AppAccents.tessera)` and `SuiteColors` | The wallpaper stand-in behind previews (`Palette.stage`), kept as a local constant |
| Type | Space Grotesk, Inter Tight and JetBrains Mono leave the Flutter font block; chrome uses Mona Sans, Mona Sans Display and Mona Sans Mono | All widget faces in `res/font/` (Doto, Space Grotesk, Inter Tight, JetBrains Mono, Instrument Serif, Oswald), drawn natively |
| Components | Buttons, text fields, selects, segmented controls, settings rows, banners and empty states come from `core_ui` | Editor-only controls (colour swatches, sliders, font specimens, size picker, preview stage), restyled with `SuiteColors` |
| Motion | Durations and curves from `AppMotion`; the scroll-linked tilt (`ScrollDepth`) is removed | `Pressable` and `SpringResponse` (direct manipulation, matching the `settle` curve), `Entrance`, preview cross-fades; all respect reduced motion |
| Mark | The 3×3 mosaic mark and its assembly animation leave the chrome; the intro shows the app tile (`SuiteAppIcon`) | The intro's copy and flow |
| Icon and splash | Brand-kit adaptive icon, monochrome layer, `flutter_native_splash` with tile and endorsement | |
| Settings | New Settings screen: Appearance, updates, About | |
| Updates | `core_update`: `GithubUpdateService(owner: 'MalicKAbdullah', repo: 'tessera')`, `UpdateBanner` on the gallery | Release workflow (already ships an `arm64-v8a` APK, which `pickApkUrl` prefers) |
| Widgets | Nothing | Engine, designs, default styles (dot-matrix type, lime LED accents), Roborazzi goldens |

Tessera takes no `core_lock`, `core_crypto`, `core_storage`, `core_backup` or
`core_notify`: it holds nothing secret (its photos and notes are shown on the
home screen by design) and posts no notifications.

## Accent: Orchid

Existing accents by hue, with Tessera in its gap:

| Accent (app) | Light | Dark | Hue |
|---|---|---|---|
| Lime (Warden) | `#457A0C` | `#A6E33C` | 89° |
| Jade (DoseWise) | `#08805F` | `#3EDDA6` | 164° |
| Cyan (OpenLock) | `#007E9C` | `#3FD0EE` | 192° |
| Cobalt (Budgetly) | `#0F62D6` | `#6AA6FF` | 215° |
| Indigo (Ledgerly) | `#4A4FE0` | `#9298FF` | 238° |
| Violet (Vaultly) | `#7341E6` | `#B197FF` | 258° |
| **Orchid (Tessera)** | **`#A21CAF`** | **`#E37BF2`** | **295°** |
| Berry (Reflect) | `#C81E5B` | `#FF6B9A` | 338° |

- **In plain words:** Tessera's chrome turns orchid (a purple-magenta) in place of electric lime.
- **Why this way:** the widest free gap on the wheel that no rule forbids sits between violet (258°) and berry (338°). Orchid sits 37° from violet and 43° from berry. White on `#A21CAF` is 6.3:1 (AA needs 4.5); `#E37BF2` on the dark background is 7.8:1 and on the dark surface 7.3:1.
- **Why not the alternatives:** lime is Warden's. Green collides with the shared positive colour, red with negative, amber/yellow with warning, and orange is banned by BRAND.md. Teal falls between cyan and jade, under 30° from each.

`AppAccents.tessera` uses containers `#F5E8F7` (light) and `#33203A` (dark),
`onAccentLight` white and `onAccentDark` the dark background, like the others.

## Icon glyph

Tessera's glyph is a 2×2 mosaic with one tile not yet laid: three outlined
tiles, and the family dot where the fourth tile goes. The dot is the next
widget you place.

On the shared 24-unit grid, with 2-unit round strokes:

```dart
const List<BrandShape> tesseraGlyph = [
  BrandShape('M5.5 4 L8.5 4 A1.5 1.5 0 0 1 10 5.5 L10 8.5 A1.5 1.5 0 0 1 8.5 10 L5.5 10 A1.5 1.5 0 0 1 4 8.5 L4 5.5 A1.5 1.5 0 0 1 5.5 4 Z', stroke: true),
  BrandShape('M15.5 4 L18.5 4 A1.5 1.5 0 0 1 20 5.5 L20 8.5 A1.5 1.5 0 0 1 18.5 10 L15.5 10 A1.5 1.5 0 0 1 14 8.5 L14 5.5 A1.5 1.5 0 0 1 15.5 4 Z', stroke: true),
  BrandShape('M5.5 14 L8.5 14 A1.5 1.5 0 0 1 10 15.5 L10 18.5 A1.5 1.5 0 0 1 8.5 20 L5.5 20 A1.5 1.5 0 0 1 4 18.5 L4 15.5 A1.5 1.5 0 0 1 5.5 14 Z', stroke: true),
  BrandShape('M15 17 A2 2 0 1 1 19 17 A2 2 0 1 1 15 17 Z', dot: true),
];
```

- Tiles are 6 units on the stroke centre line (8 outside the stroke), corner radius 1.5, with 2 units of clear space between them. The glyph spans 3 to 21, inside the grid's keyline.
- The dot has the standard radius 2 and sits at the centre of the empty cell (17, 17).
- White glyph on the `#A21CAF` tile, and the same glyph as the monochrome layer.

## Steps

Effort is for one person who knows both repos. Every step lands as its own PR
with green CI (format, analyze, `flutter test`, debug APK, Roborazzi verify).

| # | Step | Effort |
|---|---|---|
| 1 | **Core (secure-suite-core):** `AppAccents.tessera`, `SuiteApp.tessera` with `tesseraGlyph`, `brand/icons/tessera/` regenerated by `brand_render_test.dart`, Tessera rows in BRAND.md, DESIGN.md and `brand/icons/README.md` (the family becomes eight apps). | 0.5 day |
| 2 | **Dependencies:** `core_theme`, `core_ui` and `core_update` as git dependencies pinned to the step 1 merge SHA; `flutter_launcher_icons` and `flutter_native_splash` as dev dependencies. Riverpod stays on 2.x and go_router on 17, which the suite's apps already accept. | 1 hour |
| 3 | **Theme and type:** replace `TesseraTheme` and `Palette` with `AppTheme` and `SuiteColors` (`canvas`→`background`, `raised`→`surfaceAlt`, `ink`/`muted`/`faint`→`textPrimary`/`textSecondary`/`textTertiary`, `accentInk`→`accent`); drop the chrome font block; map `Motion` durations to `AppMotion`; re-record the six chrome goldens. | 1–1.5 days |
| 4 | **Components:** move generic controls to `core_ui`; restyle the editor-only controls. | 1–2 days |
| 5 | **Settings and Appearance:** a Settings route from the gallery app bar with `AppearanceSetting` near the top, a "Check for updates" row, and About (name, version, `SuiteEndorsement`, licences including the widget fonts' OFL). `AppearanceController.load()` before `runApp`, bound to `themeMode`. The configure screen runs the same `main()`, so it follows the setting too. | 0.5–1 day |
| 6 | **Updates:** `GithubUpdateService` provider, `UpdateBanner` on the gallery, `REQUEST_INSTALL_PACKAGES` in the manifest. | 0.5 day |
| 7 | **Icon, splash, endorsement:** install the brand-kit icon per `brand/icons/README.md` (`ios: false`, Tessera is Android-only), splash block from DESIGN.md, `LaunchTheme`/`NormalTheme` windows on `#F6F7F9` / `#0A0C10`, retire `MosaicMark` from the chrome, README and store line "Tessera, by Offlyne." | 0.5 day |
| 8 | **Device pass:** the checks in "Needs a device" below, on Android 12+ and one pre-12 device. | 0.5 day |

Total: about 5–7 days.

## Risks

- **Lime inside, orchid outside.** Many design defaults use the lime LED accent, so the gallery shows lime widgets in orchid chrome. That is intended: widgets are the user's content, and the accent is themable per widget. Chrome never uses lime.
- **Golden churn.** Step 3 re-records every Flutter chrome golden. The Roborazzi widget goldens must not change in any step; a diff there means the migration touched the engine.
- **Two engines.** The launcher's configure screen runs a second Flutter engine in the same process. Anything loaded before `runApp` (appearance, content) must be re-read on resume rather than cached for the life of the engine.
- **Core drift.** Tessera pins core by SHA like the other apps; core changes reach it only through a deliberate bump.
- **Brand reach.** Tessera has no lock screen and no notifications, so its only endorsements are the splash, About and the store listing.

## Needs a device

- Overlay alignment: overlays are positioned in px from the portrait size (`MIN_WIDTH` × `MAX_HEIGHT`) while the bitmap stretches to the real view. Check landscape home screens, tablets and foldables, where the real size differs.
- Pin flow on Pixel and Samsung launchers: the widget arrives bound to the chosen design and the editor shows "placed".
- Live ticker cadence (word clock, battery) with the screen on, and freshness after the screen has been off for an hour.
- Charging trigger, calendar content trigger and hourly weather after a reboot without opening the app.
- Photo picker on Android 13+ and the document picker on Android 8–10; large HEIC photos.
