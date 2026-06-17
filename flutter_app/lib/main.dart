import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import 'providers/profile_provider.dart';
import 'providers/watchlist_provider.dart';
import 'providers/leaderboard_provider.dart';
import 'providers/insights_provider.dart';
import 'screens/home_shell.dart';

void main() {
  runApp(const InstaMvpApp());
}

class InstaMvpApp extends StatelessWidget {
  const InstaMvpApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => ProfileProvider()),
        ChangeNotifierProvider(create: (_) => WatchlistProvider()),
        ChangeNotifierProvider(create: (_) => LeaderboardProvider()),
        ChangeNotifierProvider(create: (_) => InsightsProvider()),
      ],
      child: MaterialApp(
        title: 'InstaMVP',
        debugShowCheckedModeBanner: false,
        theme: ThemeData(
          colorSchemeSeed: Colors.deepPurple,
          useMaterial3: true,
        ),
        home: const HomeShell(),
      ),
    );
  }
}
