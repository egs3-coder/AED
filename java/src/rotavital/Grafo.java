package rotavital;

/**
 * Grafo da rede (6 vértices, dados sintéticos) + Floyd-Warshall.
 *
 * C++:  struct Grafo { int dist[V][V]; int prox[V][V]; };  void floydWarshall(Grafo* g)
 *       void imprimirCaminho(const Grafo* g, int u, int v)  -> printf direto
 * Java: Floyd-Warshall roda no construtor; caminho() devolve uma String
 *       (StringBuilder) em vez de imprimir, para poder ser reaproveitada
 *       por uma API REST no Spring Boot.
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

    private final int[][] dist = new int[V][V];
    private final int[][] prox = new int[V][V];

    /** Floyd-Warshall: O(V^3) tempo, O(V^2) memória. */
    public Grafo() {
        for (int i = 0; i < V; i++)
            for (int j = 0; j < V; j++) {
                dist[i][j] = ESTRADAS[i][j];
                prox[i][j] = (ESTRADAS[i][j] < INF) ? j : -1;
            }
        for (int k = 0; k < V; k++)
            for (int i = 0; i < V; i++)
                for (int j = 0; j < V; j++)
                    if (dist[i][k] + dist[k][j] < dist[i][j]) {
                        dist[i][j] = dist[i][k] + dist[k][j];
                        prox[i][j] = prox[i][k];
                    }
    }

    public int dist(int u, int v) { return dist[u][v]; }

    public String caminho(int u, int v) {
        if (prox[u][v] == -1) return "(sem caminho)";
        StringBuilder sb = new StringBuilder(NOME_LOCAL[u]);
        while (u != v) {
            u = prox[u][v];
            sb.append(" -> ").append(NOME_LOCAL[u]);
        }
        return sb.toString();
    }
}
