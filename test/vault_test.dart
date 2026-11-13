import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:core_crypto/core_crypto.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tessera/src/features/widgets/data/vault.dart';

Uint8List _key(int seed) =>
    Uint8List.fromList(List.generate(32, (i) => i ^ seed));

void main() {
  late Directory dir;
  setUp(() => dir = Directory.systemTemp.createTempSync('vault'));
  tearDown(() => dir.deleteSync(recursive: true));

  test('a document round-trips and is not plaintext on disk', () async {
    final vault = Vault(dir.path, _key(1));
    await vault.write('content', {'note': 'Buy milk', 'n': 3});

    expect(await Vault(dir.path, _key(1)).read('content'), {
      'note': 'Buy milk',
      'n': 3,
    });
    final bytes = File('${dir.path}/content.vault').readAsBytesSync();
    expect(latin1.decode(bytes).contains('Buy milk'), isFalse);
  });

  test('a missing document reads as null', () async {
    expect(await Vault(dir.path, _key(1)).read('drafts'), isNull);
  });

  test('another key cannot open a document', () async {
    await Vault(dir.path, _key(1)).write('content', {'note': 'x'});
    expect(Vault(dir.path, _key(2)).read('content'), throwsA(anything));
  });

  test('overlapping writes land in order', () async {
    final vault = Vault(dir.path, _key(1));
    await Future.wait([for (var i = 0; i < 20; i++) vault.write('d', i)]);
    expect(await vault.read('d'), 19);
  });

  test('the layout is core_crypto\'s, so the engine can open it', () async {
    await Vault(dir.path, _key(1)).write('content', 'hi');
    final payload = EncryptedPayload.fromBytes(
      File('${dir.path}/content.vault').readAsBytesSync(),
    );
    expect(payload.salt, hasLength(32));
    expect(payload.nonce, hasLength(12));
    expect(
      await const CipherService().decrypt(payload: payload, keyBytes: _key(1)),
      '"hi"',
    );
  });
}
