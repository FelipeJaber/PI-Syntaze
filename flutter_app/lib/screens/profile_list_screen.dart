import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';

import '../providers/profile_provider.dart';
import 'profile_detail_screen.dart';

class ProfileListScreen extends StatefulWidget {
  const ProfileListScreen({super.key});

  @override
  State<ProfileListScreen> createState() => _ProfileListScreenState();
}

class _ProfileListScreenState extends State<ProfileListScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<ProfileProvider>().loadProfiles();
    });
  }

  Future<void> _showSyncDialog() async {
    final controller = TextEditingController();
    final result = await showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Sincronizar perfis'),
        content: TextField(
          controller: controller,
          decoration: const InputDecoration(
            hintText: 'nike, adidas, puma',
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancelar'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, controller.text),
            child: const Text('Sincronizar'),
          ),
        ],
      ),
    );

    if (result != null && result.trim().isNotEmpty) {
      final usernames = result.split(',').map((e) => e.trim()).where((e) => e.isNotEmpty).toList();
      if (!mounted) return;
      final provider = context.read<ProfileProvider>();
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Sincronizando...')),
      );
      try {
        await provider.syncProfiles(usernames);
      } catch (e) {
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Erro: $e')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<ProfileProvider>();

    return Scaffold(
      appBar: AppBar(title: const Text('Perfis')),
      floatingActionButton: FloatingActionButton(
        onPressed: _showSyncDialog,
        child: const Icon(Icons.sync),
      ),
      body: RefreshIndicator(
        onRefresh: () => provider.loadProfiles(),
        child: _buildBody(provider),
      ),
    );
  }

  Widget _buildBody(ProfileProvider provider) {
    if (provider.isLoading && provider.profiles.isEmpty) {
      return const Center(child: CircularProgressIndicator());
    }
    if (provider.error != null && provider.profiles.isEmpty) {
      return Center(child: Text('Erro: ${provider.error}'));
    }
    if (provider.profiles.isEmpty) {
      return const Center(child: Text('Nenhum perfil. Use o botão de sincronizar.'));
    }

    return ListView.separated(
      itemCount: provider.profiles.length,
      separatorBuilder: (_, __) => const Divider(height: 1),
      itemBuilder: (context, index) {
        final profile = provider.profiles[index];
        return ListTile(
          leading: const CircleAvatar(child: Icon(Icons.person)),
          title: Text('@${profile.username}'),
          subtitle: Text(
            '${NumberFormat.compact().format(profile.followers ?? 0)} seguidores · ${profile.postsCount ?? 0} posts',
          ),
          trailing: const Icon(Icons.chevron_right),
          onTap: () {
            Navigator.push(
              context,
              MaterialPageRoute(builder: (_) => ProfileDetailScreen(profile: profile)),
            );
          },
        );
      },
    );
  }
}
