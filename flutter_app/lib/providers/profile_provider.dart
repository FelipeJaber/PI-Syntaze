import 'package:flutter/foundation.dart';

import '../models/profile.dart';
import '../models/post.dart';
import '../services/api_service.dart';

class ProfileProvider extends ChangeNotifier {
  final ApiService _api = ApiService();

  List<Profile> profiles = [];
  bool isLoading = false;
  String? error;

  Future<void> loadProfiles() async {
    isLoading = true;
    error = null;
    notifyListeners();
    try {
      profiles = await _api.getProfiles();
    } catch (e) {
      error = e.toString();
    } finally {
      isLoading = false;
      notifyListeners();
    }
  }

  Future<List<Post>> loadPosts(int profileId) {
    return _api.getPosts(profileId);
  }

  Future<void> syncProfiles(List<String> usernames) async {
    await _api.syncProfiles(usernames);
    await loadProfiles();
  }
}
