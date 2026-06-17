import 'dart:convert';
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:http/http.dart' as http;

import '../models/profile.dart';
import '../models/post.dart';
import '../models/scrape_target.dart';
import '../models/growth.dart';
import '../models/top_post.dart';
import '../models/hashtag.dart';

class ApiService {
  // Web (apresentação via `flutter run -d chrome`) e desktop/iOS simulator
  // falam direto com localhost. Só o emulador Android precisa do alias
  // especial 10.0.2.2 (ele não enxerga "localhost" do host).
  static final String baseUrl = kIsWeb ? 'http://localhost:8080/api' : 'http://10.0.2.2:8080/api';

  Future<List<Profile>> getProfiles() async {
    final response = await http.get(Uri.parse('$baseUrl/profiles'));
    _checkOk(response);
    final List<dynamic> data = jsonDecode(response.body);
    return data.map((e) => Profile.fromJson(e)).toList();
  }

  Future<Profile> getProfile(int id) async {
    final response = await http.get(Uri.parse('$baseUrl/profiles/$id'));
    _checkOk(response);
    return Profile.fromJson(jsonDecode(response.body));
  }

  Future<List<Post>> getPosts(int profileId) async {
    final response = await http.get(Uri.parse('$baseUrl/profiles/$profileId/posts'));
    _checkOk(response);
    final List<dynamic> data = jsonDecode(response.body);
    return data.map((e) => Post.fromJson(e)).toList();
  }

  Future<Growth> getGrowth(int profileId, {int days = 7}) async {
    final response = await http.get(Uri.parse('$baseUrl/profiles/$profileId/growth?days=$days'));
    _checkOk(response);
    return Growth.fromJson(jsonDecode(response.body));
  }

  Future<List<Growth>> getLeaderboard({int days = 7}) async {
    final response = await http.get(Uri.parse('$baseUrl/leaderboard?days=$days'));
    _checkOk(response);
    final List<dynamic> data = jsonDecode(response.body);
    return data.map((e) => Growth.fromJson(e)).toList();
  }

  Future<List<TopPost>> getTopPosts({int days = 1, int limit = 10}) async {
    final response = await http.get(Uri.parse('$baseUrl/insights/top-posts?days=$days&limit=$limit'));
    _checkOk(response);
    final List<dynamic> data = jsonDecode(response.body);
    return data.map((e) => TopPost.fromJson(e)).toList();
  }

  Future<List<Hashtag>> getTrendingHashtags({int days = 7, int limit = 20}) async {
    final response = await http.get(Uri.parse('$baseUrl/insights/hashtags?days=$days&limit=$limit'));
    _checkOk(response);
    final List<dynamic> data = jsonDecode(response.body);
    return data.map((e) => Hashtag.fromJson(e)).toList();
  }

  Future<Map<String, dynamic>> syncProfiles(List<String> usernames) async {
    final response = await http.post(
      Uri.parse('$baseUrl/scraper/sync'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({'usernames': usernames}),
    );
    _checkOk(response);
    return jsonDecode(response.body);
  }

  Future<List<ScrapeTarget>> getWatchlist() async {
    final response = await http.get(Uri.parse('$baseUrl/scraper/watchlist'));
    _checkOk(response);
    final List<dynamic> data = jsonDecode(response.body);
    return data.map((e) => ScrapeTarget.fromJson(e)).toList();
  }

  Future<ScrapeTarget> addToWatchlist(String username) async {
    final response = await http.post(
      Uri.parse('$baseUrl/scraper/watchlist'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({'username': username}),
    );
    _checkOk(response);
    return ScrapeTarget.fromJson(jsonDecode(response.body));
  }

  Future<ScrapeTarget> toggleWatchlist(String username) async {
    final response = await http.patch(Uri.parse('$baseUrl/scraper/watchlist/$username/toggle'));
    _checkOk(response);
    return ScrapeTarget.fromJson(jsonDecode(response.body));
  }

  Future<void> removeFromWatchlist(String username) async {
    final response = await http.delete(Uri.parse('$baseUrl/scraper/watchlist/$username'));
    _checkOk(response);
  }

  void _checkOk(http.Response response) {
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw Exception('API error ${response.statusCode}: ${response.body}');
    }
  }
}
