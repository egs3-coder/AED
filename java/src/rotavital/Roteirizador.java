package rotavital;

/**
 * Rota por VIZINHO MAIS PRÓXIMO andando pela malha (a mesma rota da US05 do HemoTrack).
 * Partindo da origem, vai sempre para o vizinho AINDA NÃO VISITADO com a estrada
 * mais curta. Se cair num beco sem saída, volta um ponto e tenta o próximo vizinho.
 * É guloso: olha só o próximo passo, então pode não achar o menor tempo.
 *
 * C++:  int rotaVizinhoMaisProximo(const int adj[V][V], int origem, int destino,
 *                                  int caminho[], int* tamanho)
 *       devolve o tempo (ou -1) e preenche caminho[] + *tamanho (parâmetros de saída)
 * Java: devolve um objeto Rota (caminho + tempo) ou null se não há ligação.
 *       Java não tem parâmetro de saída, por isso o resultado vira objeto.
 */
public class Roteirizador {
    public static final int LIMITE_CADEIA_FRIA_MIN = 120;   // caixa térmica (sintético)

    /** Resultado: caminho (índices dos vértices) e tempo total. */
    public static class Rota {
        private final int[] caminho;
        private final int tempoMin;

        Rota(int[] caminho, int tempoMin) {
            this.caminho = caminho;
            this.tempoMin = tempoMin;
        }

        public int[] getCaminho() { return caminho; }
        public int getTempoMin() { return tempoMin; }
        public boolean noPrazo(int janelaMin) { return tempoMin <= janelaMin; }
        public boolean cadeiaFriaOk() { return tempoMin <= LIMITE_CADEIA_FRIA_MIN; }

        /** C++: imprimirCaminho(caminho, tamanho) imprimia direto; aqui vira String. */
        public String descricao() {
            StringBuilder sb = new StringBuilder(Grafo.NOME_LOCAL[caminho[0]]);
            for (int i = 1; i < caminho.length; i++) sb.append(" -> ").append(Grafo.NOME_LOCAL[caminho[i]]);
            return sb.toString();
        }
    }

    private final Grafo grafo;

    public Roteirizador(Grafo grafo) { this.grafo = grafo; }

    /**
     * O(V^2): cada vértice entra na pilha no máximo 1 vez (visitado[]) e cada
     * volta do while varre uma linha da matriz (V colunas).
     */
    public Rota vizinhoMaisProximo(int origem, int destino) {
        int n = grafo.vertices();
        boolean[] visitado = new boolean[n];      // Java já inicia com false (C++: laço zerando)
        int[] pilha = new int[n];                 // C++: int caminho[] (recebido como parâmetro)
        int topo = 0;
        pilha[topo++] = origem;
        visitado[origem] = true;
        while (topo > 0) {
            int atual = pilha[topo - 1];
            if (atual == destino) break;
            int vizinho = -1;
            for (int v = 0; v < n; v++)
                if (!visitado[v] && grafo.tempo(atual, v) < Grafo.INF &&
                    (vizinho == -1 || grafo.tempo(atual, v) < grafo.tempo(atual, vizinho)))
                    vizinho = v;
            if (vizinho == -1) topo--;            // beco sem saída: volta
            else { visitado[vizinho] = true; pilha[topo++] = vizinho; }
        }
        if (topo == 0) return null;               // C++: return -1;
        int[] caminho = new int[topo];            // C++: *tamanho = topo;
        int tempo = 0;
        caminho[0] = pilha[0];
        for (int i = 1; i < topo; i++) {
            caminho[i] = pilha[i];
            tempo += grafo.tempo(pilha[i - 1], pilha[i]);
        }
        return new Rota(caminho, tempo);
    }
}
