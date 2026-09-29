import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../../core/theme.dart';
import '../models/widget_content.dart';
import '../models/widget_kind.dart';
import '../models/widget_style.dart';
import '../providers/widget_providers.dart';

/// The text a widget shows, in the same three slots the native layout has.
class TileText {
  const TileText({this.label, required this.value, this.sub, this.level});
  final String? label;
  final String value;
  final String? sub;

  /// 0..1 fill for the hairline meter drawn along the bottom edge.
  final double? level;
}

TileText tileTextFor(WidgetKind kind, WidgetRef ref) {
  final now = ref.watch(nowProvider).valueOrNull ?? DateTime.now();
  switch (kind) {
    case WidgetKind.clock:
      return TileText(
        label: DateFormat('EEEE').format(now).toUpperCase(),
        value: DateFormat('HH:mm').format(now),
        sub: DateFormat('d MMMM').format(now),
      );
    case WidgetKind.calendar:
      return TileText(
        label: DateFormat('MMMM').format(now).toUpperCase(),
        value: '${now.day}',
        sub: DateFormat('EEEE').format(now),
      );
    case WidgetKind.battery:
      final reading = ref.watch(batteryProvider).valueOrNull;
      return TileText(
        label: reading?.charging == true ? 'CHARGING' : 'BATTERY',
        value: reading == null ? '—' : '${reading.level}%',
        sub: reading?.charging == true ? 'Plugged in' : 'On battery',
        level: reading == null ? null : reading.level / 100,
      );
    case WidgetKind.weather:
      final content = ref.watch(contentProvider);
      final weather = ref.watch(weatherProvider).valueOrNull;
      return TileText(
        label: (content.city?.name ?? 'Set a city').toUpperCase(),
        value: weather?.temperatureLabel ?? '—',
        sub: weather?.condition ?? 'Tap to choose where',
      );
    case WidgetKind.countdown:
      final content = ref.watch(contentProvider);
      final days = daysUntil(content.effectiveCountdownDate(now), now);
      return TileText(
        label: content.countdownTitle.toUpperCase(),
        value: '${days.abs()}',
        sub: countdownCaption(days),
      );
    case WidgetKind.note:
      final content = ref.watch(contentProvider);
      return TileText(
        value: content.note,
        sub: content.noteAuthor.isEmpty ? null : '— ${content.noteAuthor}',
      );
  }
}

String countdownCaption(int days) => switch (days) {
      0 => 'is today',
      1 => 'day to go',
      -1 => 'day ago',
      > 1 => 'days to go',
      _ => 'days ago',
    };

/// Flutter rendition of the native tile. Every property is implicitly
/// animated so edits glide instead of snapping.
class WidgetPreview extends ConsumerWidget {
  const WidgetPreview({super.key, required this.kind, this.width = 320});

  final WidgetKind kind;
  final double width;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final style = ref.watch(styleProvider(kind));
    final text = tileTextFor(kind, ref);
    return Hero(
      tag: 'tile-${kind.id}',
      child: SizedBox(
        width: width,
        height: width * 0.5,
        child: TileSurface(style: style, text: text, isNote: kind == WidgetKind.note),
      ),
    );
  }
}

class TileSurface extends StatelessWidget {
  const TileSurface({
    super.key,
    required this.style,
    required this.text,
    this.isNote = false,
  });

  final WidgetStyle style;
  final TileText text;
  final bool isNote;

  @override
  Widget build(BuildContext context) {
    const d = TesseraTheme.motion;
    const c = TesseraTheme.ease;
    final s = style.scale;
    return AnimatedContainer(
      duration: d,
      curve: c,
      clipBehavior: Clip.antiAlias,
      decoration: BoxDecoration(
        color: style.surface,
        borderRadius: BorderRadius.circular(style.radius),
      ),
      child: Stack(
        children: [
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
            child: Material(
              type: MaterialType.transparency,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  if (text.label != null)
                    AnimatedDefaultTextStyle(
                      duration: d,
                      curve: c,
                      style: TextStyle(
                        color: style.accent,
                        fontSize: 11 * s,
                        letterSpacing: 1.6,
                        fontWeight: FontWeight.w500,
                      ),
                      child: Text(text.label!, maxLines: 1),
                    ),
                  Flexible(
                    child: AnimatedDefaultTextStyle(
                      duration: d,
                      curve: c,
                      style: TextStyle(
                        color: style.text,
                        fontSize: (isNote ? 20 : 44) * s,
                        height: isNote ? 1.25 : 1.1,
                        fontWeight: style.weight.fontWeight,
                        letterSpacing: isNote ? 0 : -1,
                      ),
                      child: AnimatedSwitcher(
                        duration: const Duration(milliseconds: 260),
                        child: Text(
                          text.value,
                          key: ValueKey(text.value),
                          maxLines: isNote ? 3 : 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                    ),
                  ),
                  if (text.sub != null)
                    AnimatedDefaultTextStyle(
                      duration: d,
                      curve: c,
                      style: TextStyle(
                        color: style.text.withValues(alpha: 0.66),
                        fontSize: 13 * s,
                        fontWeight: FontWeight.w400,
                      ),
                      child: Text(text.sub!, maxLines: 1),
                    ),
                ],
              ),
            ),
          ),
          if (text.level != null)
            Positioned(
              left: 20,
              right: 20,
              bottom: 12,
              child: _Meter(level: text.level!, style: style),
            ),
        ],
      ),
    );
  }
}

class _Meter extends StatelessWidget {
  const _Meter({required this.level, required this.style});
  final double level;
  final WidgetStyle style;

  @override
  Widget build(BuildContext context) => ClipRRect(
        borderRadius: BorderRadius.circular(2),
        child: SizedBox(
          height: 3,
          child: Stack(children: [
            Positioned.fill(
                child: ColoredBox(color: style.text.withValues(alpha: 0.12))),
            TweenAnimationBuilder<double>(
              tween: Tween(end: level),
              duration: TesseraTheme.motion,
              curve: TesseraTheme.ease,
              builder: (context, v, _) => FractionallySizedBox(
                widthFactor: v,
                child: ColoredBox(color: style.accent),
              ),
            ),
          ]),
        ),
      );
}

/// Soft wallpaper stand-in so opacity and radius read as they will on a
/// real home screen.
class Backdrop extends StatelessWidget {
  const Backdrop({super.key, required this.child, this.radius = 28});
  final Widget child;
  final double radius;

  @override
  Widget build(BuildContext context) {
    final dark = Theme.of(context).brightness == Brightness.dark;
    return DecoratedBox(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(radius),
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: dark
              ? const [Color(0xFF23262E), Color(0xFF3A3340), Color(0xFF2B3431)]
              : const [Color(0xFFD9D4CC), Color(0xFFC9CDD6), Color(0xFFD8D0C8)],
        ),
      ),
      child: child,
    );
  }
}
