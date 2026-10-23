package rotavital;

/**
 * Requisição hospitalar.
 *
 * C++:  struct Pedido { int id; int hospital; Tipo receptor; Componente comp;
 *                       int quantidade; int janelaMin; };
 *       inicializado com chaves: Pedido p = {1, 3, A_POS, CH, 3, 90};
 * Java: classe imutável com construtor na mesma ordem dos campos.
 */
public final class Pedido {
    private final int id;
    private final int hospital;        // vértice do grafo
    private final TipoSanguineo receptor;
    private final Componente componente;
    private final int quantidade;
    private final int janelaMin;       // prazo máximo de entrega (min após a saída)

    public Pedido(int id, int hospital, TipoSanguineo receptor, Componente componente,
                  int quantidade, int janelaMin) {
        this.id = id;
        this.hospital = hospital;
        this.receptor = receptor;
        this.componente = componente;
        this.quantidade = quantidade;
        this.janelaMin = janelaMin;
    }

    public int getId() { return id; }
    public int getHospital() { return hospital; }
    public TipoSanguineo getReceptor() { return receptor; }
    public Componente getComponente() { return componente; }
    public int getQuantidade() { return quantidade; }
    public int getJanelaMin() { return janelaMin; }
}
