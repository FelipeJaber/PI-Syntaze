import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';

import '../models/top_post.dart';
import '../providers/insights_provider.dart';

class InsightsScreen extends StatefulWidget {
  const InsightsScreen({super.key});

  @override
  State<InsightsScreen> createState() => _InsightsScreenState();
}

class _InsightsScreenState extends State<InsightsScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<InsightsProvider>().load();
    });
  }

  String _fmtCompact(num? n) {
    if (n == null) return '—';
    return NumberFormat.compact().format(n);
  }

  String _fmtRate(double? n) {
    if (n == null) return '—';
    return '${(n * 100).toStringAsFixed(2)}%';
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<InsightsProvider>();

    return Scaffold(
      appBar: AppBar(title: const Text('Inteligência de conteúdo')),
      body: RefreshIndicator(
        onRefresh: () => provider.load(),
        child: provider.isLoading && provider.topPosts.isEmpty
            ? const Center(child: CircularProgressIndicator())
            : ListView(
                padding: const EdgeInsets.all(12),
                children: [
                  _SectionHeader(
                    title: '🔥 Melhores posts',
                    trailing: PopupMenuButton<int>(
                      initialValue: provider.topPostsDays,
                      onSelected: (days) => provider.load(topPostsDays: days),
                      itemBuilder: (context) => const [
                        PopupMenuItem(value: 1, child: Text('hoje')),
                        PopupMenuItem(value: 7, child: Text('7 dias')),
                        PopupMenuItem(value: 30, child: Text('30 dias')),
                      ],
                      child: const Padding(
                        padding: EdgeInsets.symmetric(horizontal: 4),
                        child: Icon(Icons.tune, size: 18),
                      ),
                    ),
                  ),
                  if (provider.topPosts.isEmpty)
                    const Padding(
                      padding: EdgeInsets.symmetric(vertical: 12),
                      child: Text('Nenhum post no período.', style: TextStyle(color: Colors.grey)),
                    )
                  else
                    SizedBox(
                      height: 230,
                      child: ListView.separated(
                        scrollDirection: Axis.horizontal,
                        itemCount: provider.topPosts.length,
                        separatorBuilder: (_, __) => const SizedBox(width: 10),
                        itemBuilder: (context, index) => _TopPostCard(
                          post: provider.topPosts[index],
                          fmtCompact: _fmtCompact,
                          fmtRate: _fmtRate,
                        ),
                      ),
                    ),
                  const SizedBox(height: 24),
                  _SectionHeader(
                    title: '# Hashtags em alta',
                    trailing: PopupMenuButton<int>(
                      initialValue: provider.hashtagsDays,
                      onSelected: (days) => provider.load(hashtagsDays: days),
                      itemBuilder: (context) => const [
                        PopupMenuItem(value: 7, child: Text('7 dias')),
                        PopupMenuItem(value: 30, child: Text('30 dias')),
                      ],
                      child: const Padding(
                        padding: EdgeInsets.symmetric(horizontal: 4),
                        child: Icon(Icons.tune, size: 18),
                      ),
                    ),
                  ),
                  if (provider.hashtags.isEmpty)
                    const Padding(
                      padding: EdgeInsets.symmetric(vertical: 12),
                      child: Text('Nenhuma hashtag encontrada no período.', style: TextStyle(color: Colors.grey)),
                    )
                  else
                    Wrap(
                      spacing: 8,
                      runSpacing: 8,
                      children: provider.hashtags.map((h) {
                        return Tooltip(
                          message: h.usernames.map((u) => '@$u').join(', '),
                          child: Chip(
                            label: Text('${h.tag} · ${h.postCount} post(s) · ${h.profileCount} perfil(is)'),
                          ),
                        );
                      }).toList(),
                    ),
                ],
              ),
      ),
    );
  }
}

class _SectionHeader extends StatelessWidget {
  final String title;
  final Widget trailing;

  const _SectionHeader({required this.title, required this.trailing});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(title, style: Theme.of(context).textTheme.titleMedium),
          trailing,
        ],
      ),
    );
  }
}

class _TopPostCard extends StatelessWidget {
  final TopPost post;
  final String Function(num?) fmtCompact;
  final String Function(double?) fmtRate;

  const _TopPostCard({required this.post, required this.fmtCompact, required this.fmtRate});

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 160,
      child: Card(
        clipBehavior: Clip.antiAlias,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            AspectRatio(
              aspectRatio: 1,
              child: post.imageUrl != null
                  ? Image.network(
                      post.imageUrl!,
                      fit: BoxFit.cover,
                      errorBuilder: (_, __, ___) => Container(
                        color: Colors.grey.shade300,
                        child: const Icon(Icons.image_not_supported, color: Colors.grey),
                      ),
                    )
                  : Container(
                      color: Colors.grey.shade300,
                      child: const Icon(Icons.image_not_supported, color: Colors.grey),
                    ),
            ),
            Padding(
              padding: const EdgeInsets.all(8),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('@${post.username}', style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 12)),
                  const SizedBox(height: 4),
                  Row(
                    children: [
                      const Icon(Icons.favorite, size: 12, color: Colors.red),
                      const SizedBox(width: 2),
                      Text(fmtCompact(post.likes), style: const TextStyle(fontSize: 11)),
                      const SizedBox(width: 8),
                      Text(fmtRate(post.engagementRate),
                          style: const TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: Colors.green)),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
