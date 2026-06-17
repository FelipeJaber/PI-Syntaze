import 'package:flutter/foundation.dart';

import '../models/scrape_target.dart';
import '../services/api_service.dart';

class WatchlistProvider extends ChangeNotifier {
  final ApiService _api = ApiService();

  List<ScrapeTarget> targets = [];
  bool isLoading = false;
  String? error;

  Future<void> load() async {
    isLoading = true;
    error = null;
    notifyListeners();
    try {
      targets = await _api.getWatchlist();
    } catch (e) {
      error = e.toString();
    } finally {
      isLoading = false;
      notifyListeners();
    }
  }

  Future<void> add(String username) async {
    final target = await _api.addToWatchlist(username);
    if (!targets.any((t) => t.username == target.username)) {
      targets = [...targets, target];
      notifyListeners();
    }
  }

  Future<void> toggle(ScrapeTarget target) async {
    final index = targets.indexWhere((t) => t.id == target.id);
    if (index == -1) return;

    // Atualização otimista para o switch responder na hora.
    targets[index] = target.copyWith(active: !target.active);
    notifyListeners();

    try {
      final updated = await _api.toggleWatchlist(target.username);
      targets[index] = updated;
    } catch (e) {
      targets[index] = target; // reverte se a chamada falhar
      error = e.toString();
    }
    notifyListeners();
  }

  Future<void> remove(ScrapeTarget target) async {
    await _api.removeFromWatchlist(target.username);
    targets = targets.where((t) => t.id != target.id).toList();
    notifyListeners();
  }
}
