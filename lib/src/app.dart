import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'core/router/app_router.dart';
import 'core/theme.dart';
import 'features/settings/settings_providers.dart';

class TesseraApp extends ConsumerWidget {
  const TesseraApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final appearance = ref.watch(appearanceProvider);
    return ListenableBuilder(
      listenable: appearance,
      builder: (context, _) => MaterialApp.router(
        title: 'Tessera',
        debugShowCheckedModeBanner: false,
        theme: tesseraTheme(Brightness.light),
        darkTheme: tesseraTheme(Brightness.dark),
        themeMode: appearance.mode,
        routerConfig: appRouter,
      ),
    );
  }
}
