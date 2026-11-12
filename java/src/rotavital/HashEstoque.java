package rotavital;

/**
 * Tabela hash com ENCADEAMENTO EXTERNO (aula 06).
 * chave = componente * 8 + tipo (32 chaves: 0..31)  ->  entrada com a lista FEFO daquela combinação.
 * h(chave) = chave % 11 (o mesmo do HemoTrack). Ex.: CH O- (0) e PLQ A+ (11) colidem no slot 0.
 *
 * C++:  struct EntradaEstoque {...}; struct HashEstoque { EntradaEstoque* slots[7]; };
 *       funções soltas hashInicializar/hashBuscar/hashObterOuCriar(HashEstoque* h, ...)
 * Java: tudo vira uma classe. O ponteiro "h" do C++ passa a ser o "this"
 *       implícito; hashInicializar() vira o construtor.
 *       Não usamos java.util.HashMap de propósito: a função hash e o
 *       tratamento de colisão são o objeto de estudo.
 */
public class HashEstoque {

    public static final int TAM_HASH = 11;  // primo (o mesmo do HemoTrack)

    public static class Entrada {
        final int chave;
        final ListaFefo fefo = new ListaFefo();   // C++: NoBolsa* fefo; int quantidade;
        Entrada prox;                              // C++: EntradaEstoque* prox;
        Entrada(int chave) { this.chave = chave; }

        public int getChave() { return chave; }
        public ListaFefo getFefo() { return fefo; }
        public Componente getComponente() { return Componente.values()[chave / 8]; }
        public TipoSanguineo getTipo() { return TipoSanguineo.values()[chave % 8]; }
    }

    private final Entrada[] slots = new Entrada[TAM_HASH];   // Java já inicia com null
    private int colisoes;

    public static int chave(Componente c, TipoSanguineo t) { return c.ordinal() * 8 + t.ordinal(); }
    public static int funcaoHash(int chave) { return chave % TAM_HASH; }

    /** O(1 + alfa), alfa <= 32/11 ~ 2,9: percorre só a cadeia do slot. */
    public Entrada buscar(int chave) {
        Entrada atual = slots[funcaoHash(chave)];
        while (atual != null) {
            if (atual.chave == chave) return atual;
            atual = atual.prox;
        }
        return null;
    }

    public Entrada obterOuCriar(int chave) {
        Entrada e = buscar(chave);
        if (e != null) return e;
        int indice = funcaoHash(chave);
        if (slots[indice] != null) colisoes++;
        e = new Entrada(chave);
        e.prox = slots[indice];          // insere no início da cadeia (igual ao C++)
        slots[indice] = e;
        return e;
    }

    public Entrada primeiraDoSlot(int i) { return slots[i]; }
    public static Entrada proxima(Entrada e) { return e.prox; }
    public int getColisoes() { return colisoes; }
}
