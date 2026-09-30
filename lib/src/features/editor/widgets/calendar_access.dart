import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../widgets/providers/widget_providers.dart';
import '../../widgets/services/engine.dart';
import 'controls.dart';

/// Calendar access for calendar designs: explains why before the system
/// dialog, and points to settings once access is denied for good.
class CalendarAccessControls extends ConsumerStatefulWidget {
  const CalendarAccessControls({super.key});

  @override
  ConsumerState<CalendarAccessControls> createState() =>
      _CalendarAccessControlsState();
}

class _CalendarAccessControlsState extends ConsumerState<CalendarAccessControls>
    with WidgetsBindingObserver {
  CalendarAccess? _access;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _load();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  /// Access can change in system settings while the app is in the background.
  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) _load();
  }

  Future<void> _load() async {
    final access = await ref.read(engineProvider).calendarAccess();
    if (mounted) setState(() => _access = access);
  }

  Future<void> _connect() async {
    final engine = ref.read(engineProvider);
    if (_access == CalendarAccess.blocked) {
      await engine.openAppSettings();
      return;
    }
    final proceed = await showModalBottomSheet<bool>(
      context: context,
      showDragHandle: true,
      builder: (context) => const _Rationale(),
    );
    if (proceed != true) return;
    final access = await engine.requestCalendar();
    if (mounted) setState(() => _access = access);
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final access = _access;
    if (access == null) return const SizedBox.shrink();
    final granted = access == CalendarAccess.granted;
    return Section(
      title: 'Calendar',
      child: Row(
        children: [
          Icon(
            granted ? Icons.event_available : Icons.event_outlined,
            color: granted
                ? theme.colorScheme.primary
                : theme.colorScheme.onSurfaceVariant,
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Text(switch (access) {
              CalendarAccess.granted =>
                'Connected. Events from the calendars on this phone appear '
                    'on the widget.',
              CalendarAccess.ask =>
                'Showing the date only. Connect your calendar to see '
                    'events.',
              CalendarAccess.blocked =>
                'Calendar access is off. Turn it on in settings to see '
                    'events.',
            }, style: theme.textTheme.bodyMedium),
          ),
          if (!granted) ...[
            const SizedBox(width: 12),
            ChoicePill(
              label: access == CalendarAccess.blocked ? 'Settings' : 'Connect',
              selected: true,
              onTap: _connect,
            ),
          ],
        ],
      ),
    );
  }
}

class _Rationale extends StatelessWidget {
  const _Rationale();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    Widget point(IconData icon, String text) => Padding(
      padding: const EdgeInsets.only(bottom: 14),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, size: 20, color: theme.colorScheme.primary),
          const SizedBox(width: 14),
          Expanded(child: Text(text, style: theme.textTheme.bodyMedium)),
        ],
      ),
    );
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(24, 0, 24, 16),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Show your events on widgets',
              style: theme.textTheme.titleLarge,
            ),
            const SizedBox(height: 20),
            point(
              Icons.event_note_outlined,
              'Tessera reads the title, time, place and colour of your next '
              'two weeks of events to draw them.',
            ),
            point(
              Icons.phone_android_outlined,
              'Everything stays on this phone. Nothing is uploaded or synced.',
            ),
            point(
              Icons.visibility_outlined,
              'Read-only: Tessera cannot add, change or delete events.',
            ),
            point(
              Icons.settings_outlined,
              'You can turn access off at any time in system settings.',
            ),
            const SizedBox(height: 8),
            PrimaryButton(
              onTap: () => Navigator.pop(context, true),
              child: const Text('Continue'),
            ),
            Center(
              child: QuietButton(
                label: 'Not now',
                onTap: () => Navigator.pop(context, false),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
