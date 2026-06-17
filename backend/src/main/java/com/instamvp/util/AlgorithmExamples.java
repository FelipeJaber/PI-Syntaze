package com.instamvp.util;

import com.instamvp.model.Profile;

import java.util.*;

/**
 * Conjunto de exemplos de funções com análise de complexidade (contagem de passos),
 * adicionado como material para a disciplina "Análise e Projeto de Algoritmos".
 * Cada método contém comentário com a contagem de operações, a fórmula T(n) e a
 * complexidade assintótica resultante.
 *
 * Estas funções operam sobre {@link Profile} (a entidade real do projeto) e usam
 * apenas estruturas de dados do Java padrão — não dependem de banco, Spring nem
 * de nenhum outro componente do sistema, então podem ser usadas isoladamente
 * como exercício de análise sem qualquer efeito colateral no resto do projeto.
 */
public final class AlgorithmExamples {

    private AlgorithmExamples() {
    }

    /**
     * 1) Busca linear por username em uma lista de perfis.
     *
     * Contagem de passos (pior caso — username não está na lista, ou está no
     * último elemento):
     * - Verificação de nulidade: 1 passo (constante, não depende de n)
     * - Laço executa n vezes (n = profiles.size())
     *   - cada iteração: 1 acesso (get) + 1 comparação (equals) = 2 passos
     * - Retorno final: 1 passo
     *
     * T(n) = 1 + 2n + 1 = 2n + 2
     * Desprezando constantes e termos de menor ordem: T(n) = O(n).
     */
    public static Profile linearSearchProfileByUsername(List<Profile> profiles, String username) {
        if (profiles == null || username == null) return null; // O(1)
        for (int i = 0; i < profiles.size(); i++) { // loop executa n vezes no pior caso
            Profile p = profiles.get(i); // O(1)
            if (username.equals(p.getUsername())) { // comparação O(1)
                return p; // retorna quando encontrar
            }
        }
        return null; // não encontrado
    }

    /**
     * 2) Busca binária em uma lista de perfis ordenada por username.
     *
     * Pré-condição: {@code sortedProfiles} já está ordenada por username (ascendente).
     *
     * Contagem de passos: a cada iteração do laço, o espaço de busca é dividido
     * pela metade (lo/hi se aproximam). O número de iterações no pior caso é
     * ⌊log2(n)⌋ + 1. Cada iteração faz um número constante de operações
     * (calcular o meio, um acesso, uma comparação): c passos.
     *
     * T(n) = c · (⌊log2(n)⌋ + 1) = c·log2(n) + c
     * Desprezando constantes: T(n) = O(log n).
     */
    public static Profile binarySearchProfileByUsername(List<Profile> sortedProfiles, String username) {
        if (sortedProfiles == null || username == null) return null;
        int lo = 0, hi = sortedProfiles.size() - 1;
        while (lo <= hi) {
            int mid = lo + ((hi - lo) / 2);
            Profile p = sortedProfiles.get(mid);
            int cmp = username.compareTo(p.getUsername());
            if (cmp == 0) return p;
            if (cmp < 0) hi = mid - 1; else lo = mid + 1;
        }
        return null;
    }

    /**
     * 3) Remove usernames duplicados de uma lista, preservando a ordem de
     * primeira aparição.
     *
     * Técnica: usa uma tabela hash com ordem de inserção ({@link LinkedHashSet}),
     * que garante inserção/verificação de existência em O(1) amortizado.
     *
     * Contagem de passos:
     * - Criação do conjunto: 1 passo
     * - Laço executa n vezes (n = usernames.size()); cada iteração faz 1
     *   inserção O(1) amortizado
     * - Conversão final para List: percorre os k elementos únicos (k ≤ n): O(k)
     *
     * T(n) = 1 + n·1 + k, onde k ≤ n ⟹ T(n) ≤ 1 + 2n
     * T(n) = O(n).
     */
    public static List<String> deduplicateUsernamesPreserveOrder(List<String> usernames) {
        if (usernames == null) return Collections.emptyList();
        LinkedHashSet<String> set = new LinkedHashSet<>(); // O(1)
        for (String u : usernames) { // n iterações
            set.add(u); // O(1) amortizado
        }
        return new ArrayList<>(set); // converte para lista: O(k), k ≤ n
    }

    /**
     * 4) Ordena perfis por número de seguidores, decrescente.
     *
     * Usa {@code List.sort} (TimSort, híbrido de merge sort + insertion sort)
     * internamente — complexidade O(n log n) tanto no caso médio quanto no
     * pior caso.
     *
     * Contagem de passos (alto nível, já que o algoritmo de ordenação em si
     * não é implementado aqui, apenas invocado):
     * - Cópia da lista: percorre os n elementos: O(n)
     * - Ordenação (TimSort): O(n log n) comparações no pior caso
     *
     * T(n) = a·n + b·n·log(n) ⟹ o termo n·log(n) domina assintoticamente
     * T(n) = O(n log n).
     */
    public static List<Profile> sortProfilesByFollowersDesc(List<Profile> profiles) {
        if (profiles == null) return Collections.emptyList();
        List<Profile> copy = new ArrayList<>(profiles); // O(n)
        copy.sort(Comparator.comparing((Profile p) -> p.getFollowers() == null ? 0L : p.getFollowers()).reversed()); // O(n log n)
        return copy;
    }

