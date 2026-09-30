import 'package:flutter_test/flutter_test.dart';
import 'package:tessera/src/features/widgets/models/photo_album.dart';

void main() {
  test('PhotoAlbum parses the native album JSON', () {
    final album = PhotoAlbum.fromJson({
      'photos': [
        {
          'id': 'a',
          'path': '/data/files/photos/a.jpg',
          'width': 1080,
          'height': 1440,
          'date': '2025-08-12',
        },
      ],
      'caption': 'Lisbon',
      'max': 12,
    });
    expect(album.photos.single.id, 'a');
    expect(album.photos.single.date, DateTime(2025, 8, 12));
    expect(album.caption, 'Lisbon');
    expect(album.isFull, isFalse);
  });

  test('an album at its maximum is full', () {
    final album = PhotoAlbum.fromJson({
      'photos': [
        for (var i = 0; i < 2; i++)
          {'id': '$i', 'path': '/p/$i.jpg', 'date': '2025-01-0${i + 1}'},
      ],
      'caption': '',
      'max': 2,
    });
    expect(album.isFull, isTrue);
  });
}
