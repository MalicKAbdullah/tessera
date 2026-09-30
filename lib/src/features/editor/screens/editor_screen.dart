import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../widgets/models/catalog.dart';
import '../../widgets/models/widget_content.dart';
import '../../widgets/models/widget_style.dart';
import '../../widgets/providers/widget_providers.dart';
import '../../widgets/widgets/native_preview.dart';
import '../widgets/calendar_access.dart';
import '../widgets/content_controls.dart';
import '../widgets/controls.dart';
import '../widgets/photo_controls.dart';

/// Editing a design before adding it to the home screen.
class DraftEditorScreen extends StatelessWidget {
  const DraftEditorScreen({super.key, required this.design});
  final String design;

  @override
  Widget build(BuildContext context) =>
      _Editor(target: DraftTarget(design), initial: null, fixedSize: null);
}

/// Editing a widget already on the home screen, or choosing the design for
/// one the launcher is placing ([configureDesign] set).
class PlacedEditorScreen extends ConsumerWidget {
  const PlacedEditorScreen({
    super.key,
    required this.widgetId,
    this.configureDesign,
  });
  final int widgetId;
  final String? configureDesign;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final placed = ref.watch(placedProvider);
    final widget = placed.valueOrNull
        ?.where((p) => p.id == widgetId)
        .firstOrNull;
    if (widget == null) {
      return Scaffold(
        appBar: AppBar(),
        body: Center(
          child: placed.isLoading
              ? const CircularProgressIndicator(strokeWidth: 1.5)
              : const Text('This widget is no longer on your home screen.'),
        ),
      );
    }
    final catalog = ref.watch(catalogProvider);
    final configuring = configureDesign != null;
    final design = catalog.design(configureDesign ?? widget.design);
    return _Editor(
      target: PlacedTarget(design.id, widgetId, configuring: configuring),
      initial: configuring
          ? ref.read(storeProvider).draft(design.id) ?? design.defaults
          : widget.style,
      fixedSize: design.size(widget.size),
    );
  }
}

class _Editor extends ConsumerStatefulWidget {
  const _Editor({
    required this.target,
    required this.initial,
    required this.fixedSize,
  });

  final EditTarget target;
  final WidgetStyle? initial;
  final SizeInfo? fixedSize;

  @override
  ConsumerState<_Editor> createState() => _EditorState();
}

class _EditorState extends ConsumerState<_Editor> {
  late SizeInfo _size =
      widget.fixedSize ??
      ref.read(catalogProvider).design(widget.target.design).sizes.first;

  @override
  void initState() {
    super.initState();
    final initial = widget.initial;
    if (initial != null) {
      ref.read(styleProvider(widget.target).notifier).load(initial);
    }
  }

  @override
  Widget build(BuildContext context) {
    final catalog = ref.watch(catalogProvider);
    final design = catalog.design(widget.target.design);
    final style = ref.watch(styleProvider(widget.target));
    final wide = MediaQuery.sizeOf(context).width >= 840;
    final preview = Padding(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
      child: Backdrop(
        child: SizedBox(
          height: wide ? 420 : 250,
          child: LayoutBuilder(
            builder: (context, box) {
              final aspect = _size.widthDp / _size.heightDp;
              final width = ((box.maxHeight - 40) * aspect).clamp(
                80.0,
                box.maxWidth - 40,
              );
              return Center(
                child: NativePreview(
                  design: design.id,
                  style: style,
                  size: _size,
                  width: width,
                ),
              );
            },
          ),
        ),
      ),
    );
    final controls = ListView(
      padding: const EdgeInsets.fromLTRB(24, 0, 24, 32),
      children: [
        if (widget.fixedSize == null && design.sizes.length > 1)
          Section(
            title: 'Size',
            child: Wrap(
              spacing: 8,
              children: [
                for (final s in design.sizes)
                  ChoiceChip(
                    label: Text(s.label),
                    selected: s.id == _size.id,
                    showCheckmark: false,
                    onSelected: (_) => setState(() => _size = s),
                  ),
              ],
            ),
          ),
        if (design.motion != null)
          Padding(
            padding: const EdgeInsets.only(top: 16),
            child: Text(
              design.motion!,
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                color: Theme.of(context).colorScheme.onSurfaceVariant,
              ),
            ),
          ),
        ..._contentControls(design.category),
        if (design.toggles.isNotEmpty)
          _ToggleControls(target: widget.target, toggles: design.toggles),
        _TypeControls(target: widget.target),
        _ColourControls(target: widget.target),
        _SurfaceControls(target: widget.target),
        const SizedBox(height: 16),
        TextButton(
          onPressed: () =>
              ref.read(styleProvider(widget.target).notifier).reset(),
          child: const Text('Reset to design defaults'),
        ),
        const SizedBox(height: 16),
        _PrimaryAction(target: widget.target, size: _size),
      ],
    );
    return Scaffold(
      appBar: AppBar(title: Text(design.name)),
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

  List<Widget> _contentControls(String category) => switch (category) {
    'calendar' => const [CalendarAccessControls()],
    'weather' || 'sky' => const [_WeatherControls()],
    'countdown' => const [_CountdownControls(), EventsControls()],
    'note' => const [_NoteControls(), ChecklistControls()],
    'photo' => const [PhotoControls()],
    _ => const [],
  };
}

class _PrimaryAction extends ConsumerWidget {
  const _PrimaryAction({required this.target, required this.size});
  final EditTarget target;
  final SizeInfo size;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final engine = ref.read(engineProvider);
    final style = ref.watch(styleProvider(target));
    final theme = Theme.of(context);
    switch (target) {
      case PlacedTarget(configuring: true):
        return SizedBox(
          height: 52,
          child: FilledButton.icon(
            onPressed: () => engine.finishConfigure(target.design, style),
            icon: const Icon(Icons.check, size: 18),
            label: const Text('Place widget'),
          ),
        );
      case PlacedTarget():
        return Text(
          'Changes apply to this widget as you make them.',
          textAlign: TextAlign.center,
          style: theme.textTheme.bodySmall?.copyWith(
            color: theme.colorScheme.onSurfaceVariant,
          ),
        );
      case DraftTarget():
        return FutureBuilder<bool>(
          future: engine.canPin(),
          builder: (context, snap) {
            if (snap.data != true) {
              return Text(
                'Long-press your home screen, choose Widgets, then Tessera.',
                textAlign: TextAlign.center,
                style: theme.textTheme.bodySmall?.copyWith(
                  color: theme.colorScheme.onSurfaceVariant,
                ),
              );
            }
            return SizedBox(
              height: 52,
              child: FilledButton.icon(
                onPressed: () async {
                  await engine.pin(target.design, style, size);
                  if (context.mounted) context.go('/');
                },
                icon: const Icon(Icons.add, size: 18),
                label: Text('Add ${size.label} to home screen'),
              ),
            );
          },
        );
    }
  }
}