    /**
     * 5) Top-K perfis por seguidores, usando um min-heap de tamanho k
     * (técnica de seleção via heap, mais eficiente que ordenar a lista
     * inteira quando k ≪ n).
     *
     * Contagem de passos: mantemos um min-heap de tamanho no máximo k. Para
     * cada um dos n perfis fazemos no pior caso uma inserção e possivelmente
     * uma remoção, cada operação de heap custando O(log k):
     * - Laço externo: n iterações
     *   - cada iteração: no máximo 1 poll + 1 offer = O(log k)
     * - Ordenação final do resultado (apenas k elementos): O(k log k)
     *
     * T(n,k) = n·log(k) + k·log(k)
     * Como k ≤ n, o termo n·log(k) domina: T(n,k) = O(n log k).
     * (Quando k é uma fração pequena e constante de n, isso é melhor que
     * ordenar tudo com O(n log n).)
     */
    public static List<Profile> topKProfilesByFollowers(List<Profile> profiles, int k) {
        if (profiles == null || k <= 0) return Collections.emptyList();
        PriorityQueue<Profile> minHeap = new PriorityQueue<>(
                Comparator.comparing((Profile p) -> p.getFollowers() == null ? 0L : p.getFollowers()));
        for (Profile p : profiles) {
            long followers = p.getFollowers() == null ? 0L : p.getFollowers();
            if (minHeap.size() < k) {
                minHeap.offer(p); // O(log k)
            } else {
                long smallestInHeap = minHeap.peek().getFollowers() == null ? 0L : minHeap.peek().getFollowers();
                if (followers > smallestInHeap) {
                    minHeap.poll(); // O(log k)
                    minHeap.offer(p); // O(log k)
                }
            }
        }
        List<Profile> result = new ArrayList<>(minHeap);
        // ordena o resultado final (apenas k elementos) em ordem decrescente: O(k log k)
        result.sort(Comparator.comparing((Profile p) -> p.getFollowers() == null ? 0L : p.getFollowers()).reversed());
        return result;
    }

    // -------------------------------------------------------------------
    // Item (b): técnica de projeto de algoritmos aplicada no projeto real
    // -------------------------------------------------------------------
    /**
     * Técnica aplicada de fato no projeto (não um exemplo isolado, como os
     * métodos acima — esta é uma nota sobre código já existente e em uso):
     * <b>busca recursiva em profundidade (DFS) com retrocesso (backtracking)</b>,
     * implementada em
     * {@code com.instamvp.service.ScraperService#findNodeWithField(JsonNode, String)}.
     *
     * <p>O Instagram não documenta o formato exato do JSON embutido na página
     * pública de um perfil, e a posição de campos como {@code edge_followed_by}
     * varia conforme a versão do layout. Em vez de fixar um caminho exato no
     * JSON, o método percorre toda a árvore recursivamente, em profundidade,
     * até localizar o campo desejado:
     * <ol>
     *   <li>Caso base / poda de ramo: nó nulo ou ausente → retorna {@code null}
     *       imediatamente, sem explorar aquele ramo;</li>
     *   <li>Caso de sucesso: o nó atual já contém o campo → retorna esse nó,
     *       sem precisar visitar os demais ramos da árvore;</li>
     *   <li>Caso recursivo com retrocesso: itera sobre os filhos do nó atual,
     *       chamando a si mesma recursivamente em cada um; se um filho
     *       retornar um resultado não nulo, esse resultado é propagado para
     *       cima na pilha de chamadas. Caso contrário, a busca retrocede
     *       (backtrack) e tenta o próximo filho-irmão.</li>
     * </ol>
     *
     * <p>Complexidade: seja n o número total de nós da árvore JSON. No pior
     * caso (campo ausente, ou presente apenas no último nó visitado), todos
     * os n nós são visitados exatamente uma vez, cada visita com custo
     * constante c. Logo T(n) = n·c, ou seja, T(n) = O(n).
     *
     * <p>Essa troca (de um acesso direto O(1) por um caminho fixo, para uma
     * busca O(n)) é uma decisão clássica de engenharia de software: aceitar
     * um custo assintótico maior em troca de resiliência a mudanças
     * estruturais fora do controle da equipe — viável aqui porque o tamanho
     * da árvore JSON de um perfil do Instagram é pequeno o suficiente para
     * que o custo O(n) seja imperceptível na prática.
     */
    public static void algorithmTechniqueAppliedInProject() {
        // Método vazio de propósito — existe só como âncora de documentação
        // (javadoc acima) para o item (b) do trabalho. A técnica real está
        // implementada em ScraperService.findNodeWithField.
    }
}
