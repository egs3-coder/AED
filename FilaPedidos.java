package rotavital;

/**
 * Fila FIFO encadeada (aula 03).
 *
 * C++:  struct FilaPedidos { NoPedido* inicio; NoPedido* fim; int tamanho; };
 *       bool dequeue(FilaPedidos* f, Pedido* saida)  -> resultado via ponteiro
 * Java: dequeue() devolve o Pedido ou null quando vazia (não há ponteiro de saída).
 */
public class FilaPedidos {

    private static class No {
        final Pedido pedido;
        No prox;
        No(Pedido pedido) { this.pedido = pedido; }
    }

    private No inicio, fim;
    private int tamanho;

    /** O(1): insere no fim. */
    public void enqueue(Pedido p) {
        No novo = new No(p);
        if (fim != null) fim.prox = novo;
        else inicio = novo;
        fim = novo;
        tamanho++;
    }

    /** O(1): remove do início. */
    public Pedido dequeue() {
        if (inicio == null) return null;
        Pedido p = inicio.pedido;
        inicio = inicio.prox;
        if (inicio == null) fim = null;
        tamanho--;
        return p;
    }

    public boolean estaVazia() { return inicio == null; }
    public int tamanho() { return tamanho; }
}
