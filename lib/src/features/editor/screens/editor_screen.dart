import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/design/tokens.dart';
import '../../../core/motion/motion.dart';
import '../../../core/widgets/mosaic_mark.dart';
import '../../gallery/screens/gallery_screen.dart';
import '../../widgets/models/catalog.dart';
import '../../widgets/models/widget_style.dart';
import '../../widgets/providers/widget_providers.dart';
import '../../widgets/widgets/native_preview.dart';
import '../widgets/content_controls.dart';
import '../widgets/controls.dart';
import '../widgets/primary_action.dart';

/// Editing a design before adding it to the home screen.
class DraftEditorScreen extends StatelessWidget {
  const DraftEditorScreen({super.key, required this.design});
  final String design;

  @override
  Widget build(BuildContext context) => _Editor(
    target: DraftTarget(design),
    initial: null,
    fixedSize: null,
    heroTag: designHeroTag(design),
  );
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
        body: SafeArea(
          child: Column(
            children: [
              const _TopBar(title: '', caption: ''),
              Expanded(
                child: Center(
                  child: placed.isLoading
                      ? const MosaicLoader()
                      : const _Gone(),
                ),
              ),
            ],
          ),
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
      heroTag: configuring ? designHeroTag(design.id) : placedHeroTag(widgetId),
    );
  }
}

class _Gone extends StatelessWidget {
  const _Gone();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.all(Gap.xxl),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Opacity(opacity: 0.35, child: MosaicMark(size: 56)),
          const SizedBox(height: Gap.xl),
          Text('This widget is gone', style: theme.textTheme.titleMedium),
          const SizedBox(height: Gap.s),
          Text(
            'It is no longer on your home screen.',
            style: theme.textTheme.bodySmall,
          ),
          const SizedBox(height: Gap.l),
          QuietButton(label: 'Back to designs', onTap: () => context.go('/')),
        ],
      ),
    );
  }
}

class _Editor extends ConsumerStatefulWidget {
  const _Editor({
    required this.target,
    required this.initial,
    required this.fixedSize,
    required this.heroTag,
  });

  final EditTarget target;
  final WidgetStyle? initial;
  final SizeInfo? fixedSize;
  final String heroTag;

  @override
  ConsumerState<_Editor> createState() => _EditorState();
}

class _EditorState extends ConsumerState<_Editor>
    with SingleTickerProviderStateMixin {
  late SizeInfo _size =
      widget.fixedSize ??
      ref.read(catalogProvider).design(widget.target.design).sizes.first;

  /// Control sections rise in one after another behind the stage hero.
  late final _enter = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 1000),
  );

  @override
  void initState() {
    super.initState();
    final initial = widget.initial;
    if (initial != null) {
      ref.read(styleProvider(widget.target).notifier).load(initial);
    }
    _enter.forward();
  }

  @override
  void dispose() {
    _enter.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final catalog = ref.watch(catalogProvider);
    final design = catalog.design(widget.target.design);
    final style = ref.watch(styleProvider(widget.target));
    final theme = Theme.of(context);
    final p = Palette.of(context);
    final wide = MediaQuery.sizeOf(context).width >= 840;
    final stage = Padding(
      padding: const EdgeInsets.symmetric(horizontal: Gap.l),
      child: Column(
        children: [
          Hero(
            tag: widget.heroTag,
            child: Backdrop(
              child: SizedBox(
                height: wide ? 420 : 264,
                child: LayoutBuilder(
                  builder: (context, box) {
                    final aspect = _size.widthDp / _size.heightDp;
                    final width = ((box.maxHeight - 56) * aspect).clamp(
                      80.0,
                      box.maxWidth - 48,
                    );
                    return Center(
                      child: SpringResponse(
                        trigger: style,
                        child: NativePreview(
                          design: design.id,
                          style: style,
                          size: _size,
                          width: width,
                        ),
                      ),
                    );
                  },
                ),
              ),
            ),
          ),
          if (widget.fixedSize == null && design.sizes.length > 1)
            Padding(
              padding: const EdgeInsets.only(top: Gap.m),
              child: SizedBox(
                width: 64.0 * design.sizes.length + 6,
                child: PillSelector(
                  options: {for (final s in design.sizes) s: s.label},
                  selected: _size,
                  onSelected: (s) => setState(() => _size = s),
                ),
              ),
            ),
        ],
      ),
    );
    final sections = <Widget>[
      if (design.motion != null)
        Padding(
          padding: const EdgeInsets.only(top: Gap.xl),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Padding(
                padding: const EdgeInsets.only(top: 6, right: Gap.s),
                child: Container(
                  width: 6,
                  height: 6,
                  decoration: BoxDecoration(
                    color: p.accentInk,
                    shape: BoxShape.circle,
                  ),
                ),
              ),
              Expanded(
                child: Text(design.motion!, style: theme.textTheme.bodySmall),
              ),
            ],
          ),
        ),
      ...contentControls(design.category),
      if (design.toggles.isNotEmpty)
        _ToggleControls(target: widget.target, toggles: design.toggles),
      _TypeControls(target: widget.target),
      _ColourControls(target: widget.target),
      _SurfaceControls(target: widget.target),
      Padding(
        padding: const EdgeInsets.only(top: Gap.l),
        child: Center(
          child: QuietButton(
            label: 'Reset to design defaults',
            onTap: () =>
                ref.read(styleProvider(widget.target).notifier).reset(),
          ),
        ),
      ),
    ];
    final controls = ListView(
      physics: const BouncingScrollPhysics(
        parent: AlwaysScrollableScrollPhysics(),
      ),
      padding: const EdgeInsets.fromLTRB(Gap.xl, 0, Gap.xl, 132),
      children: [
        for (var i = 0; i < sections.length; i++)
          Entrance(
            animation: _enter,
            index: i,
            depth: false,
            child: sections[i],
          ),
      ],
    );
    final action = Positioned(
      left: 0,
      right: 0,
      bottom: 0,
      child: DecoratedBox(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [p.canvas.withValues(alpha: 0), p.canvas, p.canvas],
            stops: const [0, 0.35, 1],
          ),
        ),
        child: SafeArea(
          top: false,
          child: Padding(
            padding: const EdgeInsets.fromLTRB(Gap.xl, Gap.xxl, Gap.xl, Gap.l),
            child: PrimaryAction(target: widget.target, size: _size),
          ),
        ),
      ),
    );
    final bar = _TopBar(
      title: design.name,
      caption: catalog.categories
          .firstWhere((c) => c.id == design.category)
          .label
          .toUpperCase(),
    );
    return Scaffold(
      body: SafeArea(
        bottom: false,
        child: wide
            ? Row(
                children: [
                  Expanded(child: Column(children: [bar, stage])),
                  Expanded(child: Stack(children: [controls, action])),
                ],
              )
            : Stack(
                children: [
                  Column(
                    children: [
                      bar,
                      stage,
                      Expanded(child: controls),
                    ],
                  ),
                  action,
                ],
              ),
      ),
    );
  }
}

