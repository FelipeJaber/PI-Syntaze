import 'package:flutter/foundation.dart';

import '../models/growth.dart';
import '../services/api_service.dart';
import '../utils/error_utils.dart';

class LeaderboardProvider extends ChangeNotifier {
  final ApiService _api = ApiService();

  List<Growth> entries = [];
  bool isLoading = false;
  String? error;
  int days = 7;
  String sort = 'growth';

  Future<void> load({int? days, String? sort}) async {
    if (days != null) this.days = days;
    if (sort != null) this.sort = sort;
    isLoading = true;
    error = null;
    notifyListeners();
    try {
      entries = await _api.getLeaderboard(days: this.days, sort: this.sort);
    } catch (e) {
      error = friendlyError(e);
    } finally {
      isLoading = false;
      notifyListeners();
    }
  }
}
