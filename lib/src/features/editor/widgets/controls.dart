import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../core/design/tokens.dart';
import '../../../core/motion/motion.dart';

/// A titled group of controls, separated from the previous one by a hairline.
class Section extends StatelessWidget {
  const Section({super.key, required this.title, required this.child});
  final String title;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    final p = Palette.of(context);
    return Container(
      margin: const EdgeInsets.only(top: Gap.xl),
      padding: const EdgeInsets.only(top: Gap.l),
      decoration: BoxDecoration(
        border: Border(top: BorderSide(color: p.hairline)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            title.toUpperCase(),
            style: Theme.of(context).textTheme.labelSmall,
          ),
          const SizedBox(height: Gap.m),
          child,
        ],
      ),
    );
  }
}

/// A slider with its value as a mono readout. It clicks once per [steps]th of
/// its range, and the readout lifts in the accent colour while dragged.
class LabeledSlider extends StatefulWidget {
  const LabeledSlider({
    super.key,
    required this.label,
    required this.value,
    required this.min,
    required this.max,
    required this.display,
    required this.onChanged,
    this.steps = 20,
  });

  final String label;
  final double value;
  final double min;
  final double max;
  final String display;
  final ValueChanged<double> onChanged;
  final int steps;

  @override
  State<LabeledSlider> createState() => _LabeledSliderState();
}

class _LabeledSliderState extends State<LabeledSlider> {
  bool _dragging = false;
  late int _detent = _detentOf(widget.value);

  int _detentOf(double v) =>
      ((v - widget.min) / (widget.max - widget.min) * widget.steps).round();

