import 'package:flutter/foundation.dart';

import '../services/api_service.dart';

class AuthProvider extends ChangeNotifier {
  final ApiService _api = ApiService();

  String? username;
  bool isLoading = false;
  String? error;

  bool get isLoggedIn => username != null;

  Future<bool> login(String username, String password) async {
    isLoading = true;
    error = null;
    notifyListeners();
    try {
      final loggedUsername = await _api.login(username, password);
      this.username = loggedUsername;
      return true;
    } catch (e) {
      error = e.toString().replaceFirst('Exception: ', '');
      return false;
    } finally {
      isLoading = false;
      notifyListeners();
    }
  }

  Future<bool> register(String username, String password) async {
    isLoading = true;
    error = null;
    notifyListeners();
    try {
      await _api.register(username, password);
      return await login(username, password);
    } catch (e) {
      error = e.toString().replaceFirst('Exception: ', '');
      isLoading = false;
      notifyListeners();
      return false;
    }
  }

  void logout() {
    username = null;
    ApiService.clearCredentials();
    notifyListeners();
  }
}
