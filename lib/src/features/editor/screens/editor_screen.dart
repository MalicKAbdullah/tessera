import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../widgets/models/widget_content.dart';
import '../../widgets/models/widget_kind.dart';
import '../../widgets/models/widget_style.dart';
import '../../widgets/providers/widget_providers.dart';
import '../../widgets/widgets/widget_preview.dart';
import '../widgets/controls.dart';

class EditorScreen extends ConsumerWidget {
  const EditorScreen({super.key, required this.kind});
  final WidgetKind kind;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final wide = MediaQuery.sizeOf(context).width >= 840;
    final preview = Padding(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
      child: Backdrop(
        child: SizedBox(
          height: wide ? 420 : 220,
          child: Center(child: WidgetPreview(kind: kind, width: 300)),
        ),
      ),
    );
    final controls = ListView(
      padding: const EdgeInsets.fromLTRB(24, 0, 24, 32),
      children: [
        ..._contentControls(kind),
        _StyleControls(kind: kind),
        const SizedBox(height: 32),
        _PinButton(kind: kind),
      ],
    );
    return Scaffold(
      appBar: AppBar(title: Text(kind.label)),
      body: SafeArea(
        top: false,
        child: wide
            ? Row(
                children: [
                  Expanded(
                    child: Align(
                      alignment: Alignment.topCenter,
                      child: preview,
                    ),
                  ),
                  Expanded(child: controls),
                ],
              )
            : Column(
                children: [
                  preview,
                  Expanded(child: controls),
                ],
              ),
      ),
    );
  }

  List<Widget> _contentControls(WidgetKind kind) => switch (kind) {
    WidgetKind.weather => const [_WeatherControls()],
    WidgetKind.countdown => const [_CountdownControls()],
    WidgetKind.note => const [_NoteControls()],
    WidgetKind.clock || WidgetKind.calendar || WidgetKind.battery => const [],
  };
}

