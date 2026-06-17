/// Converte qualquer erro capturado em uma mensagem amigável para exibir na
/// UI, removendo o prefixo técnico "Exception: " que o Dart adiciona ao
/// chamar toString() em exceções genéricas.
String friendlyError(Object error) {
  return error.toString().replaceFirst('Exception: ', '');
}
