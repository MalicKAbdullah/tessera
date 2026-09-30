import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../widgets/models/catalog.dart';
import '../../widgets/models/widget_style.dart';
import '../../widgets/providers/widget_providers.dart';
import '../../widgets/widgets/native_preview.dart';

/// Every design, by category. With [configuring] set, the launcher is
/// placing that widget and only designs that fit its slot are offered.
class GalleryScreen extends ConsumerStatefulWidget {
  const GalleryScreen({super.key, this.configuring});
  final int? configuring;

  @override
  ConsumerState<GalleryScreen> createState() => _GalleryScreenState();
}

class _GalleryScreenState extends ConsumerState<GalleryScreen> {
  String? _category;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final catalog = ref.watch(catalogProvider);
    final placed = ref.watch(placedProvider).valueOrNull ?? const [];
    final slot = widget.configuring == null
        ? null
        : placed.where((p) => p.id == widget.configuring).firstOrNull;
    final category = slot?.category ?? _category;
    final designs = [
      for (final d in catalog.designs)
        if ((category == null || d.category == category) &&
            (slot == null || d.sizes.any((s) => s.id == slot.size)))
          d,
    ];
    return Scaffold(
      body: SafeArea(
        child: CustomScrollView(
          slivers: [
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(24, 32, 24, 8),
              sliver: SliverToBoxAdapter(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      slot == null ? 'Tessera' : 'Choose a design',
                      style: theme.textTheme.headlineMedium?.copyWith(
                        fontWeight: FontWeight.w300,
                        letterSpacing: -0.5,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      slot == null
                          ? 'Widgets, quietly yours.'
                          : 'For your ${slot.category} widget',
                      style: theme.textTheme.bodyMedium?.copyWith(
                        color: theme.colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
            ),
            if (slot == null) ...[
              if (placed.isNotEmpty)
                SliverToBoxAdapter(child: _PlacedStrip(placed: placed)),
              SliverToBoxAdapter(
                child: SizedBox(
                  height: 56,
                  child: ListView(
                    scrollDirection: Axis.horizontal,
                    padding: const EdgeInsets.symmetric(
                      horizontal: 20,
                      vertical: 8,
                    ),
                    children: [
                      _Chip(
                        label: 'All',
                        selected: _category == null,
                        onTap: () => setState(() => _category = null),
                      ),
                      for (final c in catalog.categories)
                        _Chip(
                          label: c.label,
                          selected: _category == c.id,
                          onTap: () => setState(() => _category = c.id),
                        ),
                    ],
                  ),
                ),
              ),
            ],
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 32),
              sliver: SliverGrid.builder(
                gridDelegate: const SliverGridDelegateWithMaxCrossAxisExtent(
                  maxCrossAxisExtent: 480,
                  mainAxisSpacing: 12,
                  crossAxisSpacing: 12,
                  childAspectRatio: 1.05,
                ),
                itemCount: designs.length,
                itemBuilder: (context, i) => _DesignCard(
                  design: designs[i],
                  size: slot == null
                      ? designs[i].sizes.first
                      : designs[i].size(slot.size),
                  onTap: () => context.go(
                    slot == null
                        ? '/design/${designs[i].id}'
                        : '/configure/${slot.id}/${designs[i].id}',
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _Chip extends StatelessWidget {
  const _Chip({
    required this.label,
    required this.selected,
    required this.onTap,
  });
  final String label;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.symmetric(horizontal: 4),
    child: ChoiceChip(
      label: Text(label),
      selected: selected,
      showCheckmark: false,
      onSelected: (_) => onTap(),
    ),
  );
}

/// Widgets already on the home screen; tapping one edits it in place.
class _PlacedStrip extends ConsumerWidget {
  const _PlacedStrip({required this.placed});
  final List<PlacedWidget> placed;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final catalog = ref.watch(catalogProvider);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(24, 16, 24, 8),
          child: Text(
            'ON YOUR HOME SCREEN',
            style: theme.textTheme.labelSmall?.copyWith(
              letterSpacing: 1.4,
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
        ),
        SizedBox(
          height: 120,
          child: ListView.separated(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(horizontal: 20),
            itemCount: placed.length,
            separatorBuilder: (_, _) => const SizedBox(width: 12),
            itemBuilder: (context, i) {
              final p = placed[i];
              final design = catalog.design(p.design);
              final size =
                  design.sizes.where((s) => s.id == p.size).firstOrNull ??
                  design.sizes.first;
              return InkWell(
                borderRadius: BorderRadius.circular(20),
                onTap: () => context.go('/widget/${p.id}'),
                child: Backdrop(
                  radius: 20,
                  child: Padding(
                    padding: const EdgeInsets.all(10),
                    child: NativePreview(
                      design: p.design,
                      style: p.style,
                      size: size,
                      width: 100 * size.widthDp / size.heightDp,
                    ),
                  ),
                ),
              );
            },
          ),
        ),
      ],
    );
  }
}

class _DesignCard extends ConsumerWidget {
  const _DesignCard({
    required this.design,
    required this.size,
    required this.onTap,
  });
  final DesignInfo design;
  final SizeInfo size;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final WidgetStyle style = ref.watch(styleProvider(DraftTarget(design.id)));
    return InkWell(
      borderRadius: BorderRadius.circular(28),
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.all(4),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              child: Backdrop(
                child: LayoutBuilder(
                  builder: (context, box) {
                    final aspect = size.widthDp / size.heightDp;
                    final width = (box.maxHeight - 32) * aspect;
                    return Center(
                      child: NativePreview(
                        design: design.id,
                        style: style,
                        size: size,
                        width: width.clamp(80, box.maxWidth - 32),
                      ),
                    );
                  },
                ),
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(8, 12, 8, 4),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          design.name,
                          style: theme.textTheme.titleMedium?.copyWith(
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      ),
                      if (design.motion != null)
                        Icon(
                          Icons.motion_photos_on_outlined,
                          size: 16,
                          color: theme.colorScheme.onSurfaceVariant,
                        ),
                      const SizedBox(width: 6),
                      Text(
                        design.sizes.map((s) => s.label).join(' · '),
                        style: theme.textTheme.labelSmall?.copyWith(
                          color: theme.colorScheme.onSurfaceVariant,
                          fontFeatures: const [FontFeature.tabularFigures()],
                        ),
                      ),
                    ],
                  ),
                  Text(
                    design.blurb,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: theme.textTheme.bodySmall?.copyWith(
                      color: theme.colorScheme.onSurfaceVariant,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
