package rotavital;

/**
 * Alocação: matriz ABO/Rh + hash + FEFO trabalhando juntos.
 * Regra (a mesma da US04 do HemoTrack): entre TODOS os tipos compatíveis sai
 * primeiro a bolsa que vence primeiro, comparando só as CABEÇAS das listas.
 * TUDO OU NADA: se não há bolsas compatíveis suficientes, nenhuma sai do estoque.
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
        if (p.getQuantidade() < 1 || p.getQuantidade() > MAX_POR_PEDIDO) return out;

        // (a) coluna da matriz: doadores aceitos (8 consultas O(1))
        TipoSanguineo[] doadores = MatrizCompatibilidade.doadoresCompativeis(p.getComponente(), p.getReceptor());

        // (b) uma busca O(1) no hash por doador; descarta as vencidas do início
        HashEstoque.Entrada[] listas = new HashEstoque.Entrada[doadores.length];   // C++: EntradaEstoque* listas[N_TIPOS]
        int nl = 0;
        int disponiveis = 0;
        for (TipoSanguineo t : doadores) {                  // C++: for (int i = 0; i < nd; i++)
            HashEstoque.Entrada en = estoque.getPorTipo().buscar(HashEstoque.chave(p.getComponente(), t));
            if (en == null) continue;
            estoque.descartarVencidas(en, hoje);
            if (!en.getFefo().estaVazia()) {
                listas[nl++] = en;
                disponiveis += en.getFefo().tamanho();
            }
        }
        out.setDisponiveis(disponiveis);
        if (disponiveis < p.getQuantidade()) return out;    // tudo ou nada

        // (c) q vezes: compara as cabeças (no máx. 8) e retira a que vence antes
        while (out.quantidade() < p.getQuantidade()) {
            HashEstoque.Entrada melhor = null;
            for (int i = 0; i < nl; i++) {
                if (listas[i].getFefo().estaVazia()) continue;
                // C++: vemAntes(listas[i]->fefo->bolsa, melhor->fefo->bolsa)
                if (melhor == null || listas[i].getFefo().primeira().venceAntesDe(melhor.getFefo().primeira()))
                    melhor = listas[i];
            }
            out.adicionar(estoque.retirar(melhor));
        }
        return out;
    }
}
