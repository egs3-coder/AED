package rotavital;

/**
 * Heurística do vizinho mais próximo + verificação de janela e cadeia fria.
 *
 * C++:  struct Parada {...};  int rotaVizinhoMaisProximo(const Grafo* g, int origem,
 *                                                      Parada paradas[], int k)
 *       std::swap(paradas[pos], paradas[melhor])
 * Java: Parada vira classe interna; o swap é feito à mão com variável
 *       temporária (troca de REFERÊNCIAS no array, não cópia de struct).
 */
public class Roteirizador {
    public static final int LIMITE_CADEIA_FRIA_MIN = 120;   // caixa térmica (sintético)

    public static class Parada {
        final int local;
        final int pedidoId;
        final int janelaMin;
        int chegadaMin;

        public Parada(int local, int pedidoId, int janelaMin) {
            this.local = local;
            this.pedidoId = pedidoId;
            this.janelaMin = janelaMin;
        }

        public int getLocal() { return local; }
        public int getPedidoId() { return pedidoId; }
        public int getJanelaMin() { return janelaMin; }
        public int getChegadaMin() { return chegadaMin; }
        public boolean noPrazo() { return chegadaMin <= janelaMin; }
        public boolean cadeiaFriaOk() { return chegadaMin <= LIMITE_CADEIA_FRIA_MIN; }
    }

    private final Grafo grafo;

    public Roteirizador(Grafo grafo) { this.grafo = grafo; }

    /**
     * Reordena 'paradas' na ordem de visita. O(k^2): para cada posição, varre
     * as restantes (mesma estrutura do Selection Sort). Retorna o tempo total
     * com a volta à origem.
     */
    public int vizinhoMaisProximo(int origem, Parada[] paradas) {
        int atual = origem;
        int tempo = 0;
        for (int pos = 0; pos < paradas.length; pos++) {
            int melhor = pos;
            for (int i = pos + 1; i < paradas.length; i++)
                if (grafo.dist(atual, paradas[i].local) < grafo.dist(atual, paradas[melhor].local))
                    melhor = i;
            Parada tmp = paradas[pos];               // C++: std::swap(paradas[pos], paradas[melhor]);
            paradas[pos] = paradas[melhor];
            paradas[melhor] = tmp;
            tempo += grafo.dist(atual, paradas[pos].local);
            paradas[pos].chegadaMin = tempo;
            atual = paradas[pos].local;
        }
        return tempo + grafo.dist(atual, origem);
    }
}
