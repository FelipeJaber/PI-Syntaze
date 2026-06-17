import 'package:flutter/foundation.dart';

import '../services/api_service.dart';

class AuthProvider extends ChangeNotifier {
  final ApiService _api = ApiService();

  String? username;
  bool isLoading = false;
  String? error;

  /// Mensagem de "sessão expirada" para a LoginScreen exibir uma única vez
  /// (ex: quando o backend reinicia o banco e as credenciais antigas param
  /// de valer). Consumida via [consumeSessionMessage].
  String? sessionMessage;

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

  /// Logout forçado por uma resposta 401 inesperada (sessão perdida) em vez
  /// de uma ação explícita do usuário — mostra um aviso na tela de login.
  void forceLogout([String message = 'Sessão expirada. Faça login novamente.']) {
    sessionMessage = message;
    logout();
  }

  String? consumeSessionMessage() {
    final msg = sessionMessage;
    sessionMessage = null;
    return msg;
  }
}
