import 'dart:async';
import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/design/tokens.dart';
import '../../widgets/models/catalog.dart';
import '../../widgets/providers/widget_providers.dart';
import 'controls.dart';

/// The editor's bottom action: add to the home screen (draft), place
/// (configuring), or a live "applied" note (placed widget).
class PrimaryAction extends ConsumerWidget {
  const PrimaryAction({super.key, required this.target, required this.size});
  final EditTarget target;
  final SizeInfo size;

  @override
  Widget build(BuildContext context, WidgetRef ref) => switch (target) {
    PlacedTarget(configuring: true) => PrimaryButton(
      onTap: () => ref
          .read(engineProvider)
          .finishConfigure(target.design, ref.read(styleProvider(target))),
      child: const Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.check),
          SizedBox(width: Gap.s),
          Text('Place widget'),
        ],
      ),
    ),
    PlacedTarget() => _AppliedNote(target: target),
    final DraftTarget draft => PinAction(target: draft, size: size),
  };
}

/// "Applying…" while edits settle into the placed widget, then "Applied".
class _AppliedNote extends ConsumerStatefulWidget {
  const _AppliedNote({required this.target});
  final EditTarget target;

  @override
  ConsumerState<_AppliedNote> createState() => _AppliedNoteState();
}

class _AppliedNoteState extends ConsumerState<_AppliedNote> {
  /// Slightly longer than StyleNotifier's bind debounce.
  static const _settle = Duration(milliseconds: 450);

  Timer? _timer;
  bool _applying = false;

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    ref.listen(styleProvider(widget.target), (_, _) {
      _timer?.cancel();
      _timer = Timer(_settle, () => setState(() => _applying = false));
      if (!_applying) setState(() => _applying = true);
    });
    final p = Palette.of(context);
    final theme = Theme.of(context);
    return SizedBox(
      height: 56,
      child: Center(
        child: AnimatedSwitcher(
          duration: Motion.of(context, Motion.base),
          child: Row(
            key: ValueKey(_applying),
            mainAxisSize: MainAxisSize.min,
            children: [
              AnimatedContainer(
                duration: Motion.of(context, Motion.base),
                width: 6,
                height: 6,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: _applying ? p.accent : p.ink,
                ),
              ),
              const SizedBox(width: Gap.s),
              Text(
                _applying
                    ? 'APPLYING TO YOUR WIDGET'
                    : 'LIVE ON YOUR HOME SCREEN',
                style: theme.textTheme.labelSmall,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Where a pin request stands.
enum PinState {
  /// Ready to ask.
  idle,

  /// Waiting for the engine to hand the request to the launcher.
  asking,

  /// The launcher is showing its confirmation prompt.
  confirming,

  /// The widget arrived on the home screen.
  placed,

  /// The launcher refused the request.
  declined,
}

/// Adding a draft to the home screen. The launcher confirms asynchronously:
/// the widget counts as placed only once the pin callback has bound a new
/// widget of this design, which the engine reports as a data change.
class PinAction extends ConsumerStatefulWidget {
  const PinAction({super.key, required this.target, required this.size});
  final DraftTarget target;
  final SizeInfo size;

  @override
  ConsumerState<PinAction> createState() => PinActionState();
}

class PinActionState extends ConsumerState<PinAction> {
  late final Future<bool> _canPin = ref.read(engineProvider).canPin();
  PinState state = PinState.idle;
  Set<int> _before = const {};

  Future<void> _pin() async {
    _before = {
      for (final p in ref.read(placedProvider).valueOrNull ?? const []) p.id,
    };
    setState(() => state = PinState.asking);
    final accepted = await ref
        .read(engineProvider)
        .pin(
          widget.target.design,
          ref.read(styleProvider(widget.target)),
          widget.size,
        );
    if (!mounted) return;
    setState(() => state = accepted ? PinState.confirming : PinState.declined);
    if (!accepted) HapticFeedback.heavyImpact();
  }

  void _arrived() {
    setState(() => state = PinState.placed);
    HapticFeedback.mediumImpact();
    Future.delayed(const Duration(milliseconds: 1900), () {
      if (mounted) context.go('/');
    });
  }

  @override
  Widget build(BuildContext context) {
    ref.listen(placedProvider, (_, next) {
      if (state != PinState.confirming) return;
      final arrived = (next.valueOrNull ?? const []).any(
        (p) =>
            !_before.contains(p.id) &&
            p.bound &&
            p.design == widget.target.design,
      );
      if (arrived) _arrived();
    });
    return FutureBuilder<bool>(
      future: _canPin,
      builder: (context, snap) => AnimatedSwitcher(
        duration: Motion.of(context, Motion.base),
        child: switch (snap.data) {
          null => const SizedBox(height: 56),
          false => const _Unsupported(),
          true => _pinBody(context),
        },
      ),
    );
  }

  Widget _pinBody(BuildContext context) {
    final p = Palette.of(context);
    final theme = Theme.of(context);
    final label = switch (state) {
      PinState.idle => Text('Add ${widget.size.label} to home screen'),
      PinState.asking => SizedBox(
        width: 18,
        height: 18,
        child: CircularProgressIndicator(strokeWidth: 1.6, color: p.canvas),
      ),
      PinState.confirming => const Text('Confirm in the launcher prompt'),
      PinState.placed => const Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.check),
          SizedBox(width: Gap.s),
          Text('On your home screen'),
        ],
      ),
      PinState.declined => const Text('Try again'),
    };
    final note = switch (state) {
      PinState.confirming => 'Nothing appeared? Tap to ask again.',
      PinState.declined =>
        'Your launcher declined. You can also long-press the home screen, '
            'choose Widgets, then Tessera.',
      _ => null,
    };
    return Column(
      key: const ValueKey('pin'),
      mainAxisSize: MainAxisSize.min,
      children: [
        AnimatedSize(
          duration: Motion.of(context, Motion.base),
          curve: Motion.ease,
          child: note == null
              ? const SizedBox(width: double.infinity)
              : Padding(
                  padding: const EdgeInsets.only(bottom: Gap.m),
                  child: Text(
                    note,
                    key: ValueKey(note),
                    textAlign: TextAlign.center,
                    style: theme.textTheme.bodySmall!.copyWith(
                      color: state == PinState.declined ? p.accent : p.muted,
                    ),
                  ),
                ),
        ),
        Stack(
          clipBehavior: Clip.none,
          alignment: Alignment.center,
          children: [
            if (state == PinState.placed)
              Positioned.fill(
                child: IgnorePointer(
                  child: TileBurst(key: const ValueKey('burst')),
                ),
              ),
            PrimaryButton(
              color: state == PinState.placed ? p.accent : null,
              foreground: state == PinState.placed ? p.onAccent : null,
              onTap: switch (state) {
                PinState.idle ||
                PinState.confirming ||
                PinState.declined => _pin,
                PinState.asking || PinState.placed => null,
              },
              child: AnimatedSwitcher(
                duration: Motion.of(context, Motion.base),
                transitionBuilder: (child, a) => FadeTransition(
                  opacity: a,
                  child: ScaleTransition(
                    scale: Tween(begin: 0.85, end: 1.0).animate(a),
                    child: child,
                  ),
                ),
                child: KeyedSubtree(key: ValueKey(state), child: label),
              ),
            ),
          ],
        ),
      ],
    );
  }
}

class _Unsupported extends StatelessWidget {
  const _Unsupported();

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.symmetric(vertical: Gap.m),
    child: Text(
      'To add it, long-press your home screen, choose Widgets, then Tessera.',
      textAlign: TextAlign.center,
      style: Theme.of(context).textTheme.bodySmall,
    ),
  );
}

/// A single, quiet burst: mosaic tiles spring out of the button, turn and
/// fade. Plays once.
class TileBurst extends StatefulWidget {
  const TileBurst({super.key});

  @override
  State<TileBurst> createState() => _TileBurstState();
}

class _TileBurstState extends State<TileBurst>
    with SingleTickerProviderStateMixin {
  late final _c = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 1100),
  );

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (!Motion.reduced(context) && !_c.isAnimating && !_c.isCompleted) {
      _c.forward();
    }
  }

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (Motion.reduced(context)) return const SizedBox.shrink();
    final p = Palette.of(context);
    return AnimatedBuilder(
      animation: _c,
      builder: (context, _) =>
          CustomPaint(painter: _BurstPainter(_c.value, p.accent, p.ink)),
    );
  }
}

