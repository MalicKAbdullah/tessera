import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../features/editor/screens/editor_screen.dart';
import '../../features/gallery/screens/gallery_screen.dart';
import '../../features/widgets/models/widget_kind.dart';
import '../theme.dart';

final appRouter = GoRouter(
  routes: [
    GoRoute(
      path: '/',
      builder: (context, state) => const GalleryScreen(),
      routes: [
        GoRoute(
          path: 'edit/:kind',
          pageBuilder: (context, state) => CustomTransitionPage(
            key: state.pageKey,
            transitionDuration: TesseraTheme.motion,
            reverseTransitionDuration: const Duration(milliseconds: 320),
            child: EditorScreen(
                kind: WidgetKind.fromId(state.pathParameters['kind']!)),
            transitionsBuilder: (context, animation, _, child) {
              final curved = CurvedAnimation(
                  parent: animation, curve: TesseraTheme.ease);
              return FadeTransition(opacity: curved, child: child);
            },
          ),
        ),
      ],
    ),
  ],
);
