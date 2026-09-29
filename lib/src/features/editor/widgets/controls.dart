import 'package:flutter/material.dart';

import '../../../core/theme.dart';

class Section extends StatelessWidget {
  const Section({super.key, required this.title, required this.child});
  final String title;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.only(top: 24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title.toUpperCase(),
              style: theme.textTheme.labelSmall?.copyWith(
                  letterSpacing: 1.4,
                  color: theme.colorScheme.onSurfaceVariant)),
          const SizedBox(height: 10),
          child,
        ],
      ),
    );
  }
}

class LabeledSlider extends StatelessWidget {
  const LabeledSlider({
    super.key,
    required this.label,
    required this.value,
    required this.min,
    required this.max,
    required this.display,
    required this.onChanged,
  });

  final String label;
  final double value;
  final double min;
  final double max;
  final String display;
  final ValueChanged<double> onChanged;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Row(
      children: [
        SizedBox(width: 84, child: Text(label, style: theme.textTheme.bodyMedium)),
        Expanded(
          child: Slider(value: value, min: min, max: max, onChanged: onChanged),
        ),
        SizedBox(
          width: 44,
          child: Text(display,
              textAlign: TextAlign.end,
              style: theme.textTheme.bodySmall?.copyWith(
                  color: theme.colorScheme.onSurfaceVariant,
                  fontFeatures: const [FontFeature.tabularFigures()])),
        ),
      ],
    );
  }
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
          ColorDot(color: c, selected: c == selected, onTap: () => onSelected(c)),
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
  const ColorDot({super.key, required this.color, required this.selected, required this.onTap});

  /// Null draws the "custom colour" dot.
  final Color? color;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final ring = Theme.of(context).colorScheme.onSurface;
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 240),
        curve: TesseraTheme.ease,
        width: 34,
        height: 34,
        padding: EdgeInsets.all(selected ? 3 : 0),
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          border: Border.all(
              color: selected ? ring : ring.withValues(alpha: 0.12),
              width: selected ? 1.5 : 1),
        ),
        child: DecoratedBox(
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            color: color,
            gradient: color == null
                ? const SweepGradient(colors: [
                    Color(0xFFE0A6A6),
                    Color(0xFFE3D39B),
                    Color(0xFFA6D1B0),
                    Color(0xFF9FB8DC),
                    Color(0xFFCDA6DA),
                    Color(0xFFE0A6A6),
                  ])
                : null,
          ),
        ),
      ),
    );
  }
}

Future<Color?> showCustomColorPicker(BuildContext context, Color initial) {
  return showModalBottomSheet<Color>(
    context: context,
    showDragHandle: true,
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
      padding: const EdgeInsets.fromLTRB(24, 0, 24, 24),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          AnimatedContainer(
            duration: const Duration(milliseconds: 200),
            height: 56,
            decoration: BoxDecoration(
                color: color, borderRadius: BorderRadius.circular(16)),
          ),
          const SizedBox(height: 12),
          LabeledSlider(
            label: 'Hue',
            value: _hsv.hue,
            min: 0,
            max: 360,
            display: '${_hsv.hue.round()}°',
            onChanged: (v) => setState(() => _hsv = _hsv.withHue(v)),
          ),
          LabeledSlider(
            label: 'Saturation',
            value: _hsv.saturation,
            min: 0,
            max: 1,
            display: '${(_hsv.saturation * 100).round()}%',
            onChanged: (v) => setState(() => _hsv = _hsv.withSaturation(v)),
          ),
          LabeledSlider(
            label: 'Brightness',
            value: _hsv.value,
            min: 0,
            max: 1,
            display: '${(_hsv.value * 100).round()}%',
            onChanged: (v) => setState(() => _hsv = _hsv.withValue(v)),
          ),
          const SizedBox(height: 8),
          SizedBox(
            width: double.infinity,
            child: FilledButton(
              onPressed: () => Navigator.pop(context, color),
              child: const Text('Use colour'),
            ),
          ),
        ],
      ),
    );
  }
}
