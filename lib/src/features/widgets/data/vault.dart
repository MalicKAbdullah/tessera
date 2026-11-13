import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:core_crypto/core_crypto.dart';

/// Named JSON documents the app keeps.
abstract interface class Documents {
  Future<Object?> read(String name);
  Future<void> write(String name, Object? value);
}

/// Each document AES-256-GCM encrypted with core_crypto into its own file,
/// under the data key the widget engine keeps wrapped by the Android Keystore
/// (android/.../security/DataKey.kt): widgets must decrypt without Flutter,
/// so the engine owns the key and hands it to the app over the channel.
///
/// Writes to one document run in order, each to a temp file then renamed,
/// so an interrupted or overlapping write never corrupts it.
class Vault implements Documents {
  Vault(this._dir, this._key, {CipherService cipher = const CipherService()})
    : _cipher = cipher;

  final String _dir;
  final Uint8List _key;
  final CipherService _cipher;
  final Map<String, Future<void>> _writes = {};

  File _file(String name) => File('$_dir/$name.vault');

  @override
  Future<Object?> read(String name) async {
    await _writes[name];
    final file = _file(name);
    if (!file.existsSync()) return null;
    final plain = await _cipher.decrypt(
      payload: EncryptedPayload.fromBytes(await file.readAsBytes()),
      keyBytes: _key,
    );
    return jsonDecode(plain);
  }

  @override
  Future<void> write(String name, Object? value) {
    final plain = jsonEncode(value);
    final previous = _writes[name] ?? Future<void>.value();
    final next = previous.then((_) => _seal(name, plain));
    _writes[name] = next;
    return next;
  }

  Future<void> _seal(String name, String plain) async {
    final payload = await _cipher.encrypt(
      plaintext: plain,
      keyBytes: _key,
      salt: await _cipher.generateSalt(),
    );
    final file = _file(name);
    await file.parent.create(recursive: true);
    final tmp = File('${file.path}.tmp');
    await tmp.writeAsBytes(payload.toBytes(), flush: true);
    await tmp.rename(file.path);
  }
}
