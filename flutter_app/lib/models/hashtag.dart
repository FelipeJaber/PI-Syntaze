class Hashtag {
  final String tag;
  final int postCount;
  final int profileCount;
  final List<String> usernames;

  Hashtag({
    required this.tag,
    required this.postCount,
    required this.profileCount,
    required this.usernames,
  });

  factory Hashtag.fromJson(Map<String, dynamic> json) {
    return Hashtag(
      tag: json['tag'] as String,
      postCount: json['postCount'] as int? ?? 0,
      profileCount: json['profileCount'] as int? ?? 0,
      usernames: (json['usernames'] as List<dynamic>? ?? []).map((e) => e as String).toList(),
    );
  }
}
