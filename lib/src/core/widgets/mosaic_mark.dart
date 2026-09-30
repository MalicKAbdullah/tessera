import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../design/tokens.dart';

/// The Tessera mark: a 3×3 mosaic with one accent tile and one open tile.
///
/// [assemble] runs 0→1 as tiles fly in from scattered positions and settle
/// into the grid, one after another. [spread] pushes the settled tiles apart
/// from the centre (the gallery ties it to overscroll).
class MosaicMark extends StatelessWidget {
  const MosaicMark({
    super.key,
    required this.size,
    this.assemble = 1,
    this.spread = 0,
    this.breathe = 0,
  });

  final double size;
  final double assemble;
  final double spread;

  /// 0→1 phase of a slow per-tile pulse, used as a loading state.
  final double breathe;

  @override
  Widget build(BuildContext context) {
    final p = Palette.of(context);
    return RepaintBoundary(
      child: CustomPaint(
        size: Size.square(size),
        painter: _MosaicPainter(
          assemble: assemble,
          spread: spread,
          breathe: breathe,
          ink: p.ink,
          accent: p.accent,
          open: p.faint,
        ),
      ),
    );
  }
}

enum _Tone { ink, accent, open }

const _tones = [
  _Tone.ink, _Tone.ink, _Tone.accent, //
  _Tone.ink, _Tone.open, _Tone.ink, //
  _Tone.ink, _Tone.ink, _Tone.ink, //
];

/// Arrival order: a spiral from the accent tile, so the mark reads as being
/// laid by hand rather than appearing row by row.
const _order = [2, 1, 0, 3, 6, 7, 8, 5, 4];

class _MosaicPainter extends CustomPainter {
  _MosaicPainter({
    required this.assemble,
    required this.spread,
    required this.breathe,
    required this.ink,
    required this.accent,
    required this.open,
  });

  final double assemble;
  final double spread;
  final double breathe;
  final Color ink;
  final Color accent;
  final Color open;

  @override
  void paint(Canvas canvas, Size size) {
    final cell = size.width / 3;
    final gap = cell * 0.14;
    final tile = cell - gap;
    final centre = size.center(Offset.zero);
    final paint = Paint()..isAntiAlias = true;
    for (var i = 0; i < 9; i++) {
      final rank = _order.indexOf(i);
      final start = rank * 0.07;
      final local = ((assemble - start) / 0.44).clamp(0.0, 1.0);
      if (local == 0) continue;
      final t = Curves.easeOutBack.transform(local);
      final home = Offset(
        (i % 3) * cell + gap / 2 + tile / 2,
        (i ~/ 3) * cell + gap / 2 + tile / 2,
      );
      final outward = home - centre;
      final settled = home + outward * (spread * 0.35);
      // Deterministic scatter: each tile starts off to its own side.
      final angle = i * 2.39996 + 0.6;
      final from =
          centre + Offset(math.cos(angle), math.sin(angle)) * size.width * 1.1;
      final at = Offset.lerp(from, settled, t)!;
      final pulse = breathe == 0
          ? 1.0
          : 0.55 +
                0.45 *
                    (0.5 + 0.5 * math.cos((breathe - rank / 9) * math.pi * 2));
      final colour = switch (_tones[i]) {
        _Tone.ink => ink,
        _Tone.accent => accent,
        _Tone.open => open,
      };
      paint.color = colour.withValues(alpha: colour.a * local * pulse);
      canvas
        ..save()
        ..translate(at.dx, at.dy)
        ..rotate((1 - t) * (i.isEven ? 1.4 : -1.1))
        ..scale(0.6 + 0.4 * t);
      final rect = Rect.fromCenter(
        center: Offset.zero,
        width: tile,
        height: tile,
      );
      canvas
        ..drawRRect(
          RRect.fromRectAndRadius(rect, Radius.circular(tile * 0.24)),
          paint,
        )
        ..restore();
    }
  }

  @override
  bool shouldRepaint(_MosaicPainter old) =>
      old.assemble != assemble ||
      old.spread != spread ||
      old.breathe != breathe ||
      old.ink != ink ||
      old.accent != accent ||
      old.open != open;
}

/// A breathing mark for loading states.
class MosaicLoader extends StatefulWidget {
  const MosaicLoader({super.key, this.size = 40});
  final double size;

  @override
  State<MosaicLoader> createState() => _MosaicLoaderState();
}

class _MosaicLoaderState extends State<MosaicLoader>
    with SingleTickerProviderStateMixin {
  late final _c = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 1600),
  );

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (Motion.reduced(context)) {
      _c.stop();
    } else if (!_c.isAnimating) {
      _c.repeat();
    }
  }

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Semantics(
    label: 'Loading',
    child: AnimatedBuilder(
      animation: _c,
      builder: (context, _) => MosaicMark(
        size: widget.size,
        breathe: Motion.reduced(context) ? 0 : _c.value + 1e-6,
      ),
    ),
  );
}
