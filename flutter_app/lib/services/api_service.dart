import 'dart:async';
import 'dart:convert';
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:http/http.dart' as http;

import '../models/profile.dart';
import '../models/post.dart';
import '../models/scrape_target.dart';
import '../models/growth.dart';
import '../models/top_post.dart';
import '../models/hashtag.dart';

/// Lançada quando o backend responde 401 numa chamada já autenticada
/// (sessão "expirou" — credenciais ficaram inválidas, ex: backend reiniciou
/// o banco). Tratada de forma especial pelas telas para voltar ao login.
class UnauthorizedException implements Exception {
  final String message;
  UnauthorizedException([this.message = 'Sessão expirada. Faça login novamente.']);
  @override
  String toString() => message;
}

class ApiService {
  // Web (apresentação via `flutter run -d chrome`) e desktop/iOS simulator
  // falam direto com localhost. Só o emulador Android precisa do alias
  // especial 10.0.2.2 (ele não enxerga "localhost" do host).
  static const String baseUrl = kIsWeb ? 'http://localhost:8080/api' : 'http://10.0.2.2:8080/api';

  // Credenciais do usuário logado (definidas pelo AuthProvider após
  // login/registro bem-sucedido). O backend usa HTTP Basic — não existe
  // token de sessão, então guardamos usuário/senha em memória e os
  // reenviamos em toda chamada autenticada.
  static String? _username;
  static String? _password;

  /// Chamado sempre que uma chamada autenticada recebe 401 (sessão perdida).
  /// `main.dart` conecta isso ao AuthProvider.logout() para voltar à tela de
  /// login automaticamente em qualquer lugar do app, sem cada tela precisar
  /// tratar isso na mão.
  static void Function()? onUnauthorized;

  static void setCredentials(String username, String password) {
    _username = username;
    _password = password;
  }

  static void clearCredentials() {
    _username = null;
    _password = null;
  }

  static bool get hasCredentials => _username != null && _password != null;

  Map<String, String> get _authHeaders {
    if (_username == null || _password == null) {
      throw StateError('Nenhum usuário logado.');
    }
    return {
      'Authorization': 'Basic ${base64Encode(utf8.encode('$_username:$_password'))}',
    };
  }

  Map<String, String> get _jsonAuthHeaders => {
        ..._authHeaders,
        'Content-Type': 'application/json',
      };

  /// Cria uma nova conta. Não loga automaticamente — chame [login] em seguida.
  Future<void> register(String username, String password) async {
    final response = await _send(() => http.post(
          Uri.parse('$baseUrl/auth/register'),
          headers: {'Content-Type': 'application/json'},
          body: jsonEncode({'username': username, 'password': password}),
        ));
    if (response.statusCode != 201) {
      String message = 'Erro ao cadastrar (HTTP ${response.statusCode})';
      try {
        final body = jsonDecode(response.body);
        if (body is Map && body['error'] != null) message = body['error'];
      } catch (_) {}
      throw Exception(message);
    }
  }

  /// Valida usuário/senha contra o backend (GET /api/auth/me com HTTP Basic).
  /// Se as credenciais forem válidas, ficam salvas para as próximas chamadas.
  Future<String> login(String username, String password) async {
    final auth = 'Basic ${base64Encode(utf8.encode('$username:$password'))}';
    final response = await _send(() => http.get(
          Uri.parse('$baseUrl/auth/me'),
          headers: {'Authorization': auth},
        ));
    if (response.statusCode == 401) {
      throw Exception('Usuário ou senha inválidos.');
    }
    _checkOk(response, isAuthCall: true);
    setCredentials(username, password);
    final body = jsonDecode(response.body);
    return body['username'] as String;
  }

  Future<List<Profile>> getProfiles() async {
    final response = await _send(() => http.get(Uri.parse('$baseUrl/profiles'), headers: _authHeaders));
    _checkOk(response);
    final List<dynamic> data = jsonDecode(response.body);
    return data.map((e) => Profile.fromJson(e)).toList();
  }

  Future<Profile> getProfile(int id) async {
    final response = await _send(() => http.get(Uri.parse('$baseUrl/profiles/$id'), headers: _authHeaders));
    _checkOk(response);
    return Profile.fromJson(jsonDecode(response.body));
  }