class _StyleControls extends ConsumerWidget {
  const _StyleControls({required this.kind});
  final WidgetKind kind;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final style = ref.watch(styleProvider(kind));
    final notifier = ref.read(styleProvider(kind).notifier);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Section(
          title: 'Presets',
          child: SizedBox(
            height: 64,
            child: ListView.separated(
              scrollDirection: Axis.horizontal,
              itemCount: presets.length,
              separatorBuilder: (_, _) => const SizedBox(width: 14),
              itemBuilder: (context, i) {
                final p = presets[i];
                final active =
                    style.background == p.background &&
                    style.text == p.text &&
                    style.accent == p.accent;
                return Column(
                  children: [
                    ColorDot(
                      color: p.background,
                      selected: active,
                      onTap: () => notifier.update(
                        (s) => s.copyWith(
                          background: p.background,
                          text: p.text,
                          accent: p.accent,
                        ),
                      ),
                    ),
                    const SizedBox(height: 6),
                    Text(p.name, style: Theme.of(context).textTheme.labelSmall),
                  ],
                );
              },
            ),
          ),
        ),
        Section(
          title: 'Background',
          child: Column(
            children: [
              SwatchRow(
                colors: [for (final p in presets) p.background],
                selected: style.background,
                onSelected: (c) =>
                    notifier.update((s) => s.copyWith(background: c)),
              ),
              const SizedBox(height: 8),
              LabeledSlider(
                label: 'Opacity',
                value: style.opacity,
                min: 0,
                max: 1,
                display: '${(style.opacity * 100).round()}%',
                onChanged: (v) =>
                    notifier.update((s) => s.copyWith(opacity: v)),
              ),
              LabeledSlider(
                label: 'Corners',
                value: style.radius,
                min: WidgetStyle.minRadius,
                max: WidgetStyle.maxRadius,
                display: '${style.radius.round()}',
                onChanged: (v) => notifier.update((s) => s.copyWith(radius: v)),
              ),
            ],
          ),
        ),
        Section(
          title: 'Text',
          child: SwatchRow(
            colors: swatches,
            selected: style.text,
            onSelected: (c) => notifier.update((s) => s.copyWith(text: c)),
          ),
        ),
        Section(
          title: 'Accent',
          child: SwatchRow(
            colors: swatches,
            selected: style.accent,
            onSelected: (c) => notifier.update((s) => s.copyWith(accent: c)),
          ),
        ),
        Section(
          title: 'Type',
          child: Column(
            children: [
              SizedBox(
                width: double.infinity,
                child: SegmentedButton<TextWeight>(
                  showSelectedIcon: false,
                  segments: const [
                    ButtonSegment(
                      value: TextWeight.light,
                      label: Text('Light'),
                    ),
                    ButtonSegment(
                      value: TextWeight.regular,
                      label: Text('Regular'),
                    ),
                    ButtonSegment(
                      value: TextWeight.medium,
                      label: Text('Medium'),
                    ),
                  ],
                  selected: {style.weight},
                  onSelectionChanged: (v) =>
                      notifier.update((s) => s.copyWith(weight: v.single)),
                ),
              ),
              const SizedBox(height: 8),
              LabeledSlider(
                label: 'Size',
                value: style.scale,
                min: WidgetStyle.minScale,
                max: WidgetStyle.maxScale,
                display: '${(style.scale * 100).round()}%',
                onChanged: (v) => notifier.update((s) => s.copyWith(scale: v)),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _PinButton extends ConsumerWidget {
  const _PinButton({required this.kind});
  final WidgetKind kind;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final sync = ref.read(widgetSyncProvider);
    return FutureBuilder<bool>(
      future: sync.canPin(),
      builder: (context, snap) {
        if (snap.data != true) {
          return Text(
            'Long-press your home screen, choose Widgets, then Tessera.',
            textAlign: TextAlign.center,
            style: Theme.of(context).textTheme.bodySmall?.copyWith(
              color: Theme.of(context).colorScheme.onSurfaceVariant,
            ),
          );
        }
        return SizedBox(
          height: 52,
          child: FilledButton.icon(
            onPressed: () => sync.pin(kind),
            icon: const Icon(Icons.add, size: 18),
            label: const Text('Add to home screen'),
          ),
        );
      },
    );
  }
}

class _NoteControls extends ConsumerWidget {
  const _NoteControls();

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

class _CountdownControls extends ConsumerWidget {
  const _CountdownControls();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final content = ref.watch(contentProvider);
    final notifier = ref.read(contentProvider.notifier);
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
          ListTile(
            contentPadding: EdgeInsets.zero,
            title: const Text('Date'),
            trailing: Text(DateFormat('d MMM y').format(target)),
            onTap: () async {
              final picked = await showDatePicker(
                context: context,
                initialDate: target,
                firstDate: DateTime(now.year - 5),
                lastDate: DateTime(now.year + 50),
              );
              if (picked != null) {
                notifier.update((c) => c.copyWith(countdownDate: picked));
              }
            },
          ),
        ],
      ),
    );
  }
}

class _WeatherControls extends ConsumerStatefulWidget {
  const _WeatherControls();

  @override
  ConsumerState<_WeatherControls> createState() => _WeatherControlsState();
}

class _WeatherControlsState extends ConsumerState<_WeatherControls> {
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
    final city = ref.watch(contentProvider).city;
    final weather = ref.watch(weatherProvider);
    return Section(
      title: 'Location',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          TextField(
            decoration: InputDecoration(
              labelText: city == null ? 'Search a city' : 'Change city',
              helperText: city == null ? null : '${city.name}, ${city.region}',
              suffixIcon: weather.isLoading
                  ? const Padding(
                      padding: EdgeInsets.all(14),
                      child: SizedBox.square(
                        dimension: 16,
                        child: CircularProgressIndicator(strokeWidth: 1.5),
                      ),
                    )
                  : city == null
                  ? null
                  : IconButton(
                      tooltip: 'Refresh',
                      icon: const Icon(Icons.refresh, size: 20),
                      onPressed: () =>
                          ref.read(weatherProvider.notifier).refresh(),
                    ),
            ),
            onChanged: _search,
          ),
          if (_error != null || weather.hasError)
            Padding(
              padding: const EdgeInsets.only(top: 8),
              child: Text(
                _error ?? '${weather.error}',
                style: theme.textTheme.bodySmall?.copyWith(
                  color: theme.colorScheme.error,
                ),
              ),
            ),
          AnimatedSize(
            duration: const Duration(milliseconds: 280),
            curve: Curves.easeOutCubic,
            child: Column(
              children: [
                for (final c in _results)
                  ListTile(
                    contentPadding: EdgeInsets.zero,
                    title: Text(c.name),
                    subtitle: Text(c.region),
                    onTap: () {
                      FocusScope.of(context).unfocus();
                      setState(() => _results = const []);
                      ref.read(contentProvider.notifier).setCity(c);
                    },
                  ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
