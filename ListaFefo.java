package rotavital;

import java.util.function.Consumer;

/**
 * Lista simplesmente encadeada mantida em ordem FEFO (aula 03).
 *
 * C++:  struct NoBolsa { Bolsa bolsa; NoBolsa* prox; };
 *       NoBolsa* fefoInserir(NoBolsa* head, Bolsa b)   -> devolve a nova cabeça
 * Java: a cabeça fica DENTRO do objeto (campo 'cabeca'), então inserir() é
 *       void. "NoBolsa*" vira uma referência a No; "NULL" vira "null";
 *       "new NoBolsa" continua "new No"; "delete" some (garbage collector).
 */
public class ListaFefo {

    private static class No {
        Bolsa bolsa;
        No prox;                         // C++: NoBolsa* prox;
        No(Bolsa bolsa) { this.bolsa = bolsa; }
    }

    private No cabeca;                   // C++: NoBolsa* head
    private int tamanho;

    /** Inserção ordenada: pior caso O(m), m = tamanho da lista. */
    public void inserir(Bolsa b) {
        No novo = new No(b);             // C++: NoBolsa* novo = new NoBolsa;
        if (cabeca == null || b.venceAntesDe(cabeca.bolsa)) {
            novo.prox = cabeca;          // C++: novo->prox = head; return novo;
            cabeca = novo;
        } else {
            No atual = cabeca;
            while (atual.prox != null && !b.venceAntesDe(atual.prox.bolsa))
                atual = atual.prox;      // C++: atual = atual->prox;
            novo.prox = atual.prox;
            atual.prox = novo;
        }
        tamanho++;
    }

    /** Remove e devolve a bolsa que vence primeiro. O(1). */
    public Bolsa removerPrimeiro() {
        // C++: *removida = head->bolsa; temp = head; head = head->prox; delete temp;
        Bolsa b = cabeca.bolsa;
        cabeca = cabeca.prox;
        tamanho--;
        return b;
    }

    public Bolsa primeira() { return cabeca == null ? null : cabeca.bolsa; }
    public boolean estaVazia() { return cabeca == null; }
    public int tamanho() { return tamanho; }

    /** Percorre na ordem FEFO. C++: for (NoBolsa* no = head; no != NULL; no = no->prox) */
    public void paraCada(Consumer<Bolsa> acao) {
        for (No no = cabeca; no != null; no = no.prox) acao.accept(no.bolsa);
    }
}