class _TopBar extends StatelessWidget {
  const _TopBar({required this.title, required this.caption});
  final String title;
  final String caption;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final p = Palette.of(context);
    return Padding(
      padding: const EdgeInsets.fromLTRB(Gap.s, Gap.s, Gap.xl, Gap.m),
      child: Row(
        children: [
          Pressable(
            scale: 0.9,
            semanticLabel: 'Back',
            onTap: () => context.pop(),
            child: Container(
              width: 44,
              height: 44,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                border: Border.all(color: p.hairline),
              ),
              child: Icon(Icons.arrow_back, size: 20, color: p.ink),
            ),
          ),
          const SizedBox(width: Gap.m),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: theme.textTheme.titleLarge),
                if (caption.isNotEmpty)
                  Text(caption, style: theme.textTheme.labelSmall),
              ],
            ),
          ),
        ],
      ),
    );
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
              SwitchToggle() => SwitchRow(
                label: t.label,
                value: style.toggles[t.key] as bool? ?? t.defaultValue,
                onChanged: (v) =>
                    notifier.update((s) => s.withToggle(t.key, v)),
              ),
              ChoiceToggle() => Padding(
                padding: const EdgeInsets.symmetric(vertical: Gap.s),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      t.label,
                      style: Theme.of(context).textTheme.bodyMedium,
                    ),
                    const SizedBox(height: Gap.s),
                    OptionPicker(
                      options: t.options,
                      selected:
                          style.toggles[t.key] as String? ?? t.defaultValue,
                      onSelected: (v) =>
                          notifier.update((s) => s.withToggle(t.key, v)),
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
    return Section(
      title: 'Typography',
      child: Column(
        children: [
          SizedBox(
            height: 84,
            child: ListView.separated(
              scrollDirection: Axis.horizontal,
              physics: const BouncingScrollPhysics(),
              clipBehavior: Clip.none,
              itemCount: catalog.fonts.length,
              separatorBuilder: (_, _) => const SizedBox(width: Gap.s),
              itemBuilder: (context, i) {
                final f = catalog.fonts[i];
                return _FontCard(
                  font: f,
                  weight: snapWeight(style.weight, f.weights),
                  selected: f.key == style.font,
                  onTap: () => notifier.update(
                    (s) => s.copyWith(
                      font: f.key,
                      weight: snapWeight(s.weight, f.weights),
                    ),
                  ),
                );
              },
            ),
          ),
          const SizedBox(height: Gap.m),
          LabeledSlider(
            label: 'Weight',
            value: style.weight.toDouble(),
            min: 100,
            max: 900,
            steps: 8,
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
            steps: 10,
            display: '${(style.scale * 100).round()}%',
            onChanged: (v) => notifier.update((s) => s.copyWith(scale: v)),
          ),
          LabeledSlider(
            label: 'Spacing',
            value: style.tracking,
            min: WidgetStyle.minTracking,
            max: WidgetStyle.maxTracking,
            steps: 10,
            display: style.tracking.toStringAsFixed(2),
            onChanged: (v) => notifier.update((s) => s.copyWith(tracking: v)),
          ),
        ],
      ),
    );
  }
}

