import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/catalog.dart';
import '../models/widget_style.dart';
import '../providers/widget_providers.dart';

/// The design exactly as the launcher draws it: a PNG of the same
/// RemoteViews, rendered natively and scaled to [width].
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
  /// Last finished render, shown while the next one is in flight so edits
  /// never flash an empty frame.
  Uint8List? _shown;

  @override
  Widget build(BuildContext context) {
    final height = widget.width * widget.size.heightDp / widget.size.widthDp;
    final png = ref.watch(
      previewProvider(PreviewRequest(widget.design, widget.style, widget.size)),
    );
    final bytes = png.valueOrNull ?? _shown;
    if (png.valueOrNull != null) _shown = png.valueOrNull;
    return SizedBox(
      width: widget.width,
      height: height,
      child: bytes == null
          ? DecoratedBox(
              decoration: BoxDecoration(
                color: widget.style.background.color.withValues(alpha: 0.4),
                borderRadius: BorderRadius.circular(
                  widget.style.radius * widget.width / widget.size.widthDp,
                ),
              ),
            )
          : Image.memory(
              bytes,
              width: widget.width,
              height: height,
              gaplessPlayback: true,
              filterQuality: FilterQuality.medium,
            ),
    );
  }
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
