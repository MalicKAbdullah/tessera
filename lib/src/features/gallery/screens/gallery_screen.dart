import 'package:core_theme/core_theme.dart';
import 'package:core_ui/core_ui.dart';
import 'package:core_update/core_update.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/design/tokens.dart';
import '../../../core/motion/motion.dart';
import '../../intro/intro_overlay.dart';
import '../../settings/settings_providers.dart';
import '../../widgets/models/catalog.dart';
import '../../widgets/providers/widget_providers.dart';
import '../../widgets/widgets/native_preview.dart';

/// Hero tag shared by a preview in the gallery and the editor stage it opens.
String designHeroTag(String design) => 'design-$design';
String placedHeroTag(int widgetId) => 'placed-$widgetId';

/// Every design, by category. With [configuring] set, the launcher is
/// placing that widget and only designs that fit its slot are offered.
class GalleryScreen extends ConsumerStatefulWidget {
  const GalleryScreen({super.key, this.configuring});
  final int? configuring;

  @override
  ConsumerState<GalleryScreen> createState() => _GalleryScreenState();
}

class _GalleryScreenState extends ConsumerState<GalleryScreen>
    with TickerProviderStateMixin {
  /// Pull distance past the top that refreshes previews.
  static const _pullRefresh = 90.0;

  late final TabController _tabs;

  /// Header and home-screen strip, once per visit.
  late final _enter = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 900),
  );

  /// Design cards; replays on every category change.
  late final _grid = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 1100),
  );
  final _scroll = ScrollController();
  final _markKey = GlobalKey();
  late bool _intro;
  bool _pulled = false;

  @override
  void initState() {
    super.initState();
    final catalog = ref.read(catalogProvider);
    _tabs = TabController(length: catalog.categories.length + 1, vsync: this)
      ..addListener(_onTab);
    _intro = widget.configuring == null && !ref.read(storeProvider).introSeen;
    if (!_intro) _reveal();
    _scroll.addListener(_onScroll);
  }

  void _reveal() {
    _enter.forward();
    _grid.forward();
  }

  @override
  void dispose() {
    _tabs.dispose();
    _enter.dispose();
    _grid.dispose();
    _scroll.dispose();
    super.dispose();
  }

  String? get _category => _tabs.index == 0
      ? null
      : ref.read(catalogProvider).categories[_tabs.index - 1].id;

  void _onTab() {
    if (_tabs.indexIsChanging) return;
    setState(() {});
    _grid.forward(from: 0);
  }

  void _onScroll() {
    final pull = -_scroll.offset;
    if (!_pulled && pull > _pullRefresh) {
      _pulled = true;
      HapticFeedback.lightImpact();
      ref.read(dataRevisionProvider.notifier).state++;
    } else if (_pulled && pull < 8) {
      _pulled = false;
    }
  }

  void _introDone() {
    ref.read(storeProvider).markIntroSeen();
    setState(() => _intro = false);
  }

  @override
  Widget build(BuildContext context) {
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
    final configuring = widget.configuring != null;
    final gallery = Scaffold(
      body: SafeArea(
        bottom: false,
        child: CustomScrollView(
          controller: _scroll,
          physics: const BouncingScrollPhysics(
            parent: AlwaysScrollableScrollPhysics(),
          ),
          slivers: [
            SliverToBoxAdapter(
              child: _Header(
                scroll: _scroll,
                enter: _enter,
                markKey: _markKey,
                markVisible: !_intro,
                showSettings: !configuring,
                title: configuring
                    ? 'Choose a design'
                    : 'Widgets,\nquietly yours.',
                subtitle: slot == null
                    ? '${catalog.designs.length} DESIGNS'
                    : 'FOR YOUR ${slot.category.toUpperCase()} WIDGET',
              ),
            ),
            if (!configuring) ...[
              const SliverToBoxAdapter(child: _Update()),
              if (placed.isNotEmpty)
                SliverToBoxAdapter(
                  child: Entrance(
                    animation: _enter,
                    index: 1,
                    child: _PlacedStrip(placed: placed),
                  ),
                ),
              SliverPersistentHeader(
                pinned: true,
                delegate: _TabsHeader(
                  tabs: _tabs,
                  labels: ['All', for (final c in catalog.categories) c.label],
                  canvas: SuiteColors.of(context).background,
                ),
              ),
            ],
            if (designs.isEmpty)
              SliverFillRemaining(
                hasScrollBody: false,
                child: _Empty(configuring: configuring),
              )
            else
              SliverPadding(
                padding: const EdgeInsets.fromLTRB(
                  Gap.l,
                  Gap.s,
                  Gap.l,
                  Gap.xxl * 2,
                ),
                sliver: SliverGrid.builder(
                  gridDelegate: const SliverGridDelegateWithMaxCrossAxisExtent(
                    maxCrossAxisExtent: 480,
                    mainAxisSpacing: Gap.xl,
                    crossAxisSpacing: Gap.m,
                    childAspectRatio: 1.0,
                  ),
                  itemCount: designs.length,
                  itemBuilder: (context, i) {
                    final d = designs[i];
                    return Entrance(
                      key: ValueKey('${category ?? 'all'}/${d.id}'),
                      animation: _grid,
                      index: i,
                      child: _DesignCard(
                        design: d,
                        size: slot == null ? d.sizes.first : d.size(slot.size),
                        onTap: () => context.go(
                          slot == null
                              ? '/design/${d.id}'
                              : '/configure/${slot.id}/${d.id}',
                        ),
                      ),
                    );
                  },
                ),
              ),
          ],
        ),
      ),
    );
    // The gallery keeps its place in the tree when the overlay leaves, so
    // its scroll position and animations carry on.
    return Stack(
      children: [
        gallery,
        if (_intro)
          Positioned.fill(
            child: IntroOverlay(
              markTarget: _markKey,
              onReveal: _reveal,
              onDone: _introDone,
            ),
          ),
      ],
    );
  }
}

