package rotavital;

import java.util.Arrays;

/**
 * Índice id -> bolsa com SONDAGEM LINEAR (endereçamento aberto, aula 06).
 *
 * C++:  struct IndiceId { int chave[101]; Bolsa bolsa[101]; };
 *       indiceInicializar -> laço preenchendo VAZIO
 * Java: arrays como campos; Arrays.fill() substitui o laço de inicialização.
 *       A lógica de sondagem (base + i) % TAM e a marcação REMOVIDO é idêntica.
 */
public class IndicePorId {
    public static final int TAM_INDICE = 101;
    private static final int VAZIO = -1;
    private static final int REMOVIDO = -2;

    private final int[] chaves = new int[TAM_INDICE];
    private final Bolsa[] bolsas = new Bolsa[TAM_INDICE];

    public IndicePorId() { Arrays.fill(chaves, VAZIO); }

    private static int hash(int id) { return id % TAM_INDICE; }

    public int inserir(Bolsa b) {
        int base = hash(b.getId());
        for (int i = 0; i < TAM_INDICE; i++) {
            int t = (base + i) % TAM_INDICE;
            if (chaves[t] == VAZIO || chaves[t] == REMOVIDO) {
                chaves[t] = b.getId();
                bolsas[t] = b;
                return t;
            }
        }
        return -1;
    }

    /** O(1) médio; para no primeiro VAZIO e "pula" os REMOVIDO. */
    public int buscar(int id) {
        int base = hash(id);
        for (int i = 0; i < TAM_INDICE; i++) {
            int t = (base + i) % TAM_INDICE;
            if (chaves[t] == VAZIO) return -1;
            if (chaves[t] == id) return t;
        }
        return -1;
    }

    public boolean remover(int id) {
        int pos = buscar(id);
        if (pos == -1) return false;
        chaves[pos] = REMOVIDO;
        bolsas[pos] = null;            // em Java, solta a referência para o GC
        return true;
    }

    public Bolsa bolsaNoSlot(int pos) { return bolsas[pos]; }
}
