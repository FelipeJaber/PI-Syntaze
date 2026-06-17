import 'package:flutter/foundation.dart';

import '../models/top_post.dart';
import '../models/hashtag.dart';
import '../services/api_service.dart';
import '../utils/error_utils.dart';

class InsightsProvider extends ChangeNotifier {
  final ApiService _api = ApiService();

  List<TopPost> topPosts = [];
  List<Hashtag> hashtags = [];
  bool isLoading = false;
  String? error;
  int topPostsDays = 1;
  int hashtagsDays = 7;

  Future<void> load({int? topPostsDays, int? hashtagsDays}) async {
    if (topPostsDays != null) this.topPostsDays = topPostsDays;
    if (hashtagsDays != null) this.hashtagsDays = hashtagsDays;
    isLoading = true;
    error = null;
    notifyListeners();
    try {
      final results = await Future.wait([
        _api.getTopPosts(days: this.topPostsDays, limit: 12),
        _api.getTrendingHashtags(days: this.hashtagsDays, limit: 20),
      ]);
      topPosts = results[0] as List<TopPost>;
      hashtags = results[1] as List<Hashtag>;
    } catch (e) {
      error = friendlyError(e);
    } finally {
      isLoading = false;
      notifyListeners();
    }
  }
}
