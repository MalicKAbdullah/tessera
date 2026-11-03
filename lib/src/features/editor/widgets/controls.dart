import 'package:core_theme/core_theme.dart';
import 'package:core_ui/core_ui.dart';
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
    final c = SuiteColors.of(context);
    return Container(
      margin: const EdgeInsets.only(top: Gap.xl),
      padding: const EdgeInsets.only(top: Gap.l),
      decoration: BoxDecoration(
        border: Border(top: BorderSide(color: c.hairline)),
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

/// A slider with its value as a tabular readout. It clicks once per [steps]th of
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
    final c = SuiteColors.of(context);
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
                style: AppTextStyles.numberSmall.copyWith(
                  color: _dragging ? c.accent : c.textSecondary,
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

/// Segmented control drawn like the suite's `AppSegmentedControl` (recessed
/// track, raised thumb that settles on the selection), except that a label
/// too long for its segment fades out instead of overflowing: design option
/// names vary in length and some sets fill five segments.
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
    final c = SuiteColors.of(context);
    final dark = Theme.of(context).brightness == Brightness.dark;
    final keys = options.keys.toList();
    final index = keys.indexOf(selected);
    final n = keys.length;
    return Container(
      height: 40,
      padding: const EdgeInsets.all(3),
      decoration: ShapeDecoration(
        color: c.surfaceAlt,
        shape: const StadiumBorder(),
      ),
      child: Stack(
        children: [
          if (index >= 0)
            AnimatedAlign(
              duration: Motion.of(context, Motion.base),
              curve: AppMotion.settle,
              alignment: Alignment(n == 1 ? 0 : -1 + 2 * index / (n - 1), 0),
              child: FractionallySizedBox(
                widthFactor: 1 / n,
                heightFactor: 1,
                child: DecoratedBox(
                  decoration: ShapeDecoration(
                    color: dark ? AppColors.gray700 : c.surface,
                    shape: StadiumBorder(
                      side: BorderSide(
                        color: dark ? Colors.transparent : c.border,
                        width: 0.5,
                      ),
                    ),
                  ),
                ),
              ),
            ),
          Row(
            children: [
              for (final k in keys)
                Expanded(
                  child: Semantics(
                    button: true,
                    selected: k == selected,
                    child: GestureDetector(
                      behavior: HitTestBehavior.opaque,
                      onTap: k == selected
                          ? null
                          : () {
                              HapticFeedback.selectionClick();
                              onSelected(k);
                            },
                      child: Center(
                        child: Padding(
                          padding: const EdgeInsets.symmetric(
                            horizontal: Gap.xs,
                          ),
                          child: AnimatedDefaultTextStyle(
                            duration: Motion.of(context, Motion.quick),
                            style: AppTextStyles.labelStrong.copyWith(
                              color: k == selected
                                  ? c.textPrimary
                                  : c.textSecondary,
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
  Widget build(BuildContext context) => AppChip(
    label: label,
    selected: selected,
    onTap: () {
      HapticFeedback.selectionClick();
      onTap();
    },
  );
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
    final c = SuiteColors.of(context);
    return Pressable(
      scale: 0.9,
      onTap: onTap,
      semanticLabel: color == null ? 'Custom colour' : 'Colour',
      child: AnimatedContainer(
        duration: Motion.of(context, Motion.base),
        curve: AppMotion.settle,
        width: 34,
        height: 34,
        padding: EdgeInsets.all(selected ? 4 : 0),
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          border: Border.all(
            color: selected ? c.accent : c.border,
            width: selected ? 2 : 1,
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

/// The editor's main call to action: the suite's primary pill, with a child
/// that can change in place (labels, progress, a check).
class PrimaryButton extends StatelessWidget {
  const PrimaryButton({super.key, required this.child, required this.onTap});

  final Widget child;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final c = SuiteColors.of(context);
    return Pressable(
      onTap: onTap,
      child: Container(
        height: AppSpacing.buttonHeight,
        alignment: Alignment.center,
        decoration: ShapeDecoration(
          color: c.accent,
          shape: const StadiumBorder(),
        ),
        child: IconTheme.merge(
          data: IconThemeData(color: c.onAccent, size: 18),
          child: DefaultTextStyle.merge(
            style: AppTextStyles.button.copyWith(color: c.onAccent),
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
  Widget build(BuildContext context) =>
      TextButton(onPressed: onTap, child: Text(label));
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
              borderRadius: BorderRadius.circular(Radii.s),
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
          VaultButton(
            label: 'Use colour',
            onPressed: () => Navigator.pop(context, color),
          ),
        ],
      ),
    );
  }
}
