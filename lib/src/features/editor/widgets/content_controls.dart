import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../../core/design/tokens.dart';
import '../../../core/motion/motion.dart';
import '../../widgets/models/widget_content.dart';
import '../../widgets/providers/widget_providers.dart';
import 'calendar_access.dart';
import 'controls.dart';
import 'photo_controls.dart';

/// Controls for the user content a category shows (a city, a note, a date).
/// The editor places them above the style controls, each as its own
/// [Section]. A category with content adds one arm here and its widgets in
/// this file.
List<Widget> contentControls(String category) => switch (category) {
  'calendar' => const [CalendarAccessControls()],
  'weather' || 'sky' => const [WeatherControls()],
  'countdown' => const [CountdownControls(), EventsControls()],
  'note' => const [NoteControls(), ChecklistControls()],
  'photo' => const [PhotoControls()],
  _ => const [],
};

class NoteControls extends ConsumerWidget {
  const NoteControls({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final content = ref.read(contentProvider);
    final notifier = ref.read(contentProvider.notifier);
    return Section(
      title: 'Note',
      child: Column(
        children: [
          TextFormField(
            initialValue: content.note,
            minLines: 1,
            maxLines: 3,
            maxLength: 120,
            decoration: const InputDecoration(labelText: 'Text'),
            onChanged: (v) => notifier.update((c) => c.copyWith(note: v)),
          ),
          const SizedBox(height: Gap.s),
          TextFormField(
            initialValue: content.noteAuthor,
            decoration: const InputDecoration(
              labelText: 'Attribution (optional)',
            ),
            onChanged: (v) => notifier.update((c) => c.copyWith(noteAuthor: v)),
          ),
        ],
      ),
    );
  }
}

class CountdownControls extends ConsumerWidget {
  const CountdownControls({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final content = ref.watch(contentProvider);
    final notifier = ref.read(contentProvider.notifier);
    final theme = Theme.of(context);
    final p = Palette.of(context);
    final now = DateTime.now();
    final target = content.effectiveCountdownDate(now);
    return Section(
      title: 'Countdown',
      child: Column(
        children: [
          TextFormField(
            initialValue: content.countdownTitle,
            maxLength: 32,
            decoration: const InputDecoration(labelText: 'Title'),
            onChanged: (v) =>
                notifier.update((c) => c.copyWith(countdownTitle: v)),
          ),
          const SizedBox(height: Gap.s),
          Pressable(
            onTap: () async {
              final picked = await showDatePicker(
                context: context,
                initialDate: target,
                firstDate: DateTime(now.year - 5),
                lastDate: DateTime(now.year + 50),
              );
              if (picked != null) {
                notifier.update((c) => c.withCountdownDate(picked, now));
              }
            },
            child: Container(
              height: 52,
              padding: const EdgeInsets.symmetric(horizontal: Gap.l),
              decoration: BoxDecoration(
                color: p.surface,
                borderRadius: BorderRadius.circular(Radii.s),
                border: Border.all(color: p.hairline),
              ),
              child: Row(
                children: [
                  Expanded(
                    child: Text('Date', style: theme.textTheme.bodyMedium),
                  ),
                  AnimatedSwitcher(
                    duration: Motion.of(context, Motion.base),
                    child: Text(
                      DateFormat('d MMM y').format(target),
                      key: ValueKey(target),
                      style: theme.textTheme.labelMedium!.copyWith(
                        color: p.ink,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class WeatherControls extends ConsumerStatefulWidget {
  const WeatherControls({super.key});

  @override
  ConsumerState<WeatherControls> createState() => _WeatherControlsState();
}

class _WeatherControlsState extends ConsumerState<WeatherControls> {
  Timer? _debounce;
  List<City> _results = const [];
  String? _error;

  @override
  void dispose() {
    _debounce?.cancel();
    super.dispose();
  }

  void _search(String query) {
    _debounce?.cancel();
    if (query.trim().length < 2) {
      setState(() => _results = const []);
      return;
    }
    _debounce = Timer(const Duration(milliseconds: 400), () async {
      try {
        final found = await ref
            .read(weatherServiceProvider)
            .searchCities(query.trim());
        if (!mounted) return;
        setState(() {
          _results = found;
          _error = null;
        });
      } on Exception catch (e) {
        if (mounted) setState(() => _error = '$e');
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final p = Palette.of(context);
    final city = ref.watch(contentProvider).city;
    return Section(
      title: 'Location',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          TextField(
            decoration: InputDecoration(
              labelText: city == null ? 'Search a city' : 'Change city',
              helperText: city == null ? null : '${city.name}, ${city.region}',
              suffixIcon: city == null
                  ? null
                  : Pressable(
                      semanticLabel: 'Refresh weather',
                      onTap: () => ref.read(engineProvider).refreshWeather(),
                      child: Icon(Icons.refresh, size: 20, color: p.muted),
                    ),
            ),
            onChanged: _search,
          ),
          if (_error != null)
            Padding(
              padding: const EdgeInsets.only(top: Gap.s),
              child: Text(
                _error!,
                style: theme.textTheme.bodySmall?.copyWith(color: p.accent),
              ),
            ),
          AnimatedSize(
            duration: Motion.of(context, Motion.base),
            curve: Motion.ease,
            alignment: Alignment.topCenter,
            child: Column(
              children: [
                for (final c in _results)
                  Pressable(
                    scale: 0.985,
                    onTap: () {
                      FocusScope.of(context).unfocus();
                      setState(() => _results = const []);
                      ref
                          .read(contentProvider.notifier)
                          .update((content) => content.copyWith(city: c));
                    },
                    child: Container(
                      width: double.infinity,
                      padding: const EdgeInsets.symmetric(vertical: Gap.m),
                      decoration: BoxDecoration(
                        border: Border(bottom: BorderSide(color: p.hairline)),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(c.name, style: theme.textTheme.titleSmall),
                          Text(c.region, style: theme.textTheme.bodySmall),
                        ],
                      ),
                    ),
                  ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

/// A removable line in a content list: [child] plus a close affordance.
class _ListRow extends StatelessWidget {
  const _ListRow({
    required this.child,
    required this.onRemove,
    this.onTap,
    this.leading,
  });

  final Widget child;
  final VoidCallback onRemove;
  final VoidCallback? onTap;
  final Widget? leading;

  @override
  Widget build(BuildContext context) {
    final p = Palette.of(context);
    return Pressable(
      scale: 0.985,
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: Gap.s),
        decoration: BoxDecoration(
          border: Border(bottom: BorderSide(color: p.hairline)),
        ),
        child: Row(
          children: [
            if (leading != null) ...[leading!, const SizedBox(width: Gap.m)],
            Expanded(child: child),
            Pressable(
              scale: 0.85,
              semanticLabel: 'Remove',
              onTap: onRemove,
              child: Padding(
                padding: const EdgeInsets.all(Gap.s),
                child: Icon(Icons.close, size: 18, color: p.muted),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

/// Extra events for the "Up Next" countdown list.
class EventsControls extends ConsumerStatefulWidget {
  const EventsControls({super.key});

  @override
  ConsumerState<EventsControls> createState() => _EventsControlsState();
}

class _EventsControlsState extends ConsumerState<EventsControls> {
  final _title = TextEditingController();
  DateTime? _date;

  @override
  void dispose() {
    _title.dispose();
    super.dispose();
  }

  Future<void> _pickDate() async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: _date ?? now,
      firstDate: DateTime(now.year - 5),
      lastDate: DateTime(now.year + 50),
    );
    if (picked != null) setState(() => _date = picked);
  }

  void _add() {
    final title = _title.text.trim();
    final date = _date;
    if (title.isEmpty || date == null) return;
    ref
        .read(contentProvider.notifier)
        .update(
          (c) => c.copyWith(
            events: [
              ...c.events,
              CountdownEvent(title: title, date: date),
            ],
          ),
        );
    _title.clear();
    setState(() => _date = null);
  }

  void _remove(int index) => ref
      .read(contentProvider.notifier)
      .update((c) => c.copyWith(events: [...c.events]..removeAt(index)));

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final events = ref.watch(contentProvider).events;
    final format = DateFormat('d MMM y');
    return Section(
      title: 'More events (Up Next list)',
      child: Column(
        children: [
          AnimatedSize(
            duration: Motion.of(context, Motion.base),
            curve: Motion.ease,
            alignment: Alignment.topCenter,
            child: Column(
              children: [
                for (final (i, e) in events.indexed)
                  _ListRow(
                    onRemove: () => _remove(i),
                    child: Row(
                      children: [
                        Expanded(
                          child: Text(
                            e.title,
                            style: theme.textTheme.titleSmall,
                          ),
                        ),
                        Text(
                          format.format(e.date),
                          style: theme.textTheme.labelMedium,
                        ),
                      ],
                    ),
                  ),
              ],
            ),
          ),
          const SizedBox(height: Gap.m),
          TextField(
            controller: _title,
            maxLength: 32,
            decoration: const InputDecoration(labelText: 'Event'),
            onSubmitted: (_) => _add(),
          ),
          Row(
            children: [
              ChoicePill(
                label: _date == null ? 'Pick date' : format.format(_date!),
                selected: _date != null,
                onTap: _pickDate,
              ),
              const Spacer(),
              ChoicePill(label: 'Add', selected: true, onTap: _add),
            ],
          ),
        ],
      ),
    );
  }
}

/// Items for the checklist note; the checked state is toggled here.
class ChecklistControls extends ConsumerStatefulWidget {
  const ChecklistControls({super.key});

  @override
  ConsumerState<ChecklistControls> createState() => _ChecklistControlsState();
}

class _ChecklistControlsState extends ConsumerState<ChecklistControls> {
  final _text = TextEditingController();

  @override
  void dispose() {
    _text.dispose();
    super.dispose();
  }

  void _add() {
    final text = _text.text.trim();
    if (text.isEmpty) return;
    ref
        .read(contentProvider.notifier)
        .update(
          (c) => c.copyWith(
            checklist: [
              ...c.checklist,
              ChecklistItem(text: text),
            ],
          ),
        );
    _text.clear();
  }

  void _toggle(int index) => ref
      .read(contentProvider.notifier)
      .update(
        (c) => c.copyWith(
          checklist: [
            for (final (i, item) in c.checklist.indexed)
              i == index ? item.toggled() : item,
          ],
        ),
      );

  void _remove(int index) => ref
      .read(contentProvider.notifier)
      .update((c) => c.copyWith(checklist: [...c.checklist]..removeAt(index)));

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final p = Palette.of(context);
    final items = ref.watch(contentProvider).checklist;
    return Section(
      title: 'Checklist items',
      child: Column(
        children: [
          AnimatedSize(
            duration: Motion.of(context, Motion.base),
            curve: Motion.ease,
            alignment: Alignment.topCenter,
            child: Column(
              children: [
                for (final (i, item) in items.indexed)
                  _ListRow(
                    onTap: () => _toggle(i),
                    onRemove: () => _remove(i),
                    leading: AnimatedContainer(
                      duration: Motion.of(context, Motion.base),
                      curve: Curves.easeOutBack,
                      width: 20,
                      height: 20,
                      decoration: BoxDecoration(
                        color: item.done ? p.ink : Colors.transparent,
                        borderRadius: BorderRadius.circular(6),
                        border: Border.all(
                          color: item.done ? p.ink : p.faint,
                          width: 1.5,
                        ),
                      ),
                      child: item.done
                          ? Icon(Icons.check, size: 14, color: p.canvas)
                          : null,
                    ),
                    child: AnimatedDefaultTextStyle(
                      duration: Motion.of(context, Motion.base),
                      style: theme.textTheme.bodyMedium!.copyWith(
                        color: item.done ? p.muted : p.ink,
                        decoration: item.done
                            ? TextDecoration.lineThrough
                            : TextDecoration.none,
                      ),
                      child: Text(item.text),
                    ),
                  ),
              ],
            ),
          ),
          const SizedBox(height: Gap.m),
          TextField(
            controller: _text,
            maxLength: 60,
            decoration: InputDecoration(
              labelText: 'New item',
              suffixIcon: Pressable(
                semanticLabel: 'Add',
                onTap: _add,
                child: Icon(Icons.add, color: p.ink),
              ),
            ),
            onSubmitted: (_) => _add(),
          ),
        ],
      ),
    );
  }
}
