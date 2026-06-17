// Smoke test: garante que o app sobe sem travar e mostra a navegação
// principal (bottom navigation com as 4 abas), mesmo sem backend disponível
// durante o teste (as chamadas de API falham silenciosamente e ficam em
// estado de erro/loading, o que já é o comportamento esperado da UI).

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:instamvp_app/main.dart';

void main() {
  testWidgets('App sobe e mostra a navegação principal', (WidgetTester tester) async {
    await tester.pumpWidget(const InstaMvpApp());
    await tester.pump();

    expect(find.byType(NavigationBar), findsOneWidget);
    expect(find.text('Perfis'), findsWidgets);
    expect(find.text('Ranking'), findsOneWidget);
    expect(find.text('Insights'), findsOneWidget);
    expect(find.text('Watchlist'), findsOneWidget);
  });
}