class _ToggleControls extends ConsumerWidget {
  const _ToggleControls({required this.target, required this.toggles});
  final EditTarget target;
  final List<ToggleInfo> toggles;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final style = ref.watch(styleProvider(target));
    final notifier = ref.read(styleProvider(target).notifier);
    return Section(
      title: 'Options',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          for (final t in toggles)
            switch (t) {
              SwitchToggle() => SwitchListTile(
                contentPadding: EdgeInsets.zero,
                title: Text(t.label),
                value: style.toggles[t.key] as bool? ?? t.defaultValue,
                onChanged: (v) =>
                    notifier.update((s) => s.withToggle(t.key, v)),
              ),
              ChoiceToggle() => Padding(
                padding: const EdgeInsets.symmetric(vertical: 8),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(t.label),
                    const SizedBox(height: 6),
                    Wrap(
                      spacing: 6,
                      runSpacing: 6,
                      children: [
                        for (final o in t.options.entries)
                          ChoiceChip(
                            label: Text(o.value),
                            showCheckmark: false,
                            selected:
                                (style.toggles[t.key] as String? ??
                                    t.defaultValue) ==
                                o.key,
                            onSelected: (_) => notifier.update(
                              (s) => s.withToggle(t.key, o.key),
                            ),
                          ),
                      ],
                    ),
                  ],
                ),
              ),
            },
        ],
      ),
    );
  }
}

class _TypeControls extends ConsumerWidget {
  const _TypeControls({required this.target});
  final EditTarget target;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final style = ref.watch(styleProvider(target));
    final notifier = ref.read(styleProvider(target).notifier);
    final catalog = ref.watch(catalogProvider);
    final family = catalog.font(style.font);
    final ink = Theme.of(context).colorScheme.onSurface;
    return Section(
      title: 'Typography',
      child: Column(
        children: [
          SizedBox(
            height: 72,
            child: ListView.separated(
              scrollDirection: Axis.horizontal,
              itemCount: catalog.fonts.length,
              separatorBuilder: (_, _) => const SizedBox(width: 8),
              itemBuilder: (context, i) {
                final f = catalog.fonts[i];
                final selected = f.key == style.font;
                return InkWell(
                  borderRadius: BorderRadius.circular(14),
                  onTap: () => notifier.update(
                    (s) => s.copyWith(
                      font: f.key,
                      weight: snapWeight(s.weight, f.weights),
                    ),
                  ),
                  child: AnimatedContainer(
                    duration: const Duration(milliseconds: 200),
                    padding: const EdgeInsets.symmetric(
                      horizontal: 12,
                      vertical: 8,
                    ),
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(14),
                      border: Border.all(
                        color: selected ? ink : ink.withValues(alpha: 0.12),
                        width: selected ? 1.5 : 1,
                      ),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        _Specimen(
                          font: f.key,
                          weight: snapWeight(style.weight, f.weights),
                          color: ink,
                        ),
                        Text(
                          f.label,
                          style: Theme.of(context).textTheme.labelSmall,
                        ),
                      ],
                    ),
                  ),
                );
              },
            ),
          ),
          const SizedBox(height: 8),
          LabeledSlider(
            label: 'Weight',
            value: style.weight.toDouble(),
            min: 100,
            max: 900,
            display: '${style.weight}',
            onChanged: (v) => notifier.update(
              (s) => s.copyWith(weight: snapWeight(v.round(), family.weights)),
            ),
          ),
          LabeledSlider(
            label: 'Size',
            value: style.scale,
            min: WidgetStyle.minScale,
            max: WidgetStyle.maxScale,
            display: '${(style.scale * 100).round()}%',
            onChanged: (v) => notifier.update((s) => s.copyWith(scale: v)),
          ),
          LabeledSlider(
            label: 'Spacing',
            value: style.tracking,
            min: WidgetStyle.minTracking,
            max: WidgetStyle.maxTracking,
            display: style.tracking.toStringAsFixed(2),
            onChanged: (v) => notifier.update((s) => s.copyWith(tracking: v)),
          ),
        ],
      ),
    );
  }
}

