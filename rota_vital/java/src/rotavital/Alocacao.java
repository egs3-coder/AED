package rotavital;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultado da alocação de um pedido.
 *
 * C++:  struct Alocacao { Pedido pedido; Bolsa bolsas[MAX_POR_PEDIDO]; int qtd; };
 * Java: List<Bolsa> substitui o vetor fixo + contador "qtd" (size()).
 *       A lista é só o "recipiente" do resultado; os algoritmos estudados
 *       (matriz, hash, FEFO) continuam implementados à mão.
 */
public class Alocacao {
    private final Pedido pedido;
    private final List<Bolsa> bolsas = new ArrayList<>();

    public Alocacao(Pedido pedido) { this.pedido = pedido; }

    void adicionar(Bolsa b) { bolsas.add(b); }

    public Pedido getPedido() { return pedido; }
    public List<Bolsa> getBolsas() { return bolsas; }
    public int quantidade() { return bolsas.size(); }
    public boolean atendidoTotalmente() { return bolsas.size() == pedido.getQuantidade(); }
}