  void _changed(double v) {
    final detent = _detentOf(v);
    if (detent != _detent) {
      _detent = detent;
      HapticFeedback.selectionClick();
    }
    widget.onChanged(v);
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final p = Palette.of(context);
    return SizedBox(
      height: 44,
      child: Row(
        children: [
          SizedBox(
            width: 76,
            child: Text(widget.label, style: theme.textTheme.bodyMedium),
          ),
          Expanded(
            child: Slider(
              value: widget.value,
              min: widget.min,
              max: widget.max,
              onChangeStart: (_) => setState(() => _dragging = true),
              onChangeEnd: (_) => setState(() => _dragging = false),
              onChanged: _changed,
            ),
          ),
          SizedBox(
            width: 48,
            child: AnimatedScale(
              scale: _dragging ? 1.12 : 1,
              alignment: Alignment.centerRight,
              duration: Motion.of(context, Motion.quick),
              curve: Motion.ease,
              child: AnimatedDefaultTextStyle(
                duration: Motion.of(context, Motion.quick),
                style: theme.textTheme.labelMedium!.copyWith(
                  color: _dragging ? p.accent : p.muted,
                  fontFeatures: const [FontFeature.tabularFigures()],
                ),
                child: Text(widget.display, textAlign: TextAlign.end),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

/// Segmented control: equal segments on a track, with a pill that glides to
/// the selection.
class PillSelector<T> extends StatelessWidget {
  const PillSelector({
    super.key,
    required this.options,
    required this.selected,
    required this.onSelected,
  });

  /// value -> label, in display order.
  final Map<T, String> options;
  final T selected;
  final ValueChanged<T> onSelected;

  @override
  Widget build(BuildContext context) {
    final p = Palette.of(context);
    final theme = Theme.of(context);
    final keys = options.keys.toList();
    final index = keys.indexOf(selected);
    final n = keys.length;
    return Container(
      height: 40,
      padding: const EdgeInsets.all(3),
      decoration: BoxDecoration(
        color: p.surface,
        borderRadius: BorderRadius.circular(Radii.pill),
        border: Border.all(color: p.hairline),
      ),
      child: Stack(
        children: [
          if (index >= 0)
            AnimatedAlign(
              duration: Motion.of(context, Motion.base),
              curve: Motion.ease,
              alignment: Alignment(n == 1 ? 0 : -1 + 2 * index / (n - 1), 0),
              child: FractionallySizedBox(
                widthFactor: 1 / n,
                heightFactor: 1,
                child: DecoratedBox(
                  decoration: BoxDecoration(
                    color: p.ink,
                    borderRadius: BorderRadius.circular(Radii.pill),
                  ),
                ),
              ),
            ),
          Row(
            children: [
              for (final k in keys)
                Expanded(
                  child: Pressable(
                    onTap: k == selected ? null : () => onSelected(k),
                    child: Center(
                      child: AnimatedDefaultTextStyle(
                        duration: Motion.of(context, Motion.base),
                        style: theme.textTheme.labelLarge!.copyWith(
                          fontSize: 13,
                          color: k == selected ? p.canvas : p.muted,
                        ),
                        child: Text(
                          options[k]!,
                          maxLines: 1,
                          overflow: TextOverflow.fade,
                          softWrap: false,
                        ),
                      ),
                    ),
                  ),
                ),
            ],
          ),
        ],
      ),
    );
  }
}

/// A single selectable pill for option sets too long for [PillSelector].
class ChoicePill extends StatelessWidget {
  const ChoicePill({
    super.key,
    required this.label,
    required this.selected,
    required this.onTap,
  });

  final String label;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final p = Palette.of(context);
    return Pressable(
      onTap: onTap,
      child: AnimatedContainer(
        duration: Motion.of(context, Motion.base),
        curve: Motion.ease,
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 9),
        decoration: BoxDecoration(
          color: selected ? p.ink : p.surface,
          borderRadius: BorderRadius.circular(Radii.pill),
          border: Border.all(color: selected ? p.ink : p.hairline),
        ),
        child: AnimatedDefaultTextStyle(
          duration: Motion.of(context, Motion.base),
          style: Theme.of(context).textTheme.labelLarge!.copyWith(
            fontSize: 13,
            color: selected ? p.canvas : p.ink,
          ),
          child: Text(label),
        ),
      ),
    );
  }
}

/// Options that fit on one track use [PillSelector]; longer sets wrap.
class OptionPicker<T> extends StatelessWidget {
  const OptionPicker({
    super.key,
    required this.options,
    required this.selected,
    required this.onSelected,
  });

  final Map<T, String> options;
  final T selected;
  final ValueChanged<T> onSelected;

  static const _maxOnTrack = 5;

  @override
  Widget build(BuildContext context) {
    if (options.length <= _maxOnTrack) {
      return PillSelector(
        options: options,
        selected: selected,
        onSelected: onSelected,
      );
    }
    return Wrap(
      spacing: Gap.s,
      runSpacing: Gap.s,
      children: [
        for (final e in options.entries)
          ChoicePill(
            label: e.value,
            selected: e.key == selected,
            onTap: () => onSelected(e.key),
          ),
      ],
    );
  }
}

class SwitchRow extends StatelessWidget {
  const SwitchRow({
    super.key,
    required this.label,
    required this.value,
    required this.onChanged,
  });

  final String label;
  final bool value;
  final ValueChanged<bool> onChanged;

  @override
  Widget build(BuildContext context) => Pressable(
    scale: 0.99,
    onTap: () => onChanged(!value),
    child: SizedBox(
      height: 48,
      child: Row(
        children: [
          Expanded(
            child: Text(label, style: Theme.of(context).textTheme.bodyMedium),
          ),
          ExcludeSemantics(
            child: Switch(
              value: value,
              onChanged: (v) {
                HapticFeedback.selectionClick();
                onChanged(v);
              },
            ),
          ),
        ],
      ),
    ),
  );
}

/// A row of colour dots plus a custom picker at the end.
class SwatchRow extends StatelessWidget {
  const SwatchRow({
    super.key,
    required this.colors,
    required this.selected,
    required this.onSelected,
  });

  final List<Color> colors;
  final Color selected;
  final ValueChanged<Color> onSelected;

  @override
  Widget build(BuildContext context) {
    final custom = !colors.contains(selected);
    return Wrap(
      spacing: 10,
      runSpacing: 10,
      children: [
        for (final c in colors)
          ColorDot(
            color: c,
            selected: c == selected,
            onTap: () => onSelected(c),
          ),
        ColorDot(
          color: custom ? selected : null,
          selected: custom,
          onTap: () async {
            final picked = await showCustomColorPicker(context, selected);
            if (picked != null) onSelected(picked);
          },
        ),
      ],
    );
  }
}

class ColorDot extends StatelessWidget {
  const ColorDot({
    super.key,
    required this.color,
    required this.selected,
    required this.onTap,
  });

  /// Null draws the "custom colour" dot.
  final Color? color;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final p = Palette.of(context);
    return Pressable(
      scale: 0.9,
      onTap: onTap,
      semanticLabel: color == null ? 'Custom colour' : 'Colour',
      child: AnimatedContainer(
        duration: Motion.of(context, Motion.base),
        curve: Curves.easeOutBack,
        width: 34,
        height: 34,
        padding: EdgeInsets.all(selected ? 4 : 0),
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          border: Border.all(
            color: selected ? p.ink : p.hairline,
            width: selected ? 1.5 : 1,
          ),
        ),
        child: DecoratedBox(
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            color: color,
            gradient: color == null
                ? const SweepGradient(
                    colors: [
                      Color(0xFFE0A6A6),
                      Color(0xFFE3D39B),
                      Color(0xFFA6D1B0),
                      Color(0xFF9FB8DC),
                      Color(0xFFCDA6DA),
                      Color(0xFFE0A6A6),
                    ],
                  )
                : null,
          ),
        ),
      ),
    );
  }
}

/// The app's main call to action: a full-width ink pill.
class PrimaryButton extends StatelessWidget {
  const PrimaryButton({
    super.key,
    required this.child,
    required this.onTap,
    this.color,
    this.foreground,
  });

  final Widget child;
  final VoidCallback? onTap;
  final Color? color;
  final Color? foreground;

  @override
  Widget build(BuildContext context) {
    final p = Palette.of(context);
    final fg = foreground ?? p.canvas;
    return Pressable(
      onTap: onTap,
      child: AnimatedContainer(
        duration: Motion.of(context, Motion.slow),
        curve: Motion.ease,
        height: 56,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: color ?? p.ink,
          borderRadius: BorderRadius.circular(Radii.pill),
        ),
        child: IconTheme.merge(
          data: IconThemeData(color: fg, size: 18),
          child: DefaultTextStyle.merge(
            style: Theme.of(context).textTheme.labelLarge!.copyWith(color: fg),
            child: child,
          ),
        ),
      ),
    );
  }
}

/// A text-only action.
class QuietButton extends StatelessWidget {
  const QuietButton({super.key, required this.label, required this.onTap});
  final String label;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) => Pressable(
    onTap: onTap,
    child: Padding(
      padding: const EdgeInsets.symmetric(horizontal: Gap.m, vertical: Gap.m),
      child: Text(
        label,
        style: Theme.of(
          context,
        ).textTheme.labelLarge!.copyWith(color: Palette.of(context).muted),
      ),
    ),
  );
}

Future<Color?> showCustomColorPicker(BuildContext context, Color initial) {
  return showModalBottomSheet<Color>(
    context: context,
    builder: (context) => _HsvPicker(initial: HSVColor.fromColor(initial)),
  );
}

class _HsvPicker extends StatefulWidget {
  const _HsvPicker({required this.initial});
  final HSVColor initial;

