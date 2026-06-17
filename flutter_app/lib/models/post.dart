class Post {
  final int id;
  final String? instagramPostId;
  final String? caption;
  final int? likes;
  final int? comments;
  final DateTime? postDate;
  final String? postUrl;
  final String? imageUrl;
  final double? engagementRate;

  Post({
    required this.id,
    this.instagramPostId,
    this.caption,
    this.likes,
    this.comments,
    this.postDate,
    this.postUrl,
    this.imageUrl,
    this.engagementRate,
  });

  factory Post.fromJson(Map<String, dynamic> json) {
    return Post(
      id: json['id'] as int,
      instagramPostId: json['instagramPostId'] as String?,
      caption: json['caption'] as String?,
      likes: (json['likes'] as num?)?.toInt(),
      comments: (json['comments'] as num?)?.toInt(),
      postDate: json['postDate'] != null ? DateTime.tryParse(json['postDate']) : null,
      postUrl: json['postUrl'] as String?,
      imageUrl: json['imageUrl'] as String?,
      engagementRate: (json['engagementRate'] as num?)?.toDouble(),
    );
  }
}
