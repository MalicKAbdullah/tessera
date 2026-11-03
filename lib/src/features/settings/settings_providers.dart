import 'package:core_theme/core_theme.dart';
import 'package:core_update/core_update.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:package_info_plus/package_info_plus.dart';

import '../widgets/providers/widget_providers.dart';

/// Loaded before `runApp` (see main) so the first frame has the right theme.
final appearanceProvider = Provider<AppearanceController>(
  (ref) => throw StateError('appearanceProvider not overridden'),
);

final appVersionProvider = FutureProvider<String>(
  (ref) async => (await PackageInfo.fromPlatform()).version,
);

final updateServiceProvider = Provider<IUpdateService>(
  (ref) => GithubUpdateService(owner: 'MalicKAbdullah', repo: 'tessera'),
);

/// Shared-preferences key for checking for updates on open.
const updateAutoCheckKey = 'updateAutoCheck';

/// Whether the gallery asks GitHub for a newer release on open. On by
/// default; the update check is the app's only call that is not widget data.
final updateAutoCheckProvider = Provider<bool>(
  (ref) =>
      ref.watch(sharedPreferencesProvider).getBool(updateAutoCheckKey) ?? true,
);

Future<void> setUpdateAutoCheck(WidgetRef ref, bool on) async {
  await ref.read(sharedPreferencesProvider).setBool(updateAutoCheckKey, on);
  ref.invalidate(updateAutoCheckProvider);
}

/// The pending update: null when the check is off, up to date, or offline.
final updateCheckProvider = FutureProvider<UpdateInfo?>((ref) async {
  if (!ref.watch(updateAutoCheckProvider)) return null;
  return ref.watch(updateServiceProvider).check();
});

/// The update banner was dismissed for this session.
final updateDismissedProvider = StateProvider<bool>((ref) => false);
