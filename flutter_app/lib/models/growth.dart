class Growth {
  final int? profileId;
  final String username;
  final int periodDays;
  final int? rank;
  final int? followersStart;
  final int? followersEnd;
  final int? followersDelta;
  final double? followersGrowthPercent;
  final int? postsCountDelta;
  final double? avgEngagementRate;
  final int snapshotsInPeriod;
  final bool insufficientData;

  Growth({
    this.profileId,
    required this.username,
    required this.periodDays,
    this.rank,
    this.followersStart,
    this.followersEnd,
    this.followersDelta,
    this.followersGrowthPercent,
    this.postsCountDelta,
    this.avgEngagementRate,
    required this.snapshotsInPeriod,
    required this.insufficientData,
  });

  factory Growth.fromJson(Map<String, dynamic> json) {
    return Growth(
      profileId: json['profileId'] as int?,
      username: json['username'] as String,
      periodDays: json['periodDays'] as int? ?? 0,
      rank: json['rank'] as int?,
      followersStart: (json['followersStart'] as num?)?.toInt(),
      followersEnd: (json['followersEnd'] as num?)?.toInt(),
      followersDelta: (json['followersDelta'] as num?)?.toInt(),
      followersGrowthPercent: (json['followersGrowthPercent'] as num?)?.toDouble(),
      postsCountDelta: (json['postsCountDelta'] as num?)?.toInt(),
      avgEngagementRate: (json['avgEngagementRate'] as num?)?.toDouble(),
      snapshotsInPeriod: json['snapshotsInPeriod'] as int? ?? 0,
      insufficientData: json['insufficientData'] as bool? ?? false,
    );
  }
}
