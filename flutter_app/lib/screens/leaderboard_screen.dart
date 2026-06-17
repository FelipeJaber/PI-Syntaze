import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';

import '../models/growth.dart';
import '../providers/leaderboard_provider.dart';

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
      body: RefreshIndicator(
        onRefresh: () => provider.load(),
        child: _buildBody(provider),
      ),
    );
  }

  Widget _buildBody(LeaderboardProvider provider) {
    if (provider.isLoading && provider.entries.isEmpty) {
      return const Center(child: CircularProgressIndicator());
    }
    if (provider.error != null && provider.entries.isEmpty) {
      return Center(child: Text('Erro: ${provider.error}'));
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
  final String Function(num?) fmtCompact;
  final String Function(double?) fmtPercent;
  final String Function(double?) fmtRate;

  const _LeaderboardCard({
    required this.growth,
    required this.fmtCompact,
    required this.fmtPercent,
    required this.fmtRate,
  });

  @override
  Widget build(BuildContext context) {
    final isUp = (growth.followersGrowthPercent ?? 0) > 0;
    final color = growth.insufficientData
        ? Colors.grey
        : (isUp ? Colors.green : Colors.red);

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
                growth.insufficientData ? 'sem histórico' : fmtPercent(growth.followersGrowthPercent),
                style: TextStyle(color: color, fontWeight: FontWeight.bold, fontSize: 12),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
