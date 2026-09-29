import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'src/app.dart';
import 'src/features/widgets/data/tessera_store.dart';
import 'src/features/widgets/providers/widget_providers.dart';
import 'src/features/widgets/services/background_refresh.dart';
import 'src/features/widgets/services/widget_sync.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final prefs = await SharedPreferences.getInstance();
  runApp(ProviderScope(
    overrides: [sharedPreferencesProvider.overrideWithValue(prefs)],
    child: const TesseraApp(),
  ));
  // Widgets placed before this launch get the persisted look immediately.
  await const WidgetSync().pushAll(TesseraStore(prefs));
  await scheduleBackgroundRefresh();
}
