import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../models/scrape_target.dart';
import '../providers/watchlist_provider.dart';

class WatchlistScreen extends StatefulWidget {
  const WatchlistScreen({super.key});

  @override
  State<WatchlistScreen> createState() => _WatchlistScreenState();
}

class _WatchlistScreenState extends State<WatchlistScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<WatchlistProvider>().load();
    });
  }

  Future<void> _showAddDialog() async {
    final controller = TextEditingController();
    final username = await showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Adicionar perfil à watchlist'),
        content: TextField(
          controller: controller,
          decoration: const InputDecoration(hintText: 'username (sem @)'),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancelar'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, controller.text),
            child: const Text('Adicionar'),
          ),
        ],
      ),
    );

    if (username != null && username.trim().isNotEmpty) {
      if (!mounted) return;
      try {
        await context.read<WatchlistProvider>().add(username.trim());
      } catch (e) {
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Erro: $e')));
      }
    }
  }

  Future<void> _confirmRemove(ScrapeTarget target) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Remover perfil'),
        content: Text('Remover @${target.username} da watchlist?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Cancelar')),
          TextButton(onPressed: () => Navigator.pop(context, true), child: const Text('Remover')),
        ],
      ),
    );

    if (confirmed == true) {
      if (!mounted) return;
      try {
        await context.read<WatchlistProvider>().remove(target);
      } catch (e) {
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Erro: $e')));
      }
    }
  }

  Widget _statusBadge(String status) {
    final map = {
      'PENDING': (Colors.grey, 'pendente'),
      'OK': (Colors.green, 'ok'),
      'PRIVATE': (Colors.grey, 'privado'),
      'NOT_FOUND': (Colors.red, 'não encontrado'),
      'ERROR': (Colors.red, 'erro'),
    };
    final (color, label) = map[status] ?? map['PENDING']!;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.15),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(label, style: TextStyle(color: color, fontSize: 11, fontWeight: FontWeight.bold)),
    );
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<WatchlistProvider>();

    return Scaffold(
      appBar: AppBar(title: const Text('Watchlist (busca contínua)')),
      floatingActionButton: FloatingActionButton(
        onPressed: _showAddDialog,
        child: const Icon(Icons.add),
      ),
      body: RefreshIndicator(
        onRefresh: () => provider.load(),
        child: _buildBody(provider),
      ),
    );
  }

  Widget _buildBody(WatchlistProvider provider) {
    if (provider.isLoading && provider.targets.isEmpty) {
      return const Center(child: CircularProgressIndicator());
    }
    if (provider.error != null && provider.targets.isEmpty) {
      return Center(child: Text('Erro: ${provider.error}'));
    }
    if (provider.targets.isEmpty) {
      return const Center(child: Text('Nenhum perfil na watchlist. Toque em + para adicionar.'));
    }

    return ListView.separated(
      itemCount: provider.targets.length,
      separatorBuilder: (_, __) => const Divider(height: 1),
      itemBuilder: (context, index) {
        final target = provider.targets[index];
        return ListTile(
          leading: Icon(
            target.active ? Icons.visibility : Icons.visibility_off,
            color: target.active ? Colors.green : Colors.grey,
          ),
          title: Text('@${target.username}'),
          subtitle: Row(
            children: [
              Text(target.active ? 'Busca ativa' : 'Busca pausada'),
              const SizedBox(width: 8),
              _statusBadge(target.lastStatus),
            ],
          ),
          trailing: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Switch(
                value: target.active,
                onChanged: (_) => provider.toggle(target),
              ),
              IconButton(
                icon: const Icon(Icons.delete_outline),
                onPressed: () => _confirmRemove(target),
              ),
            ],
          ),
        );
      },
    );
  }
}
