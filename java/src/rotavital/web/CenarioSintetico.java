package rotavital.web;

import static rotavital.Componente.*;
import static rotavital.TipoSanguineo.*;

import rotavital.Bolsa;
import rotavital.Pedido;

/**
 * Dados sintéticos do cenário (os MESMOS do main em C++ e de RotaVitalApp).
 *
 * Camada web: NÃO tem equivalente em C++. Só reúne os dados de exemplo
 * para o servidor poder reiniciar o estoque e carregar os pedidos de teste.
 */
public final class CenarioSintetico {
    public static final int HOJE = 292;   // 19/10/2026, igual ao C++

    private CenarioSintetico() { }

    public static Bolsa[] carga() {
        return new Bolsa[] {
            new Bolsa(1001, O_NEG, CH, 300),  new Bolsa(1002, O_NEG, CH, 295),
            new Bolsa(1003, O_POS, CH, 310),  new Bolsa(1004, O_POS, CH, 293),
            new Bolsa(1005, O_POS, CH, 288),  new Bolsa(1006, A_POS, CH, 305),
            new Bolsa(1007, A_POS, CH, 297),  new Bolsa(1008, A_NEG, CH, 320),
            new Bolsa(1009, B_POS, CH, 299),  new Bolsa(1010, B_NEG, CH, 315),
            new Bolsa(1011, AB_POS, CH, 301), new Bolsa(1012, AB_NEG, CH, 290),
            new Bolsa(1013, A_POS, PFC, 600), new Bolsa(1014, AB_POS, PFC, 550),
            new Bolsa(1015, O_POS, PFC, 580), new Bolsa(1016, B_POS, PFC, 620),
            new Bolsa(1017, A_POS, PLQ, 294), new Bolsa(1018, O_POS, PLQ, 293),
            new Bolsa(1019, O_POS, PLQ, 291), new Bolsa(1020, AB_POS, PLQ, 296),
            new Bolsa(1021, A_POS, CH, 294),  new Bolsa(1022, O_NEG, CH, 302),
        };
    }

    /** Os 6 pedidos do cenário do console (ids definidos pelo servidor). */
    public static Pedido[] pedidosExemplo(int primeiroId) {
        int id = primeiroId;
        return new Pedido[] {
            new Pedido(id++, 3, A_POS, CH, 3, 90),     // Gama
            new Pedido(id++, 1, O_NEG, CH, 2, 60),     // Alfa
            new Pedido(id++, 4, AB_POS, PFC, 1, 75),   // Delta
            new Pedido(id++, 2, O_POS, CH, 3, 45),     // Beta
            new Pedido(id++, 5, B_NEG, CH, 2, 90),     // Epsilon
            new Pedido(id, 1, O_POS, PLQ, 1, 60),      // Alfa
        };
    }
}
