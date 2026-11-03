# Tessera in the Offlyne family

Tessera joins Offlyne as an endorsed app: "Tessera, by Offlyne." The app
chrome (gallery, editor, settings, launcher icon, splash) moves onto the
shared core, [secure-suite-core](https://github.com/MalicKAbdullah/secure-suite-core),
and follows its `docs/DESIGN.md` and `docs/BRAND.md`. The widgets are content,
not chrome: the Kotlin engine and every design stay as they are.

## What changes and what stays

| Area | Joins Offlyne (replaced) | Stays Tessera |
|---|---|---|
| Colour | `Palette` (cool monochrome + lime) becomes `AppTheme.build(accent: AppAccents.tessera)` and `SuiteColors`, still lime | The wallpaper stand-in behind previews (`Palette.stage`), kept as a local constant |
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

## Accent: electric lime

Tessera keeps its electric lime. Warden, which shares the lime hue, is no
longer supported, so no two supported apps collide.

| | Value | Contrast |
|---|---|---|
| Light accent (`AppAccents.tessera.light`) | `#4F7300` deep lime ink | 5.5:1 under white text, 5.2:1 on the light background |
| Dark accent | `#D4FF3A` electric lime | 16.9:1 on the dark background, under dark text |
| Containers | `#F4FFCE` light, `#313A1F` dark | accent 5.3:1 and 10.4:1 on them |
| Launcher tile | `#D4FF3A` with an ink `#0E1116` glyph | 16.4:1 |

- **In plain words:** the chrome is lime, as before: a deep olive-lime for text, strokes and buttons on light screens, and the bright electric lime on dark screens.
- **Why this way:** lime is Tessera's identity and already lights the widgets' LED accents, so the chrome and the widgets agree. The suite's accent model has one colour per brightness that serves as fill under `onAccent` text and as text on the background, which the deep ink does in light mode and the electric lime does in dark mode.
- **Why not the alternatives:** electric lime as the light accent fails both jobs (white on `#D4FF3A` is 1.2:1, lime text on white is unreadable). A new hue, such as orchid, would split the app from its widgets. On the launcher tile the lime stays and the glyph turns ink, the one exception to the white-glyph rule, recorded in core's BRAND.md.

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
- Ink `#0E1116` glyph on the `#D4FF3A` tile; the monochrome layer is the same glyph in white, which the system tints.

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

- **Lime in two places.** Many design defaults use the lime LED accent, and the chrome's accent is lime too. Chrome lime marks only the accent roles (primary button, selection, focus), on the suite's neutral shell, so the previews still carry most of the colour on screen.
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