/// One family, drawn by the engine in its own face at the current weight.
class _FontCard extends StatelessWidget {
  const _FontCard({
    required this.font,
    required this.weight,
    required this.selected,
    required this.onTap,
  });

  final FontInfo font;
  final int weight;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final p = Palette.of(context);
    return Pressable(
      semanticLabel: font.label,
      onTap: onTap,
      child: AnimatedContainer(
        duration: Motion.of(context, Motion.base),
        curve: Motion.ease,
        padding: const EdgeInsets.fromLTRB(Gap.m, Gap.s, Gap.l, Gap.s),
        decoration: BoxDecoration(
          color: selected ? p.surface : p.canvas,
          borderRadius: BorderRadius.circular(Radii.s + 4),
          border: Border.all(
            color: selected ? p.ink : p.hairline,
            width: selected ? 1.5 : 1,
          ),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            _Specimen(font: font.key, weight: weight, color: p.ink),
            const SizedBox(height: Gap.xs),
            Text(
              font.label.toUpperCase(),
              style: Theme.of(context).textTheme.labelSmall!.copyWith(
                color: selected ? p.ink : p.muted,
              ),
            ),
          ],
        ),
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
    final bytes = png.valueOrNull;
    return SizedBox(
      height: 32,
      child: AnimatedSwitcher(
        duration: Motion.of(context, Motion.base),
        layoutBuilder: (current, previous) => Stack(
          alignment: Alignment.centerLeft,
          children: [...previous, ?current],
        ),
        child: bytes == null
            ? const SizedBox(width: 72, key: ValueKey('empty'))
            : Image.memory(
                bytes,
                key: ValueKey('$font/$weight/${color.toARGB32()}'),
                height: 32,
              ),
      ),
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
    final theme = Theme.of(context);
    return Section(
      title: 'Surface',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          OptionPicker(
            options: {
              // Photo backgrounds need a picker; the engine already draws them.
              for (final k in BgKind.values.where((k) => k != BgKind.photo))
                k: k.label,
            },
            selected: bg.kind,
            onSelected: (k) => notifier.update(
              (s) => s.copyWith(background: bg.copyWith(kind: k)),
            ),
          ),
          AnimatedSize(
            duration: Motion.of(context, Motion.base),
            curve: Motion.ease,
            alignment: Alignment.topCenter,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                if (bg.kind != BgKind.transparent) ...[
                  const SizedBox(height: Gap.l),
                  SwatchRow(
                    colors: swatches,
                    selected: bg.color,
                    onSelected: (c) => notifier.update(
                      (s) => s.copyWith(background: bg.copyWith(color: c)),
                    ),
                  ),
                ],
                if (bg.kind == BgKind.gradient) ...[
                  const SizedBox(height: Gap.m),
                  Text('Blends into', style: theme.textTheme.bodySmall),
                  const SizedBox(height: Gap.s),
                  SwatchRow(
                    colors: swatches,
                    selected: bg.color2,
                    onSelected: (c) => notifier.update(
                      (s) => s.copyWith(background: bg.copyWith(color2: c)),
                    ),
                  ),
                ],
              ],
            ),
          ),
          const SizedBox(height: Gap.s),
          LabeledSlider(
            label: 'Opacity',
            value: style.opacity,
            min: 0,
            max: 1,
            steps: 10,
            display: '${(style.opacity * 100).round()}%',
            onChanged: (v) => notifier.update((s) => s.copyWith(opacity: v)),
          ),
          LabeledSlider(
            label: 'Corners',
            value: style.radius,
            min: WidgetStyle.minRadius,
            max: WidgetStyle.maxRadius,
            steps: 12,
            display: '${style.radius.round()}',
            onChanged: (v) => notifier.update((s) => s.copyWith(radius: v)),
          ),
          LabeledSlider(
            label: 'Padding',
            value: style.padding,
            min: WidgetStyle.minPadding,
            max: WidgetStyle.maxPadding,
            steps: 10,
            display: '${style.padding.round()}',
            onChanged: (v) => notifier.update((s) => s.copyWith(padding: v)),
          ),
        ],
      ),
    );
  }
}