class _BurstPainter extends CustomPainter {
  _BurstPainter(this.t, this.accent, this.ink);
  final double t;
  final Color accent;
  final Color ink;

  static const _count = 14;

  @override
  void paint(Canvas canvas, Size size) {
    if (t == 0 || t == 1) return;
    final centre = size.center(Offset.zero);
    final travel = Curves.easeOutCubic.transform(t);
    final fade = 1 - Curves.easeIn.transform(t);
    final paint = Paint();
    for (var i = 0; i < _count; i++) {
      final angle = i / _count * math.pi * 2 + 0.3;
      final reach = (i.isEven ? 0.62 : 0.48) * size.width;
      final at =
          centre +
          Offset(
            math.cos(angle) * reach * travel,
            math.sin(angle) * reach * travel * 0.9 - 40 * travel,
          );
      final side = (i % 3 == 0 ? 9.0 : 6.0) * (1 - 0.4 * t);
      paint.color = (i % 4 == 0 ? ink : accent).withValues(alpha: fade);
      canvas
        ..save()
        ..translate(at.dx, at.dy)
        ..rotate(travel * (i.isEven ? 2.2 : -1.8))
        ..drawRRect(
          RRect.fromRectAndRadius(
            Rect.fromCenter(center: Offset.zero, width: side, height: side),
            Radius.circular(side * 0.25),
          ),
          paint,
        )
        ..restore();
    }
  }

  @override
  bool shouldRepaint(_BurstPainter old) => old.t != t;
}
