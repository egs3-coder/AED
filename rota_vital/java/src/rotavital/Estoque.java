package rotavital;

/**
 * Estoque = hash por (componente,tipo) com listas FEFO + índice por id.
 *
 * C++:  struct Estoque { HashEstoque porTipo; IndiceId porId; int total; int descartadas; };
 *       + funções estoqueReceber(Estoque* e, ...), estoqueDescartarVencidas(...), ...
 *       + global bool g_verboso
 * Java: classe com os mesmos campos (encapsulados) e métodos de instância.
 *       O global g_verboso virou atributo do objeto (evita estado global).
 *       estoqueLiberar() não existe: o garbage collector libera os nós.
 */
public class Estoque {
    private final HashEstoque porTipo = new HashEstoque();
    private final IndicePorId porId = new IndicePorId();
    private int total;
    private int descartadas;
    private boolean verboso = true;

    public void setVerboso(boolean verboso) { this.verboso = verboso; }
    public int getTotal() { return total; }
    public int getDescartadas() { return descartadas; }
    public HashEstoque getPorTipo() { return porTipo; }

    /** O(1) para achar a lista + O(m) da inserção ordenada. */
    public void receber(Bolsa b) {
        HashEstoque.Entrada en = porTipo.obterOuCriar(HashEstoque.chave(b.getComponente(), b.getTipo()));
        en.getFefo().inserir(b);
        porId.inserir(b);
        total++;
    }

    /** Vencidas formam um PREFIXO da lista FEFO: O(v), v = vencidas. */
    public int descartarVencidas(HashEstoque.Entrada en, int hoje) {
        int n = 0;
        ListaFefo fefo = en.getFefo();
        while (!fefo.estaVazia() && fefo.primeira().getValidade() < hoje) {
            Bolsa b = fefo.removerPrimeiro();
            total--;
            descartadas++;
            porId.remover(b.getId());
            if (verboso)
                System.out.printf("   [descarte] bolsa %d (%s %s) vencida em %s\n",
                        b.getId(), b.getComponente(), b.getTipo(), Datas.formatar(b.getValidade()));
            n++;
        }
        return n;
    }

    /** Varredura diária: O(TAM_HASH + chaves + v). */
    public int descartarTodasVencidas(int hoje) {
        int n = 0;
        for (int i = 0; i < HashEstoque.TAM_HASH; i++)
            for (HashEstoque.Entrada en = porTipo.primeiraDoSlot(i); en != null; en = HashEstoque.proxima(en))
                n += descartarVencidas(en, hoje);
        return n;
    }

    public Bolsa retirar(HashEstoque.Entrada en) {
        Bolsa b = en.getFefo().removerPrimeiro();
        total--;
        porId.remover(b.getId());
        return b;
    }

    /** Posição no índice por id ou -1. */
    public int buscarPorId(int id) { return porId.buscar(id); }

    public void imprimir() {
        System.out.printf("Estoque: %d bolsas | hash %d slots | colisoes no hash: %d\n",
                total, HashEstoque.TAM_HASH, porTipo.getColisoes());
        for (int i = 0; i < HashEstoque.TAM_HASH; i++) {
            System.out.printf("  slot %d:", i);
            HashEstoque.Entrada en = porTipo.primeiraDoSlot(i);
            if (en == null) System.out.print(" (vazio)");
            System.out.print("\n");
            while (en != null) {
                System.out.printf("    [%s %s] qtd=%d:", en.getComponente(), en.getTipo(), en.getFefo().tamanho());
                en.getFefo().paraCada(b ->
                        System.out.printf(" %d(%s)", b.getId(), Datas.formatar(b.getValidade())));
                System.out.print("\n");
                en = HashEstoque.proxima(en);
            }
        }
    }

    /** Copia para vetor (relatórios). C++ recebia Bolsa saida[] + max; Java devolve o array. */
    public Bolsa[] exportar() {
        Bolsa[] saida = new Bolsa[total];
        int[] n = {0};   // contador mutável usado dentro da lambda
        for (int i = 0; i < HashEstoque.TAM_HASH; i++)
            for (HashEstoque.Entrada en = porTipo.primeiraDoSlot(i); en != null; en = HashEstoque.proxima(en))
                en.getFefo().paraCada(b -> saida[n[0]++] = b);
        return saida;
    }
}