class _Header extends StatelessWidget {
  const _Header({
    required this.scroll,
    required this.enter,
    required this.markKey,
    required this.markVisible,
    required this.showSettings,
    required this.title,
    required this.subtitle,
  });

  final ScrollController scroll;
  final Animation<double> enter;
  final GlobalKey markKey;
  final bool markVisible;
  final bool showSettings;
  final String title;
  final String subtitle;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final reduced = Motion.reduced(context);
    return Padding(
      padding: const EdgeInsets.fromLTRB(Gap.xl, Gap.s, Gap.s, Gap.l),
      child: AnimatedBuilder(
        animation: scroll,
        builder: (context, _) {
          final offset = scroll.hasClients ? scroll.offset : 0.0;
          final pull = reduced ? 0.0 : (-offset / 140).clamp(0.0, 1.0);
          final lift = reduced ? 0.0 : offset.clamp(0.0, 200.0);
          return Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              SizedBox(
                height: AppSpacing.tapTarget,
                child: Row(
                  children: [
                    Opacity(
                      opacity: markVisible ? 1 : 0,
                      child: SuiteAppIcon(
                        key: markKey,
                        app: SuiteApp.tessera,
                        size: 28,
                      ),
                    ),
                    const SizedBox(width: Gap.m),
                    Text('Tessera', style: theme.textTheme.titleLarge),
                    const Spacer(),
                    Text(subtitle, style: theme.textTheme.labelSmall),
                    if (showSettings)
                      IconButton(
                        tooltip: 'Settings',
                        onPressed: () => context.go('/settings'),
                        icon: const Icon(Icons.settings_outlined),
                      ),
                  ],
                ),
              ),
              const SizedBox(height: Gap.xl),
              Entrance(
                animation: enter,
                index: 0,
                child: Transform.translate(
                  offset: Offset(0, lift * 0.35 + pull * 18),
                  child: Opacity(
                    opacity: (1 - lift / 180).clamp(0.0, 1.0),
                    child: Text(title, style: theme.textTheme.displaySmall),
                  ),
                ),
              ),
            ],
          );
        },
      ),
    );
  }
}

/// Category tabs; the ink pill glides between them with the controller.
class _TabsHeader extends SliverPersistentHeaderDelegate {
  _TabsHeader({required this.tabs, required this.labels, required this.canvas});

  final TabController tabs;
  final List<String> labels;
  final Color canvas;

  static const _height = 60.0;

  @override
  double get minExtent => _height;
  @override
  double get maxExtent => _height;

  @override
  Widget build(BuildContext context, double shrink, bool overlaps) {
    final c = SuiteColors.of(context);
    final theme = Theme.of(context);
    return AnimatedContainer(
      duration: Motion.of(context, Motion.base),
      decoration: BoxDecoration(
        color: canvas,
        border: Border(
          bottom: BorderSide(
            color: overlaps ? c.hairline : c.hairline.withValues(alpha: 0),
          ),
        ),
      ),
      alignment: Alignment.centerLeft,
      child: TabBar(
        controller: tabs,
        isScrollable: true,
        tabAlignment: TabAlignment.start,
        padding: const EdgeInsets.symmetric(horizontal: Gap.l),
        labelPadding: const EdgeInsets.symmetric(horizontal: Gap.l),
        dividerColor: Colors.transparent,
        indicatorSize: TabBarIndicatorSize.tab,
        indicator: BoxDecoration(
          color: c.accentContainer,
          borderRadius: BorderRadius.circular(Radii.pill),
        ),
        indicatorPadding: const EdgeInsets.symmetric(vertical: 12),
        labelColor: c.textPrimary,
        unselectedLabelColor: c.textSecondary,
        labelStyle: theme.textTheme.labelLarge!.copyWith(fontSize: 14),
        unselectedLabelStyle: theme.textTheme.labelLarge!.copyWith(
          fontSize: 14,
        ),
        overlayColor: const WidgetStatePropertyAll(Colors.transparent),
        splashFactory: NoSplash.splashFactory,
        onTap: (_) => HapticFeedback.selectionClick(),
        tabs: [for (final l in labels) Tab(text: l, height: _height)],
      ),
    );
  }

