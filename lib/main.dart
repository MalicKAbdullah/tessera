import 'package:core_theme/core_theme.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'src/app.dart';
import 'src/core/router/app_router.dart';
import 'src/features/settings/settings_providers.dart';
import 'src/features/widgets/data/tessera_store.dart';
import 'src/features/widgets/providers/widget_providers.dart';
import 'src/features/widgets/services/engine.dart';
import 'src/features/widgets/services/migration.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final prefs = await SharedPreferences.getInstance();
  final appearance = await AppearanceController.load();
  LicenseRegistry.addLicense(_widgetFontLicences);
  final engine = Engine();
  final catalog = await engine.catalog();
  final store = TesseraStore(prefs);
  await migrateFromV1(store, engine, catalog);
  await engine.setContent(store.content());

  final container = ProviderContainer(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      engineProvider.overrideWithValue(engine),
      catalogProvider.overrideWithValue(catalog),
      appearanceProvider.overrideWithValue(appearance),
    ],
  );
  engine.onDataChanged = () =>
      container.read(dataRevisionProvider.notifier).state++;
  engine.onLaunchTarget = openLaunchTarget;
  AppLifecycleListener(
    onResume: () async {
      await reloadContent(container);
      // The launcher's configure screen runs a second engine; a choice made
      // in the other engine reaches this one only through the stored value.
      await appearance.setMode((await AppearanceController.load()).mode);
    },
  );
  runApp(
    UncontrolledProviderScope(container: container, child: const TesseraApp()),
  );
  final initial = await engine.launchTarget();
  if (initial != null) openLaunchTarget(initial);
}

/// The typefaces the widget engine bundles in res/font, all under the SIL
/// Open Font License. The licence texts ship in the APK's assets for the
/// engine and are declared as Flutter assets so the licence page lists them.
Stream<LicenseEntry> _widgetFontLicences() async* {
  const fonts = {
    'doto': 'Doto',
    'inter_tight': 'Inter Tight',
    'instrument_serif': 'Instrument Serif',
    'jetbrains_mono': 'JetBrains Mono',
    'oswald': 'Oswald',
    'space_grotesk': 'Space Grotesk',
  };
  for (final MapEntry(key: file, value: name) in fonts.entries) {
    final text = await rootBundle.loadString(
      'android/app/src/main/assets/licenses/${file}_OFL.txt',
    );
    yield LicenseEntryWithLineBreaks([name], text);
  }
}