class _Specimen extends ConsumerWidget {
  const _Specimen({
    required this.font,
    required this.weight,
    required this.color,
  });
  final String font;
  final int weight;
  final Color color;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final png = ref.watch(
      specimenProvider(SpecimenRequest(font, weight, color.toARGB32())),
    );
    return SizedBox(
      height: 30,
      child: png.valueOrNull == null
          ? const SizedBox(width: 60)
          : Image.memory(png.value!, height: 30, gaplessPlayback: true),
    );
  }
}

class _ColourControls extends ConsumerWidget {
  const _ColourControls({required this.target});
  final EditTarget target;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final style = ref.watch(styleProvider(target));
    final notifier = ref.read(styleProvider(target).notifier);
    return Column(
      children: [
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
      ],
    );
  }
}

class _SurfaceControls extends ConsumerWidget {
  const _SurfaceControls({required this.target});
  final EditTarget target;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final style = ref.watch(styleProvider(target));
    final notifier = ref.read(styleProvider(target).notifier);
    final bg = style.background;
    return Section(
      title: 'Surface',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Wrap(
            spacing: 6,
            runSpacing: 6,
            children: [
              // Photo backgrounds need a picker; the engine already draws them.
              for (final k in BgKind.values.where((k) => k != BgKind.photo))
                ChoiceChip(
                  label: Text(k.label),
                  showCheckmark: false,
                  selected: bg.kind == k,
                  onSelected: (_) => notifier.update(
                    (s) => s.copyWith(background: bg.copyWith(kind: k)),
                  ),
                ),
            ],
          ),
          if (bg.kind != BgKind.transparent) ...[
            const SizedBox(height: 12),
            SwatchRow(
              colors: swatches,
              selected: bg.color,
              onSelected: (c) => notifier.update(
                (s) => s.copyWith(background: bg.copyWith(color: c)),
              ),
            ),
          ],
          if (bg.kind == BgKind.gradient) ...[
            const SizedBox(height: 12),
            SwatchRow(
              colors: swatches,
              selected: bg.color2,
              onSelected: (c) => notifier.update(
                (s) => s.copyWith(background: bg.copyWith(color2: c)),
              ),
            ),
          ],
          const SizedBox(height: 8),
          LabeledSlider(
            label: 'Opacity',
            value: style.opacity,
            min: 0,
            max: 1,
            display: '${(style.opacity * 100).round()}%',
            onChanged: (v) => notifier.update((s) => s.copyWith(opacity: v)),
          ),
          LabeledSlider(
            label: 'Corners',
            value: style.radius,
            min: WidgetStyle.minRadius,
            max: WidgetStyle.maxRadius,
            display: '${style.radius.round()}',
            onChanged: (v) => notifier.update((s) => s.copyWith(radius: v)),
          ),
          LabeledSlider(
            label: 'Padding',
            value: style.padding,
            min: WidgetStyle.minPadding,
            max: WidgetStyle.maxPadding,
            display: '${style.padding.round()}',
            onChanged: (v) => notifier.update((s) => s.copyWith(padding: v)),
          ),
        ],
      ),
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
                notifier.update((c) => c.withCountdownDate(picked, now));
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
                  : IconButton(
                      tooltip: 'Refresh',
                      icon: const Icon(Icons.refresh, size: 20),
                      onPressed: () =>
                          ref.read(engineProvider).refreshWeather(),
                    ),
            ),
            onChanged: _search,
          ),
          if (_error != null)
            Padding(
              padding: const EdgeInsets.only(top: 8),
              child: Text(
                _error!,
                style: theme.textTheme.bodySmall?.copyWith(
                  color: theme.colorScheme.error,
                ),
              ),
            ),
          for (final c in _results)
            ListTile(
              contentPadding: EdgeInsets.zero,
              title: Text(c.name),
              subtitle: Text(c.region),
              onTap: () {
                FocusScope.of(context).unfocus();
                setState(() => _results = const []);
                ref
                    .read(contentProvider.notifier)
                    .update((content) => content.copyWith(city: c));
              },
            ),
        ],
      ),
    );
  }
}