  @override
  State<_HsvPicker> createState() => _HsvPickerState();
}

class _HsvPickerState extends State<_HsvPicker> {
  late HSVColor _hsv = widget.initial.withAlpha(1);

  @override
  Widget build(BuildContext context) {
    final color = _hsv.toColor();
    return Padding(
      padding: const EdgeInsets.fromLTRB(Gap.xl, 0, Gap.xl, Gap.xl),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          AnimatedContainer(
            duration: Motion.of(context, Motion.quick),
            height: 64,
            decoration: BoxDecoration(
              color: color,
              borderRadius: BorderRadius.circular(Radii.m),
            ),
          ),
          const SizedBox(height: Gap.m),
          LabeledSlider(
            label: 'Hue',
            value: _hsv.hue,
            min: 0,
            max: 360,
            steps: 24,
            display: '${_hsv.hue.round()}°',
            onChanged: (v) => setState(() => _hsv = _hsv.withHue(v)),
          ),
          LabeledSlider(
            label: 'Chroma',
            value: _hsv.saturation,
            min: 0,
            max: 1,
            display: '${(_hsv.saturation * 100).round()}%',
            onChanged: (v) => setState(() => _hsv = _hsv.withSaturation(v)),
          ),
          LabeledSlider(
            label: 'Light',
            value: _hsv.value,
            min: 0,
            max: 1,
            display: '${(_hsv.value * 100).round()}%',
            onChanged: (v) => setState(() => _hsv = _hsv.withValue(v)),
          ),
          const SizedBox(height: Gap.m),
          PrimaryButton(
            onTap: () => Navigator.pop(context, color),
            child: const Text('Use colour'),
          ),
        ],
      ),
    );
  }
}