  @override
  bool shouldRebuild(_TabsHeader old) =>
      old.tabs != tabs || old.labels != labels || old.canvas != canvas;
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
          padding: const EdgeInsets.fromLTRB(Gap.xl, Gap.s, Gap.xl, Gap.m),
          child: Text('ON YOUR HOME SCREEN', style: theme.textTheme.labelSmall),
        ),
        SizedBox(
          height: 116,
          child: ListView.separated(
            scrollDirection: Axis.horizontal,
            physics: const BouncingScrollPhysics(),
            padding: const EdgeInsets.symmetric(horizontal: Gap.page),
            itemCount: placed.length,
            separatorBuilder: (_, _) => const SizedBox(width: Gap.m),
            itemBuilder: (context, i) {
              final p = placed[i];
              final design = catalog.design(p.design);
              final shown =
                  design.sizes.where((s) => s.id == p.size).firstOrNull ??
                  design.sizes.first;
              return Pressable(
                semanticLabel: 'Edit ${design.name}',
                onTap: () => context.go('/widget/${p.id}'),
                child: Hero(
                  tag: placedHeroTag(p.id),
                  child: Backdrop(
                    radius: Radii.m,
                    child: Padding(
                      padding: const EdgeInsets.all(Gap.m),
                      child: NativePreview(
                        design: p.design,
                        style: p.style,
                        size: shown,
                        width: 92 * shown.widthDp / shown.heightDp,
                      ),
                    ),
                  ),
                ),
              );
            },
          ),
        ),
        const SizedBox(height: Gap.s),
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
    final c = SuiteColors.of(context);
    final style = ref.watch(styleProvider(DraftTarget(design.id)));
    return Pressable(
      semanticLabel: design.name,
      onTap: onTap,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            child: Hero(
              tag: designHeroTag(design.id),
              child: Backdrop(
                child: LayoutBuilder(
                  builder: (context, box) {
                    final aspect = size.widthDp / size.heightDp;
                    final width = ((box.maxHeight - 48) * aspect).clamp(
                      80.0,
                      box.maxWidth - 48,
                    );
                    return Center(
                      child: NativePreview(
                        design: design.id,
                        style: style,
                        size: size,
                        width: width,
                      ),
                    );
                  },
                ),
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(Gap.xs, Gap.m, Gap.xs, 0),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.center,
              children: [
                Expanded(
                  child: Text(design.name, style: theme.textTheme.titleMedium),
                ),
                if (design.motion != null) ...[
                  Container(
                    width: 6,
                    height: 6,
                    decoration: BoxDecoration(
                      color: c.accent,
                      shape: BoxShape.circle,
                    ),
                  ),
                  const SizedBox(width: Gap.xs + 2),
                  Text('MOTION', style: theme.textTheme.labelSmall),
                  const SizedBox(width: Gap.m),
                ],
                Text(
                  design.sizes.map((s) => s.label).join(' '),
                  style: theme.textTheme.labelMedium,
                ),
              ],
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(Gap.xs, Gap.xs, Gap.xs, 0),
            child: Text(
              design.blurb,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: theme.textTheme.bodySmall,
            ),
          ),
        ],
      ),
    );
  }
}

class _Empty extends StatelessWidget {
  const _Empty({required this.configuring});
  final bool configuring;

  @override
  Widget build(BuildContext context) => AppEmptyState(
    icon: Icons.widgets_outlined,
    title: configuring ? 'Nothing fits this slot yet' : 'No designs here yet',
    message: configuring
        ? 'Try a different widget size from your launcher.'
        : 'New designs arrive with updates.',
  );
}

/// A newer release on GitHub, until dismissed for the session.
class _Update extends ConsumerWidget {
  const _Update();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final info = ref.watch(updateCheckProvider).valueOrNull;
    if (info == null || ref.watch(updateDismissedProvider)) {
      return const SizedBox.shrink();
    }
    return Padding(
      padding: const EdgeInsets.fromLTRB(Gap.l, 0, Gap.l, Gap.l),
      child: UpdateBanner(
        info: info,
        onUpdate: () => ref.read(updateServiceProvider).openDownload(info),
        onDismiss: () =>
            ref.read(updateDismissedProvider.notifier).state = true,
      ),
    );
  }
}
