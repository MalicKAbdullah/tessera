import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/design/tokens.dart';
import '../models/catalog.dart';
import '../models/widget_style.dart';
import '../providers/widget_providers.dart';

/// The design exactly as the launcher draws it: a PNG of the same
/// RemoteViews, rendered natively and scaled to [width].
///
/// Each new render fades in over the previous one, so a slider drag reads
/// as one continuous morph; frames underneath are dropped as soon as a newer
/// frame is fully opaque, because stacked translucent frames would render
/// the widget denser than it is.
class NativePreview extends ConsumerStatefulWidget {
  const NativePreview({
    super.key,
    required this.design,
    required this.style,
    required this.size,
    required this.width,
  });

  final String design;
  final WidgetStyle style;
  final SizeInfo size;
  final double width;

  @override
  ConsumerState<NativePreview> createState() => _NativePreviewState();
}

class _NativePreviewState extends ConsumerState<NativePreview> {
  static const _maxFrames = 3;

  /// Oldest first; the last is the newest render.
  final List<Uint8List> _frames = [];

  void _settled(Uint8List frame) {
    final i = _frames.indexOf(frame);
    if (i > 0) setState(() => _frames.removeRange(0, i));
  }

  @override
  Widget build(BuildContext context) {
    final height = widget.width * widget.size.heightDp / widget.size.widthDp;
    final png = ref.watch(
      previewProvider(PreviewRequest(widget.design, widget.style, widget.size)),
    );
    final latest = png.valueOrNull;
    if (latest != null && (_frames.isEmpty || _frames.last != latest)) {
      _frames.add(latest);
      if (_frames.length > _maxFrames) _frames.removeAt(0);
    }
    final fade = Motion.of(context, Motion.quick);
    return RepaintBoundary(
      child: SizedBox(
        width: widget.width,
        height: height,
        child: _frames.isEmpty
            ? _Placeholder(
                color: widget.style.background.color,
                radius:
                    widget.style.radius * widget.width / widget.size.widthDp,
              )
            : Stack(
                fit: StackFit.expand,
                children: [
                  for (final frame in _frames)
                    Image.memory(
                      frame,
                      key: ObjectKey(frame),
                      width: widget.width,
                      height: height,
                      filterQuality: FilterQuality.medium,
                      frameBuilder: (context, child, index, sync) {
                        if (sync) {
                          WidgetsBinding.instance.addPostFrameCallback(
                            (_) => mounted ? _settled(frame) : null,
                          );
                          return child;
                        }
                        return AnimatedOpacity(
                          opacity: index == null ? 0 : 1,
                          duration: fade,
                          curve: Motion.ease,
                          onEnd: () => _settled(frame),
                          child: child,
                        );
                      },
                    ),
                ],
              ),
      ),
    );
  }
}

/// Before the first render arrives: the surface colour, breathing softly.
class _Placeholder extends StatefulWidget {
  const _Placeholder({required this.color, required this.radius});
  final Color color;
  final double radius;

  @override
  State<_Placeholder> createState() => _PlaceholderState();
}

class _PlaceholderState extends State<_Placeholder>
    with SingleTickerProviderStateMixin {
  late final _c = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 1100),
  );

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (!Motion.reduced(context) && !_c.isAnimating) {
      _c.repeat(reverse: true);
    }
  }

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => FadeTransition(
    opacity: Tween(begin: 0.25, end: 0.5).animate(_c),
    child: DecoratedBox(
      decoration: BoxDecoration(
        color: widget.color,
        borderRadius: BorderRadius.circular(widget.radius),
      ),
    ),
  );
}

/// Soft wallpaper stand-in so opacity and radius read as they will on a
/// real home screen.
class Backdrop extends StatelessWidget {
  const Backdrop({super.key, required this.child, this.radius = Radii.l});
  final Widget child;
  final double radius;

  @override
  Widget build(BuildContext context) => DecoratedBox(
    decoration: BoxDecoration(
      borderRadius: BorderRadius.circular(radius),
      gradient: LinearGradient(
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
        colors: Palette.of(context).stage,
      ),
    ),
    child: child,
  );
}
