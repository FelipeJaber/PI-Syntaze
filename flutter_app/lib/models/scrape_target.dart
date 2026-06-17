class ScrapeTarget {
  final int id;
  final String username;
  final bool active;
  final String lastStatus;
  final String? lastMessage;
  final DateTime? lastCheckedAt;

  ScrapeTarget({
    required this.id,
    required this.username,
    required this.active,
    this.lastStatus = 'PENDING',
    this.lastMessage,
    this.lastCheckedAt,
  });

  factory ScrapeTarget.fromJson(Map<String, dynamic> json) {
    return ScrapeTarget(
      id: json['id'] as int,
      username: json['username'] as String,
      active: json['active'] as bool? ?? true,
      lastStatus: json['lastStatus'] as String? ?? 'PENDING',
      lastMessage: json['lastMessage'] as String?,
      lastCheckedAt: json['lastCheckedAt'] != null ? DateTime.tryParse(json['lastCheckedAt']) : null,
    );
  }

  ScrapeTarget copyWith({bool? active}) {
    return ScrapeTarget(
      id: id,
      username: username,
      active: active ?? this.active,
      lastStatus: lastStatus,
      lastMessage: lastMessage,
      lastCheckedAt: lastCheckedAt,
    );
  }
}
