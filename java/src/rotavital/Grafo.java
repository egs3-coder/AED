package rotavital;

/**
 * Grafo da rede (6 vértices, dados sintéticos) como MATRIZ DE ADJACÊNCIA.
 * ESTRADAS[u][v] = minutos da estrada direta; INF = não há estrada.
 *
 * C++:  const int ESTRADAS[V][V] = {...};  (a rota recebe "const int adj[V][V]")
 * Java: a matriz fica dentro do objeto. O construtor sem argumento usa a malha
 *       do cenário; o outro recebe uma matriz qualquer (usado nos testes, como
 *       a matriz "beco" do C++).
 */
public class Grafo {
    public static final int V = 6;
    public static final int INF = 1_000_000;
    public static final String[] NOME_LOCAL = {
        "Hemocentro Central", "Hospital Alfa", "Hospital Beta",
        "Hospital Gama", "Hospital Delta", "Hospital Epsilon"
    };

    private static final int[][] ESTRADAS = {
        //  0    1    2    3    4    5
        {   0,  15,  25,  40, INF, INF },
        {  15,   0,  12, INF,  30, INF },
        {  25,  12,   0,  18,  22, INF },
        {  40, INF,  18,   0, INF,  20 },
        { INF,  30,  22, INF,   0,  16 },
        { INF, INF, INF,  20,  16,   0 },
    };

    private final int[][] adj;

    public Grafo() { this(ESTRADAS); }

    public Grafo(int[][] adj) { this.adj = adj; }

    /** Minutos da estrada direta u-v, ou INF. O(1). */
    public int tempo(int u, int v) { return adj[u][v]; }

    public int vertices() { return adj.length; }
}
