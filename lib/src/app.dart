import 'package:flutter/material.dart';

import 'core/router/app_router.dart';
import 'core/theme.dart';

class TesseraApp extends StatelessWidget {
  const TesseraApp({super.key});

  @override
  Widget build(BuildContext context) => MaterialApp.router(
        title: 'Tessera',
        debugShowCheckedModeBanner: false,
        theme: TesseraTheme.light(),
        darkTheme: TesseraTheme.dark(),
        routerConfig: appRouter,
      );
}
