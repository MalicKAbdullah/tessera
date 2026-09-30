import 'package:flutter/material.dart';
import 'package:flutter/physics.dart';
import 'package:flutter/services.dart';

import '../design/tokens.dart';

/// A tappable surface: it sinks to [Motion.pressScale] under the finger on a
/// stiff spring and clicks on release. Replaces ink splashes app-wide.
class Pressable extends StatefulWidget {
  const Pressable({
    super.key,
    required this.child,
    required this.onTap,
    this.scale = Motion.pressScale,
    this.haptic = true,
    this.semanticLabel,
  });

  final Widget child;

  /// Null disables the surface.
  final VoidCallback? onTap;
  final double scale;
  final bool haptic;
  final String? semanticLabel;

  @override
  State<Pressable> createState() => _PressableState();
}

class _PressableState extends State<Pressable>
    with SingleTickerProviderStateMixin {
  late final _scale = AnimationController.unbounded(vsync: this, value: 1);

  @override
  void dispose() {
    _scale.dispose();
    super.dispose();
  }

  void _springTo(double target) {
    if (Motion.reduced(context)) return;
    _scale.animateWith(
      SpringSimulation(Motion.press, _scale.value, target, _scale.velocity),
    );
  }

  @override
  Widget build(BuildContext context) {
    final enabled = widget.onTap != null;
    return Semantics(
      button: true,
      enabled: enabled,
      label: widget.semanticLabel,
      child: GestureDetector(
        behavior: HitTestBehavior.opaque,
        onTapDown: enabled ? (_) => _springTo(widget.scale) : null,
        onTapCancel: enabled ? () => _springTo(1) : null,
        onTapUp: enabled ? (_) => _springTo(1) : null,
        onTap: enabled
            ? () {
                if (widget.haptic) HapticFeedback.selectionClick();
                widget.onTap!();
              }
            : null,
        child: ScaleTransition(scale: _scale, child: widget.child),
      ),
    );
  }
}

/// Staggered entrance with depth: rises, un-tilts and fades in along
/// [animation], offset by [index] (capped so long lists do not queue up).
class Entrance extends StatefulWidget {
  const Entrance({
    super.key,
    required this.animation,
    required this.index,
    required this.child,
    this.depth = true,
  });

  final Animation<double> animation;
  final int index;
  final Widget child;

  /// Adds a perspective tilt that flattens as the child lands.
  final bool depth;

  @override
  State<Entrance> createState() => _EntranceState();
}

class _EntranceState extends State<Entrance> {
  static const _step = 0.07;
  static const _span = 0.55;
  static const _maxStaggered = 6;

  // CurvedAnimation subscribes to its parent on construction, so it is
  // created once per parent and index, never per build.
  late CurvedAnimation _t = _curve();

  CurvedAnimation _curve() {
    final start = (widget.index.clamp(0, _maxStaggered) * _step).toDouble();
    return CurvedAnimation(
      parent: widget.animation,
      curve: Interval(start, (start + _span).clamp(0, 1), curve: Motion.enter),
    );
  }

  @override
  void didUpdateWidget(Entrance old) {
    super.didUpdateWidget(old);
    if (old.animation != widget.animation || old.index != widget.index) {
      _t.dispose();
      _t = _curve();
    }
  }

  @override
  void dispose() {
    _t.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => FadeTransition(
    opacity: _t,
    child: AnimatedBuilder(
      animation: _t,
      child: widget.child,
      builder: (context, child) {
        final r = 1 - _t.value;
        final m = Matrix4.identity()
          ..setEntry(3, 2, 0.0012)
          ..translateByDouble(0, 28 * r, 0, 1);
        if (widget.depth) m.rotateX(0.22 * r);
        m.scaleByDouble(0.94 + 0.06 * _t.value, 0.94 + 0.06 * _t.value, 1, 1);
        return Transform(
          alignment: Alignment.center,
          transform: m,
          child: child,
        );
      },
    ),
  );
}

/// Tilts and shifts [child] by where it sits in the enclosing scroll view:
/// cards lean back as they near the top edge and forward near the bottom,
/// and [shift] moves the child against the scroll for parallax. Runs in the
/// paint phase, so scrolling repaints without rebuilding.
class ScrollDepth extends StatelessWidget {
  const ScrollDepth({
    super.key,
    required this.child,
    this.tilt = 0,
    this.shift = 0,
  });

  final Widget child;

  /// Radians at the viewport edges.
  final double tilt;

  /// Logical pixels at the viewport edges.
  final double shift;

  @override
  Widget build(BuildContext context) {
    if (Motion.reduced(context)) return child;
    return Flow(
      delegate: _DepthDelegate(
        scrollable: Scrollable.of(context),
        item: context,
        tilt: tilt,
        shift: shift,
      ),
      children: [child],
    );
  }
}

class _DepthDelegate extends FlowDelegate {
  _DepthDelegate({
    required this.scrollable,
    required this.item,
    required this.tilt,
    required this.shift,
  }) : super(repaint: scrollable.position);

  final ScrollableState scrollable;
  final BuildContext item;
  final double tilt;
  final double shift;

  @override
  void paintChildren(FlowPaintingContext context) {
    final viewport = scrollable.context.findRenderObject()! as RenderBox;
    final box = item.findRenderObject()! as RenderBox;
    final centre = box.localToGlobal(
      box.size.center(Offset.zero),
      ancestor: viewport,
    );
    final vertical = scrollable.position.axis == Axis.vertical;
    final extent = vertical ? viewport.size.height : viewport.size.width;
    final t = (((vertical ? centre.dy : centre.dx) / extent) * 2 - 1).clamp(
      -1.0,
      1.0,
    );
    final size = context.size;
    final m = Matrix4.identity()
      ..translateByDouble(size.width / 2, size.height / 2, 0, 1)
      ..setEntry(3, 2, 0.0009);
    if (vertical) {
      m
        ..rotateX(-t * tilt)
        ..translateByDouble(0, t * shift, 0, 1);
    } else {
      m
        ..rotateY(t * tilt)
        ..translateByDouble(t * shift, 0, 0, 1);
    }
    m.translateByDouble(-size.width / 2, -size.height / 2, 0, 1);
    context.paintChild(0, transform: m);
  }

  @override
  bool shouldRepaint(_DepthDelegate old) =>
      old.scrollable != scrollable ||
      old.item != item ||
      old.tilt != tilt ||
      old.shift != shift;
}

/// Answers every change of [trigger] with a spring: the child dips slightly
/// and settles with one small overshoot, so edits feel physical.
class SpringResponse extends StatefulWidget {
  const SpringResponse({
    super.key,
    required this.trigger,
    required this.child,
    this.impulse = 0.45,
  });

  final Object trigger;
  final Widget child;

  /// Initial downward scale velocity per change.
  final double impulse;

  @override
  State<SpringResponse> createState() => _SpringResponseState();
}

class _SpringResponseState extends State<SpringResponse>
    with SingleTickerProviderStateMixin {
  late final _scale = AnimationController.unbounded(vsync: this, value: 1);

  @override
  void didUpdateWidget(SpringResponse old) {
    super.didUpdateWidget(old);
    if (old.trigger != widget.trigger && !Motion.reduced(context)) {
      _scale.animateWith(
        SpringSimulation(Motion.stage, _scale.value, 1, -widget.impulse),
      );
    }
  }

  @override
  void dispose() {
    _scale.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) =>
      ScaleTransition(scale: _scale, child: widget.child);
}
