package rotavital;

/**
 * Alocação: matriz ABO/Rh + hash + FEFO trabalhando juntos.
 * Regra: 1º tipo idêntico (FEFO); se faltar, entre os compatíveis pega a
 * bolsa que vence primeiro comparando só as CABEÇAS das listas.
 *
 * C++:  int alocar(Estoque* e, Pedido p, int hoje, Alocacao* out)
 * Java: classe de serviço que recebe o Estoque no construtor (injeção de
 *       dependência, o mesmo padrão de um @Service no Spring Boot) e devolve
 *       um objeto Alocacao em vez de preencher um ponteiro de saída.
 */
public class ServicoAlocacao {
    public static final int MAX_POR_PEDIDO = 10;

    private final Estoque estoque;

    public ServicoAlocacao(Estoque estoque) { this.estoque = estoque; }

    public Alocacao alocar(Pedido p, int hoje) {
        Alocacao out = new Alocacao(p);
        HashEstoque.Entrada[] candidatas = new HashEstoque.Entrada[8];   // C++: EntradaEstoque* candidatas[8]
        int nc = 0;
        HashEstoque.Entrada identica = null;

        // (a) 8 consultas O(1) na matriz + 8 consultas O(1) no hash
        for (TipoSanguineo t : TipoSanguineo.values()) {          // C++: for (int t = 0; t < N_TIPOS; t++)
            if (!MatrizCompatibilidade.compativel(p.getComponente(), t, p.getReceptor())) continue;
            HashEstoque.Entrada en = estoque.getPorTipo().buscar(HashEstoque.chave(p.getComponente(), t));
            if (en == null) continue;
            estoque.descartarVencidas(en, hoje);
            candidatas[nc++] = en;
            if (t == p.getReceptor()) identica = en;
        }

        // (b) cada bolsa: compara no máximo 8 cabeças de lista -> O(1) por bolsa
        while (out.quantidade() < p.getQuantidade() && out.quantidade() < MAX_POR_PEDIDO) {
            HashEstoque.Entrada escolhida = null;
            if (identica != null && !identica.getFefo().estaVazia()) {
                escolhida = identica;
            } else {
                for (int i = 0; i < nc; i++) {
                    if (candidatas[i].getFefo().estaVazia()) continue;
                    if (escolhida == null ||
                        candidatas[i].getFefo().primeira().venceAntesDe(escolhida.getFefo().primeira()))
                        escolhida = candidatas[i];
                }
            }
            if (escolhida == null) break;
            out.adicionar(estoque.retirar(escolhida));
        }
        return out;
    }
}
