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

/// Staggered entrance: rises and fades in along [animation], offset by
/// [index] (capped so long lists do not queue up).
class Entrance extends StatefulWidget {
  const Entrance({
    super.key,
    required this.animation,
    required this.index,
    required this.child,
  });

  final Animation<double> animation;
  final int index;
  final Widget child;

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
    child: SlideTransition(
      position: Tween(
        begin: const Offset(0, 0.06),
        end: Offset.zero,
      ).animate(_t),
      child: widget.child,
    ),
  );
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
