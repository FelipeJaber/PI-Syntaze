import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';

import '../models/profile.dart';
import '../models/post.dart';
import '../models/growth.dart';
import '../providers/profile_provider.dart';
import '../services/api_service.dart';
import '../utils/error_utils.dart';

class ProfileDetailScreen extends StatelessWidget {
  final Profile profile;

  const ProfileDetailScreen({super.key, required this.profile});

  String _fmt(int? n) {
    if (n == null) return '-';
    return NumberFormat.compact().format(n);
  }

  Future<Growth?> _safeGrowth(ApiService api, int profileId) async {
    try {
      return await api.getGrowth(profileId, days: 7);
    } catch (_) {
      return null;
    }
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.read<ProfileProvider>();
    final api = ApiService();

    return Scaffold(
      appBar: AppBar(title: Text('@${profile.username}')),
      body: FutureBuilder<List<dynamic>>(
        future: Future.wait([
          provider.loadPosts(profile.id),
          _safeGrowth(api, profile.id),
        ]),
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snapshot.hasError) {
            return Center(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(Icons.cloud_off, size: 40, color: Colors.grey),
                  const SizedBox(height: 8),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 24),
                    child: Text(friendlyError(snapshot.error!), textAlign: TextAlign.center),
                  ),
                ],
              ),
            );
          }

          final posts = (snapshot.data?[0] as List<Post>?) ?? [];
          final growth = snapshot.data?[1] as Growth?;

          return ListView(
            padding: const EdgeInsets.all(16),
            children: [
              Text(
                profile.fullName ?? profile.username,
                style: Theme.of(context).textTheme.headlineSmall,
              ),
              const SizedBox(height: 8),
              if (profile.bio != null && profile.bio!.isNotEmpty)
                Text(profile.bio!),
              const SizedBox(height: 16),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceAround,
                children: [
                  _stat('Seguidores', _fmt(profile.followers)),
                  _stat('Seguindo', _fmt(profile.following)),
                  _stat('Posts', _fmt(profile.postsCount)),
                ],
              ),
              if (growth != null) ...[
                const SizedBox(height: 16),
                _growthBanner(growth),
              ],
              const Divider(height: 32),
              if (posts.isEmpty)
                const Text('Nenhum post encontrado.')
              else
                ...posts.map(_postTile),
            ],
          );
        },
      ),
    );
  }

  Widget _growthBanner(Growth growth) {
    if (growth.insufficientData) {
      return Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: Colors.grey.shade200,
          borderRadius: BorderRadius.circular(8),
        ),
        child: const Text(
          'Histórico insuficiente ainda para calcular crescimento (aguarde mais ciclos do worker).',
          style: TextStyle(fontSize: 12, color: Colors.grey),
        ),
      );
    }

    final percent = growth.followersGrowthPercent ?? 0;
    final isUp = percent > 0;
    final color = isUp ? Colors.green : (percent < 0 ? Colors.red : Colors.grey);

    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Row(
        children: [
          Icon(isUp ? Icons.trending_up : Icons.trending_down, color: color),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              'Crescimento em ${growth.periodDays}d: ${percent > 0 ? '+' : ''}${percent.toStringAsFixed(2)}% '
              '(${growth.followersDelta != null && growth.followersDelta! > 0 ? '+' : ''}${growth.followersDelta ?? 0} seguidores)',
              style: TextStyle(color: color, fontWeight: FontWeight.bold, fontSize: 13),
            ),
          ),
        ],
      ),
    );
  }

  Widget _stat(String label, String value) {
    return Column(
      children: [
        Text(value, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
        Text(label, style: const TextStyle(color: Colors.grey)),
      ],
    );
  }

  Widget _postTile(Post post) {
    final dateStr = post.postDate != null
        ? DateFormat('dd/MM/yyyy').format(post.postDate!)
        : '-';
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      clipBehavior: Clip.antiAlias,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (post.imageUrl != null)
            AspectRatio(
              aspectRatio: 16 / 9,
              child: Image.network(
                post.imageUrl!,
                fit: BoxFit.cover,
                errorBuilder: (_, __, ___) => Container(
                  color: Colors.grey.shade300,
                  child: const Icon(Icons.image_not_supported, color: Colors.grey),
                ),
              ),
            ),
          Padding(
            padding: const EdgeInsets.all(12),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  post.caption?.isNotEmpty == true ? post.caption! : '(sem legenda)',
                  maxLines: 3,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: 8),
                Row(
                  children: [
                    const Icon(Icons.favorite, size: 16, color: Colors.red),
                    const SizedBox(width: 4),
                    Text('${post.likes ?? 0}'),
                    const SizedBox(width: 16),
                    const Icon(Icons.comment, size: 16, color: Colors.blue),
                    const SizedBox(width: 4),
                    Text('${post.comments ?? 0}'),
                    if (post.engagementRate != null) ...[
                      const SizedBox(width: 16),
                      Text(
                        '${(post.engagementRate! * 100).toStringAsFixed(2)}%',
                        style: const TextStyle(color: Colors.green, fontWeight: FontWeight.bold),
                      ),
                    ],
                    const Spacer(),
                    Text(dateStr, style: const TextStyle(color: Colors.grey)),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
