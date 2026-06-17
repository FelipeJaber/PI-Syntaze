import 'package:flutter/material.dart';

import 'profile_list_screen.dart';
import 'leaderboard_screen.dart';
import 'insights_screen.dart';
import 'watchlist_screen.dart';

/// Navegação principal mobile-first: bottom navigation com 4 abas,
/// pensada pra rodar bem tanto em tela estreita (telefone) quanto em
/// `flutter run -d chrome` redimensionado pra simular mobile.
class HomeShell extends StatefulWidget {
  const HomeShell({super.key});

  @override
  State<HomeShell> createState() => _HomeShellState();
}

class _HomeShellState extends State<HomeShell> {
  int _index = 0;

  static const _screens = [
    ProfileListScreen(),
    LeaderboardScreen(),
    InsightsScreen(),
    WatchlistScreen(),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: IndexedStack(index: _index, children: _screens),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _index,
        onDestinationSelected: (i) => setState(() => _index = i),
        destinations: const [
          NavigationDestination(icon: Icon(Icons.people_outline), selectedIcon: Icon(Icons.people), label: 'Perfis'),
          NavigationDestination(icon: Icon(Icons.leaderboard_outlined), selectedIcon: Icon(Icons.leaderboard), label: 'Ranking'),
          NavigationDestination(icon: Icon(Icons.insights_outlined), selectedIcon: Icon(Icons.insights), label: 'Insights'),
          NavigationDestination(icon: Icon(Icons.playlist_add_check_outlined), selectedIcon: Icon(Icons.playlist_add_check), label: 'Watchlist'),
        ],
      ),
    );
  }
}
