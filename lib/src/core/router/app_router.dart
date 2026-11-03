import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../features/editor/screens/editor_screen.dart';
import '../../features/gallery/screens/gallery_screen.dart';
import '../../features/settings/settings_screen.dart';
import '../../features/widgets/services/engine.dart';
import '../design/tokens.dart';

/// One transition for every screen: the incoming page fades up from a
/// slightly smaller scale while the outgoing one recedes. Shared previews
/// fly between them as heroes.
Page<void> _page(BuildContext context, GoRouterState state, Widget child) {
  final reduced = Motion.reduced(context);
  return CustomTransitionPage(
    key: state.pageKey,
    transitionDuration: reduced ? Duration.zero : Motion.page,
    reverseTransitionDuration: reduced ? Duration.zero : Motion.base,
    child: child,
    transitionsBuilder: (context, animation, secondary, child) {
      final inward = CurvedAnimation(
        parent: animation,
        curve: Motion.ease,
        reverseCurve: Motion.ease.flipped,
      );
      final outward = CurvedAnimation(parent: secondary, curve: Motion.ease);
      return FadeTransition(
        opacity: inward,
        child: ScaleTransition(
          scale: Tween(begin: 0.965, end: 1.0).animate(inward),
          child: FadeTransition(
            opacity: Tween(begin: 1.0, end: 0.0).animate(outward),
            child: ScaleTransition(
              scale: Tween(begin: 1.0, end: 1.03).animate(outward),
              child: child,
            ),
          ),
        ),
      );
    },
  );
}

/// Where a launch opens: the gallery, a tapped widget's editor, or the
/// launcher's configure flow for a widget being placed.
String launchLocation(LaunchTarget? target) => switch (target) {
  null => '/',
  LaunchEdit(:final widgetId) => '/widget/$widgetId',
  LaunchConfigure(:final widgetId) => '/configure/$widgetId',
};

/// Starts at [initialLocation] so a cold start from a widget tap opens on
/// its editor, with the gallery beneath it for back.
GoRouter createRouter({String initialLocation = '/'}) => GoRouter(
  initialLocation: initialLocation,
  routes: [
    GoRoute(
      path: '/',
      pageBuilder: (context, state) =>
          _page(context, state, const GalleryScreen()),
      routes: [
        GoRoute(
          path: 'settings',
          pageBuilder: (context, state) =>
              _page(context, state, const SettingsScreen()),
        ),
        GoRoute(
          path: 'design/:design',
          pageBuilder: (context, state) => _page(
            context,
            state,
            DraftEditorScreen(design: state.pathParameters['design']!),
          ),
        ),
        GoRoute(
          path: 'widget/:id',
          pageBuilder: (context, state) => _page(
            context,
            state,
            PlacedEditorScreen(
              widgetId: int.parse(state.pathParameters['id']!),
            ),
          ),
        ),
        GoRoute(
          path: 'configure/:id',
          pageBuilder: (context, state) => _page(
            context,
            state,
            GalleryScreen(configuring: int.parse(state.pathParameters['id']!)),
          ),
          routes: [
            GoRoute(
              path: ':design',
              pageBuilder: (context, state) => _page(
                context,
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

/// Created in main() once the launch target is known.
late final GoRouter appRouter;

/// A widget tapped while the app is already running.
void openLaunchTarget(LaunchTarget target) =>
    appRouter.go(launchLocation(target));
