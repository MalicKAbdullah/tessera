/// The photos the photo widgets show, as the native store keeps them
/// (android/.../data/Photos.kt). Newest first.
class PhotoAlbum {
  const PhotoAlbum({
    required this.photos,
    required this.caption,
    required this.max,
  });

  final List<AlbumPhoto> photos;
  final String caption;

  /// Most photos the album holds.
  final int max;

  bool get isFull => photos.length >= max;

  factory PhotoAlbum.fromJson(Map<String, dynamic> json) => PhotoAlbum(
    photos: [
      for (final p in json['photos'] as List<dynamic>)
        AlbumPhoto.fromJson(p as Map<String, dynamic>),
    ],
    caption: json['caption'] as String,
    max: json['max'] as int,
  );
}

class AlbumPhoto {
  const AlbumPhoto({required this.id, required this.path, required this.date});

  final String id;

  /// Absolute path of the downscaled private copy.
  final String path;
  final DateTime date;

  factory AlbumPhoto.fromJson(Map<String, dynamic> json) => AlbumPhoto(
    id: json['id'] as String,
    path: json['path'] as String,
    date: DateTime.parse(json['date'] as String),
  );
}
