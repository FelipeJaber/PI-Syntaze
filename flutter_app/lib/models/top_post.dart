class TopPost {
  final int postId;
  final int profileId;
  final String username;
  final String? caption;
  final int? likes;
  final int? comments;
  final double? engagementRate;
  final DateTime? postDate;
  final String? postUrl;
  final String? imageUrl;

  TopPost({
    required this.postId,
    required this.profileId,
    required this.username,
    this.caption,
    this.likes,
    this.comments,
    this.engagementRate,
    this.postDate,
    this.postUrl,
    this.imageUrl,
  });

  factory TopPost.fromJson(Map<String, dynamic> json) {
    return TopPost(
      postId: json['postId'] as int,
      profileId: json['profileId'] as int,
      username: json['username'] as String,
      caption: json['caption'] as String?,
      likes: (json['likes'] as num?)?.toInt(),
      comments: (json['comments'] as num?)?.toInt(),
      engagementRate: (json['engagementRate'] as num?)?.toDouble(),
      postDate: json['postDate'] != null ? DateTime.tryParse(json['postDate']) : null,
      postUrl: json['postUrl'] as String?,
      imageUrl: json['imageUrl'] as String?,
    );
  }
}
