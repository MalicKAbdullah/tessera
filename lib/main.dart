import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'src/app.dart';
import 'src/core/router/app_router.dart';
import 'src/features/widgets/data/tessera_store.dart';
import 'src/features/widgets/providers/widget_providers.dart';
import 'src/features/widgets/services/engine.dart';
import 'src/features/widgets/services/migration.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final prefs = await SharedPreferences.getInstance();
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
    ],
  );
  engine.onDataChanged = () =>
      container.read(dataRevisionProvider.notifier).state++;
  engine.onLaunchTarget = openLaunchTarget;
  runApp(
    UncontrolledProviderScope(container: container, child: const TesseraApp()),
  );
  final initial = await engine.launchTarget();
  if (initial != null) openLaunchTarget(initial);
}
