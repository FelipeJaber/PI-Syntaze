class Profile {
  final int id;
  final String username;
  final String? fullName;
  final String? bio;
  final int? followers;
  final int? following;
  final int? postsCount;

  Profile({
    required this.id,
    required this.username,
    this.fullName,
    this.bio,
    this.followers,
    this.following,
    this.postsCount,
  });

  factory Profile.fromJson(Map<String, dynamic> json) {
    return Profile(
      id: json['id'] as int,
      username: json['username'] as String,
      fullName: json['fullName'] as String?,
      bio: json['bio'] as String?,
      followers: (json['followers'] as num?)?.toInt(),
      following: (json['following'] as num?)?.toInt(),
      postsCount: (json['postsCount'] as num?)?.toInt(),
    );
  }
}
