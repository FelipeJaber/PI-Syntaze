import 'package:flutter/foundation.dart';

import '../models/growth.dart';
import '../services/api_service.dart';

class LeaderboardProvider extends ChangeNotifier {
  final ApiService _api = ApiService();

  List<Growth> entries = [];
  bool isLoading = false;
  String? error;
  int days = 7;

  Future<void> load({int? days}) async {
    if (days != null) this.days = days;
    isLoading = true;
    error = null;
    notifyListeners();
    try {
      entries = await _api.getLeaderboard(days: this.days);
    } catch (e) {
      error = e.toString();
    } finally {
      isLoading = false;
      notifyListeners();
    }
  }
}
