// Smoke test: garante que o app sobe sem travar e mostra a tela de login
// (já que ninguém está autenticado ao abrir o app pela primeira vez).

import 'package:flutter_test/flutter_test.dart';

import 'package:instamvp_app/main.dart';

void main() {
  testWidgets('App sobe e mostra a tela de login', (WidgetTester tester) async {
    await tester.pumpWidget(const InstaMvpApp());
    await tester.pump();

    expect(find.text('InstaMVP'), findsOneWidget);
    expect(find.text('Usuário'), findsOneWidget);
    expect(find.text('Senha'), findsOneWidget);
    expect(find.text('Entrar'), findsOneWidget);
    expect(find.text('Criar uma conta'), findsOneWidget);
  });
}
