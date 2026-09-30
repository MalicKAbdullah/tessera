import 'package:flutter/material.dart';

import '../../core/design/tokens.dart';
import '../../core/motion/motion.dart';
import '../../core/widgets/mosaic_mark.dart';

/// First-launch sequence over the gallery: tiles assemble into the mark, the
/// wordmark rises, then the veil lifts while the mark flies into the
/// gallery header at [markTarget]. Tapping anywhere skips to the exit.
class IntroOverlay extends StatefulWidget {
  const IntroOverlay({
    super.key,
    required this.markTarget,
    required this.onReveal,
    required this.onDone,
  });

  /// The header mark the intro mark lands on.
  final GlobalKey markTarget;

  /// The veil starts lifting; the gallery should begin its entrance.
  final VoidCallback onReveal;

  /// The sequence finished or was skipped; remove the overlay.
  final VoidCallback onDone;

  @override
  State<IntroOverlay> createState() => _IntroOverlayState();
}

class _IntroOverlayState extends State<IntroOverlay>
    with SingleTickerProviderStateMixin {
  static const _exitAt = 0.78;
  static const _markSize = 88.0;

  late final _c = AnimationController(vsync: this);
  bool _revealed = false;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_c.isAnimating || _c.isCompleted) return;
    _c
      ..duration = Duration(milliseconds: Motion.reduced(context) ? 1400 : 2700)
      ..addListener(_onTick)
      ..addStatusListener((s) {
        if (s == AnimationStatus.completed) widget.onDone();
      })
      ..forward();
  }

  void _onTick() {
    if (!_revealed && _c.value >= _exitAt) {
      _revealed = true;
      widget.onReveal();
    }
  }

  void _skip() {
    if (_c.value >= _exitAt) return;
    _c
      ..value = _exitAt
      ..animateTo(1, duration: Motion.of(context, Motion.slow));
  }

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  double _phase(double from, double to) =>
      ((_c.value - from) / (to - from)).clamp(0.0, 1.0);

  @override
  Widget build(BuildContext context) {
    final p = Palette.of(context);
    final theme = Theme.of(context);
    final reduced = Motion.reduced(context);
    return GestureDetector(
      key: const Key('intro'),
      behavior: HitTestBehavior.opaque,
      onTap: _skip,
      child: LayoutBuilder(
        builder: (context, box) => AnimatedBuilder(
          animation: _c,
          builder: (context, _) {
            final exit = Motion.ease.transform(_phase(_exitAt, 1));
            final words = Motion.enter.transform(_phase(0.34, 0.62));
            final centre = Rect.fromCenter(
              center: box.biggest.center(Offset.zero).translate(0, -40),
              width: _markSize,
              height: _markSize,
            );
            final target = _targetRect(context) ?? centre;
            final mark = reduced ? centre : Rect.lerp(centre, target, exit)!;
            return Stack(
              children: [
                Positioned.fill(
                  child: ColoredBox(
                    color: p.canvas.withValues(alpha: 1 - exit),
                  ),
                ),
                Positioned.fromRect(
                  rect: mark,
                  child: Opacity(
                    opacity: reduced ? 1 - exit : 1,
                    child: MosaicMark(
                      size: mark.width,
                      assemble: reduced ? 1 : _phase(0, 0.52),
                    ),
                  ),
                ),
                Positioned(
                  left: 0,
                  right: 0,
                  top: centre.bottom + Gap.xl,
                  child: Opacity(
                    opacity: words * (1 - exit),
                    child: Transform.translate(
                      offset: Offset(0, reduced ? 0 : 16 * (1 - words)),
                      child: Column(
                        children: [
                          Text('Tessera', style: theme.textTheme.displayMedium),
                          const SizedBox(height: Gap.s),
                          Text(
                            'WIDGETS, QUIETLY YOURS',
                            style: theme.textTheme.labelSmall,
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
                Positioned(
                  top: MediaQuery.paddingOf(context).top + Gap.s,
                  right: Gap.s,
                  child: Opacity(
                    opacity: 1 - exit,
                    child: Pressable(
                      onTap: _skip,
                      semanticLabel: 'Skip intro',
                      child: Padding(
                        padding: const EdgeInsets.all(Gap.m),
                        child: Text('SKIP', style: theme.textTheme.labelSmall),
                      ),
                    ),
                  ),
                ),
              ],
            );
          },
        ),
      ),
    );
  }

  /// Where the header mark sits, in this overlay's coordinates.
  Rect? _targetRect(BuildContext context) {
    final target = widget.markTarget.currentContext?.findRenderObject();
    final self = context.findRenderObject();
    if (target is! RenderBox || self is! RenderBox || !target.hasSize) {
      return null;
    }
    final origin = target.localToGlobal(Offset.zero, ancestor: self);
    return origin & target.size;
  }
}