  Future<List<Post>> getPosts(int profileId) async {
    final response =
        await _send(() => http.get(Uri.parse('$baseUrl/profiles/$profileId/posts'), headers: _authHeaders));
    _checkOk(response);
    final List<dynamic> data = jsonDecode(response.body);
    return data.map((e) => Post.fromJson(e)).toList();
  }

  Future<Growth> getGrowth(int profileId, {int days = 7}) async {
    final response = await _send(
        () => http.get(Uri.parse('$baseUrl/profiles/$profileId/growth?days=$days'), headers: _authHeaders));
    _checkOk(response);
    return Growth.fromJson(jsonDecode(response.body));
  }

  Future<List<Growth>> getLeaderboard({int days = 7}) async {
    final response = await _send(() => http.get(Uri.parse('$baseUrl/leaderboard?days=$days'), headers: _authHeaders));
    _checkOk(response);
    final List<dynamic> data = jsonDecode(response.body);
    return data.map((e) => Growth.fromJson(e)).toList();
  }

  Future<List<TopPost>> getTopPosts({int days = 1, int limit = 10}) async {
    final response = await _send(
        () => http.get(Uri.parse('$baseUrl/insights/top-posts?days=$days&limit=$limit'), headers: _authHeaders));
    _checkOk(response);
    final List<dynamic> data = jsonDecode(response.body);
    return data.map((e) => TopPost.fromJson(e)).toList();
  }

  Future<List<Hashtag>> getTrendingHashtags({int days = 7, int limit = 20}) async {
    final response = await _send(
        () => http.get(Uri.parse('$baseUrl/insights/hashtags?days=$days&limit=$limit'), headers: _authHeaders));
    _checkOk(response);
    final List<dynamic> data = jsonDecode(response.body);
    return data.map((e) => Hashtag.fromJson(e)).toList();
  }

  Future<Map<String, dynamic>> syncProfiles(List<String> usernames) async {
    final response = await _send(() => http.post(
          Uri.parse('$baseUrl/scraper/sync'),
          headers: _jsonAuthHeaders,
          body: jsonEncode({'usernames': usernames}),
        ));
    _checkOk(response);
    return jsonDecode(response.body);
  }

  Future<List<ScrapeTarget>> getWatchlist() async {
    final response = await _send(() => http.get(Uri.parse('$baseUrl/scraper/watchlist'), headers: _authHeaders));
    _checkOk(response);
    final List<dynamic> data = jsonDecode(response.body);
    return data.map((e) => ScrapeTarget.fromJson(e)).toList();
  }

  Future<ScrapeTarget> addToWatchlist(String username) async {
    final response = await _send(() => http.post(
          Uri.parse('$baseUrl/scraper/watchlist'),
          headers: _jsonAuthHeaders,
          body: jsonEncode({'username': username}),
        ));
    _checkOk(response);
    return ScrapeTarget.fromJson(jsonDecode(response.body));
  }

  Future<ScrapeTarget> toggleWatchlist(String username) async {
    final response = await _send(
        () => http.patch(Uri.parse('$baseUrl/scraper/watchlist/$username/toggle'), headers: _authHeaders));
    _checkOk(response);
    return ScrapeTarget.fromJson(jsonDecode(response.body));
  }

  Future<void> removeFromWatchlist(String username) async {
    final response =
        await _send(() => http.delete(Uri.parse('$baseUrl/scraper/watchlist/$username'), headers: _authHeaders));
    _checkOk(response);
  }

  /// Executa a requisição traduzindo falhas de rede/timeout em mensagens
  /// amigáveis, em vez de deixar vazar exceções técnicas (SocketException,
  /// ClientException, TimeoutException) até a UI.
  Future<http.Response> _send(Future<http.Response> Function() request) async {
    try {
      return await request().timeout(const Duration(seconds: 15));
    } on TimeoutException {
      throw Exception('O servidor demorou demais para responder. Tente novamente.');
    } on http.ClientException {
      // Cobre tanto "connection refused" (mobile/desktop) quanto falha de
      // fetch no navegador (web) — o tipo de exceção exata varia por
      // plataforma, mas o http package sempre embrulha em ClientException.
      throw Exception('Não foi possível conectar ao servidor. Verifique se o backend está rodando em $baseUrl.');
    }
  }

  void _checkOk(http.Response response, {bool isAuthCall = false}) {
    if (response.statusCode == 401 && !isAuthCall) {
      onUnauthorized?.call();
      throw UnauthorizedException();
    }
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw Exception('Erro do servidor (HTTP ${response.statusCode}).');
    }
  }
}
