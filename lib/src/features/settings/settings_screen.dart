import 'package:core_theme/core_theme.dart';
import 'package:core_ui/core_ui.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'settings_providers.dart';

class SettingsScreen extends ConsumerWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final c = SuiteColors.of(context);
    final version = ref.watch(appVersionProvider).valueOrNull;
    return Scaffold(
      appBar: AppBar(title: const Text('Settings')),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(
          AppSpacing.gutter,
          AppSpacing.sm,
          AppSpacing.gutter,
          AppSpacing.xl,
        ),
        children: [
          RowGroup(
            children: [
              AppearanceSetting(controller: ref.watch(appearanceProvider)),
            ],
          ),
          const _Gap(),
          const SectionHeader(title: 'Updates'),
          const _UpdateTiles(),
          const _Gap(),
          const SectionHeader(title: 'About'),
          VaultCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    const SuiteAppIcon(app: SuiteApp.tessera, size: 44),
                    const SizedBox(width: AppSpacing.smd),
                    Expanded(
                      child: Text(
                        'Tessera',
                        style: AppTextStyles.h3.copyWith(color: c.textPrimary),
                      ),
                    ),
                    if (version != null)
                      Text(
                        'v$version',
                        style: AppTextStyles.numberSmall.copyWith(
                          color: c.textSecondary,
                        ),
                      ),
                  ],
                ),
                const SizedBox(height: AppSpacing.md),
                Text(
                  'Your widgets, photos and notes stay on this device. The '
                  'app goes online only for weather widgets\' forecasts and '
                  'the optional update check.',
                  style: AppTextStyles.bodySmall.copyWith(
                    color: c.textSecondary,
                  ),
                ),
                const SizedBox(height: AppSpacing.md),
                const SuiteEndorsement(),
              ],
            ),
          ),
          const SizedBox(height: AppSpacing.smd),
          RowGroup(
            children: [
              SettingsTile(
                icon: Icons.description_outlined,
                title: 'Licences',
                subtitle: 'Open-source software and widget typefaces',
                onTap: () => showLicensePage(
                  context: context,
                  applicationName: 'Tessera',
                  applicationVersion: version,
                  applicationIcon: const Padding(
                    padding: EdgeInsets.all(AppSpacing.md),
                    child: SuiteAppIcon(app: SuiteApp.tessera, size: 48),
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

/// Space before a section header.
class _Gap extends StatelessWidget {
  const _Gap();

  @override
  Widget build(BuildContext context) => const SizedBox(height: AppSpacing.mdl);
}

/// "Check on open" and a manual "Check now", backed by core_update.
class _UpdateTiles extends ConsumerStatefulWidget {
  const _UpdateTiles();

  @override
  ConsumerState<_UpdateTiles> createState() => _UpdateTilesState();
}

class _UpdateTilesState extends ConsumerState<_UpdateTiles> {
  bool _checking = false;

  Future<void> _checkNow() async {
    setState(() => _checking = true);
    final service = ref.read(updateServiceProvider);
    final info = await service.check();
    if (!mounted) return;
    setState(() => _checking = false);
    ScaffoldMessenger.of(context).showSnackBar(
      info == null
          ? const SnackBar(content: Text("You're on the latest version."))
          : SnackBar(
              content: Text('Update available · v${info.version}'),
              action: SnackBarAction(
                label: 'Update',
                onPressed: () => service.openDownload(info),
              ),
            ),
    );
  }

  @override
  Widget build(BuildContext context) => RowGroup(
    children: [
      SettingsTile(
        icon: Icons.system_update_alt,
        title: 'Check on open',
        subtitle: 'Asks GitHub for a newer release',
        showChevron: false,
        trailing: Switch(
          value: ref.watch(updateAutoCheckProvider),
          onChanged: (on) => setUpdateAutoCheck(ref, on),
        ),
      ),
      SettingsTile(
        icon: Icons.refresh,
        title: 'Check now',
        trailing: _checking
            ? const SizedBox(
                width: 18,
                height: 18,
                child: CircularProgressIndicator(strokeWidth: 2),
              )
            : null,
        onTap: _checking ? null : _checkNow,
      ),
    ],
  );
}
