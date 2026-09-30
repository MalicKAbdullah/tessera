import 'dart:async';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/design/tokens.dart';
import '../../../core/motion/motion.dart';
import '../../../core/widgets/mosaic_mark.dart';
import '../../widgets/models/photo_album.dart';
import '../../widgets/providers/widget_providers.dart';
import 'controls.dart';

/// The photo album every photo widget draws from: add, remove, caption.
class PhotoControls extends ConsumerStatefulWidget {
  const PhotoControls({super.key});

  @override
  ConsumerState<PhotoControls> createState() => _PhotoControlsState();
}

class _PhotoControlsState extends ConsumerState<PhotoControls> {
  PhotoAlbum? _album;
  bool _busy = false;
  Timer? _captionDebounce;

  @override
  void initState() {
    super.initState();
    ref.read(engineProvider).photos().then((a) {
      if (mounted) setState(() => _album = a);
    });
  }

  @override
  void dispose() {
    _captionDebounce?.cancel();
    super.dispose();
  }

  Future<void> _run(Future<PhotoAlbum> Function() action) async {
    setState(() => _busy = true);
    try {
      final album = await action();
      if (mounted) setState(() => _album = album);
    } on PlatformException catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Could not add that photo: ${e.message}')),
        );
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  void _caption(String value) {
    _captionDebounce?.cancel();
    _captionDebounce = Timer(
      const Duration(milliseconds: 400),
      () => ref.read(engineProvider).setPhotoCaption(value),
    );
  }

  @override
  Widget build(BuildContext context) {
    final album = _album;
    final theme = Theme.of(context);
    final engine = ref.read(engineProvider);
    return Section(
      title: 'Photos',
      child: album == null
          ? const SizedBox(height: 72)
          : Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                SizedBox(
                  height: 72,
                  child: ListView(
                    scrollDirection: Axis.horizontal,
                    physics: const BouncingScrollPhysics(),
                    children: [
                      if (!album.isFull)
                        _AddTile(
                          busy: _busy,
                          onTap: () => _run(engine.pickPhotos),
                        ),
                      for (final p in album.photos)
                        _Thumb(
                          photo: p,
                          onRemove: _busy
                              ? null
                              : () => _run(() => engine.removePhoto(p.id)),
                        ),
                    ],
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  album.photos.isEmpty
                      ? 'Choose up to ${album.max} photos. Tessera keeps a '
                            'small private copy of each; nothing else on your '
                            'phone is read.'
                      : '${album.photos.length} of ${album.max} · shared by '
                            'every photo widget',
                  style: theme.textTheme.bodySmall,
                ),
                const SizedBox(height: Gap.m),
                TextFormField(
                  initialValue: album.caption,
                  maxLength: 40,
                  decoration: const InputDecoration(
                    labelText: 'Caption (optional)',
                    helperText:
                        'Shown when a design’s caption is set to Caption',
                  ),
                  onChanged: _caption,
                ),
              ],
            ),
    );
  }
}

class _AddTile extends StatelessWidget {
  const _AddTile({required this.busy, required this.onTap});
  final bool busy;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final p = Palette.of(context);
    return Padding(
      padding: const EdgeInsets.only(right: Gap.s),
      child: Pressable(
        semanticLabel: 'Add photos',
        onTap: busy ? null : onTap,
        child: Container(
          width: 72,
          decoration: BoxDecoration(
            color: p.surface,
            borderRadius: BorderRadius.circular(Radii.s + 2),
            border: Border.all(color: p.hairline),
          ),
          child: Center(
            child: busy
                ? const MosaicLoader(size: 22)
                : Icon(Icons.add_photo_alternate_outlined, color: p.ink),
          ),
        ),
      ),
    );
  }
}

class _Thumb extends StatelessWidget {
  const _Thumb({required this.photo, required this.onRemove});
  final AlbumPhoto photo;
  final VoidCallback? onRemove;

  @override
  Widget build(BuildContext context) {
    final dpr = MediaQuery.devicePixelRatioOf(context);
    final p = Palette.of(context);
    return Padding(
      padding: const EdgeInsets.only(right: Gap.s),
      child: Stack(
        children: [
          ClipRRect(
            borderRadius: BorderRadius.circular(Radii.s + 2),
            child: Image.file(
              File(photo.path),
              width: 72,
              height: 72,
              fit: BoxFit.cover,
              cacheWidth: (72 * dpr).round(),
            ),
          ),
          Positioned(
            top: 4,
            right: 4,
            child: Pressable(
              scale: 0.85,
              semanticLabel: 'Remove',
              onTap: onRemove,
              child: Container(
                width: 22,
                height: 22,
                decoration: BoxDecoration(
                  color: p.canvas.withValues(alpha: 0.85),
                  shape: BoxShape.circle,
                ),
                child: Icon(Icons.close, size: 13, color: p.ink),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
