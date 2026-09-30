import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../features/editor/screens/editor_screen.dart';
import '../../features/gallery/screens/gallery_screen.dart';
import '../../features/widgets/services/engine.dart';
import '../theme.dart';

CustomTransitionPage<void> _fade(GoRouterState state, Widget child) =>
    CustomTransitionPage(
      key: state.pageKey,
      transitionDuration: TesseraTheme.motion,
      reverseTransitionDuration: const Duration(milliseconds: 320),
      child: child,
      transitionsBuilder: (context, animation, _, child) => FadeTransition(
        opacity: CurvedAnimation(parent: animation, curve: TesseraTheme.ease),
        child: child,
      ),
    );

final appRouter = GoRouter(
  routes: [
    GoRoute(
      path: '/',
      builder: (context, state) => const GalleryScreen(),
      routes: [
        GoRoute(
          path: 'design/:design',
          pageBuilder: (context, state) => _fade(
            state,
            DraftEditorScreen(design: state.pathParameters['design']!),
          ),
        ),
        GoRoute(
          path: 'widget/:id',
          pageBuilder: (context, state) => _fade(
            state,
            PlacedEditorScreen(
              widgetId: int.parse(state.pathParameters['id']!),
            ),
          ),
        ),
        GoRoute(
          path: 'configure/:id',
          builder: (context, state) => GalleryScreen(
            configuring: int.parse(state.pathParameters['id']!),
          ),
          routes: [
            GoRoute(
              path: ':design',
              pageBuilder: (context, state) => _fade(
                state,
                PlacedEditorScreen(
                  widgetId: int.parse(state.pathParameters['id']!),
                  configureDesign: state.pathParameters['design'],
                ),
              ),
            ),
          ],
        ),
      ],
    ),
  ],
);

void openLaunchTarget(LaunchTarget target) => switch (target) {
  LaunchEdit(:final widgetId) => appRouter.go('/widget/$widgetId'),
  LaunchConfigure(:final widgetId) => appRouter.go('/configure/$widgetId'),
};
