import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';

import '../models/growth.dart';
import '../providers/leaderboard_provider.dart';

const Map<String, String> _sortLabels = {
  'growth': 'Crescimento',
  'likes': 'Mais curtidos',
  'engagement': 'Melhor engajamento (retenção)',
  'activity': 'Mais ativos',
};

class LeaderboardScreen extends StatefulWidget {
  const LeaderboardScreen({super.key});

  @override
  State<LeaderboardScreen> createState() => _LeaderboardScreenState();
}

class _LeaderboardScreenState extends State<LeaderboardScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<LeaderboardProvider>().load();
    });
  }

  String _fmtCompact(num? n) {
    if (n == null) return '—';
    return NumberFormat.compact().format(n);
  }

  String _fmtPercent(double? n) {
    if (n == null) return '—';
    final sign = n > 0 ? '+' : '';
    return '$sign${n.toStringAsFixed(2)}%';
  }

  String _fmtRate(double? n) {
    if (n == null) return '—';
    return '${(n * 100).toStringAsFixed(2)}%';
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<LeaderboardProvider>();

    return Scaffold(
      appBar: AppBar(
        title: const Text('Leaderboard'),
        actions: [
          PopupMenuButton<String>(
            initialValue: provider.sort,
            tooltip: 'Critério de ordenação',
            onSelected: (sort) => provider.load(sort: sort),
            itemBuilder: (context) => _sortLabels.entries
                .map((e) => PopupMenuItem(value: e.key, child: Text(e.value)))
                .toList(),
            child: const Padding(
              padding: EdgeInsets.symmetric(horizontal: 8),
              child: Icon(Icons.sort),
            ),
          ),
          PopupMenuButton<int>(
            initialValue: provider.days,
            onSelected: (days) => provider.load(days: days),
            itemBuilder: (context) => const [
              PopupMenuItem(value: 1, child: Text('1 dia')),
              PopupMenuItem(value: 7, child: Text('7 dias')),
              PopupMenuItem(value: 30, child: Text('30 dias')),
            ],
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 12),
              child: Center(child: Text('${provider.days}d')),
            ),
          ),
        ],
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
            child: Align(
              alignment: Alignment.centerLeft,
              child: Text(
                'Ordenado por: ${_sortLabels[provider.sort]}',
                style: const TextStyle(fontSize: 12, color: Colors.grey),
              ),
            ),
          ),
          Expanded(
            child: RefreshIndicator(
              onRefresh: () => provider.load(),
              child: _buildBody(provider),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBody(LeaderboardProvider provider) {
    if (provider.isLoading && provider.entries.isEmpty) {
      return const Center(child: CircularProgressIndicator());
    }
    if (provider.error != null && provider.entries.isEmpty) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.cloud_off, size: 40, color: Colors.grey),
            const SizedBox(height: 8),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 24),
              child: Text(provider.error!, textAlign: TextAlign.center),
            ),
            const SizedBox(height: 12),
            OutlinedButton(onPressed: () => provider.load(), child: const Text('Tentar novamente')),
          ],
        ),
      );
    }
    if (provider.entries.isEmpty) {
      return const Center(child: Text('Nenhum perfil monitorado ainda.'));
    }

    return ListView.separated(
      padding: const EdgeInsets.all(12),
      itemCount: provider.entries.length,
      separatorBuilder: (_, __) => const SizedBox(height: 8),
      itemBuilder: (context, index) {
        final g = provider.entries[index];
        return _LeaderboardCard(
          growth: g,
          sort: provider.sort,
          fmtCompact: _fmtCompact,
          fmtPercent: _fmtPercent,
          fmtRate: _fmtRate,
        );
      },
    );
  }
}

class _LeaderboardCard extends StatelessWidget {
  final Growth growth;
  final String sort;
  final String Function(num?) fmtCompact;
  final String Function(double?) fmtPercent;
  final String Function(double?) fmtRate;

  const _LeaderboardCard({
    required this.growth,
    required this.sort,
    required this.fmtCompact,
    required this.fmtPercent,
    required this.fmtRate,
  });

  /// Texto + cor do badge de destaque, de acordo com o critério de ordenação ativo.
  (String, Color) _metricBadge() {
    switch (sort) {
      case 'likes':
        return ('${fmtCompact(growth.totalLikesInPeriod)} curtidas', Colors.pink);
      case 'engagement':
        return (fmtRate(growth.avgEngagementRate), Colors.green);
      case 'activity':
        return ('${growth.postsInPeriod ?? 0} posts', Colors.blue);
      case 'growth':
      default:
        if (growth.insufficientData) return ('sem histórico', Colors.grey);
        final percent = growth.followersGrowthPercent ?? 0;
        return (fmtPercent(growth.followersGrowthPercent), percent >= 0 ? Colors.green : Colors.red);
    }
  }

  @override
  Widget build(BuildContext context) {
    final (badgeText, color) = _metricBadge();

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Row(
          children: [
            CircleAvatar(
              radius: 16,
              child: Text('${growth.rank ?? '-'}', style: const TextStyle(fontSize: 12)),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('@${growth.username}', style: const TextStyle(fontWeight: FontWeight.bold)),
                  const SizedBox(height: 2),
                  Text(
                    '${fmtCompact(growth.followersEnd)} seguidores · engaj. médio ${fmtRate(growth.avgEngagementRate)}',
                    style: const TextStyle(fontSize: 12, color: Colors.grey),
                  ),
                ],
              ),
            ),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
              decoration: BoxDecoration(
                color: color.withValues(alpha: 0.15),
                borderRadius: BorderRadius.circular(999),
              ),
              child: Text(
                badgeText,
                style: TextStyle(color: color, fontWeight: FontWeight.bold, fontSize: 12),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
