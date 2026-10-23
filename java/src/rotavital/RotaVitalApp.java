package rotavital;

import static rotavital.Componente.*;
import static rotavital.TipoSanguineo.*;

import java.util.ArrayList;
import java.util.List;

/**
 * ROTA VITAL - Unidade 2 - VERSÃO JAVA (POO).
 * Cenário sintético completo: estoque -> varredura de vencidas -> fila de
 * pedidos -> alocação (matriz + hash + FEFO) -> rota (Floyd + vizinho mais
 * próximo + cadeia fria) -> relatórios (ordenações + buscas).
 *
 * C++:  tudo dentro de int main() chamando funções soltas sobre structs.
 * Java: main() orquestra OBJETOS (Estoque, ServicoAlocacao, Grafo,
 *       Roteirizador, Ordenacao). Em Spring Boot, Estoque/ServicoAlocacao/
 *       Roteirizador virariam @Service e este fluxo seria chamado por um
 *       @RestController.
 *
 * Compilar:  javac -d out src/rotavital/*.java
 * Executar:  java -cp out rotavital.RotaVitalApp
 */
public class RotaVitalApp {

    public static void main(String[] args) {
        final int HOJE = 292;   // 19/10/2026
        System.out.printf("ROTA VITAL - Unidade 2 (Java) | hoje: %s | dados sinteticos\n\n", Datas.formatar(HOJE));

        int falhas = new TestesRotaVital().executar();

        // --- Carga do estoque (sintético) ---
        Estoque estoque = new Estoque();
        Bolsa[] carga = {
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
        for (Bolsa b : carga) estoque.receber(b);   // C++: for (i < sizeof(carga)/sizeof(carga[0]))
        System.out.print("=== 1. ESTOQUE INICIAL (hash por componente+tipo, listas FEFO) ===\n");
        estoque.imprimir();

        FilaPedidos fila = new FilaPedidos();
        fila.enqueue(new Pedido(1, 3, A_POS, CH, 3, 90));     // Gama
        fila.enqueue(new Pedido(2, 1, O_NEG, CH, 2, 60));     // Alfa
        fila.enqueue(new Pedido(3, 4, AB_POS, PFC, 1, 75));   // Delta
        fila.enqueue(new Pedido(4, 2, O_POS, CH, 3, 45));     // Beta
        fila.enqueue(new Pedido(5, 5, B_NEG, CH, 2, 90));     // Epsilon
        fila.enqueue(new Pedido(6, 1, O_POS, PLQ, 1, 60));    // Alfa

        System.out.print("\n=== 2. VARREDURA DIARIA DE VENCIDAS ===\n");
        int vencidas = estoque.descartarTodasVencidas(HOJE);
        System.out.printf("Descartadas: %d | em estoque: %d\n", vencidas, estoque.getTotal());

        System.out.print("\n=== 3. ALOCACAO (fila FIFO -> matriz ABO/Rh -> hash -> FEFO) ===\n");
        ServicoAlocacao servico = new ServicoAlocacao(estoque);
        List<Alocacao> alocacoes = new ArrayList<>();
        Pedido p;
        while ((p = fila.dequeue()) != null) {          // C++: while (dequeue(&fila, &p))
            System.out.printf("Pedido #%d | %s | %s %s x%d | janela %d min\n", p.getId(),
                    Grafo.NOME_LOCAL[p.getHospital()], p.getComponente(), p.getReceptor(),
                    p.getQuantidade(), p.getJanelaMin());
            Alocacao a = servico.alocar(p, HOJE);
            for (Bolsa b : a.getBolsas())
                System.out.printf("   -> bolsa %d (%s %s) validade %s%s\n", b.getId(), b.getComponente(),
                        b.getTipo(), Datas.formatar(b.getValidade()),
                        b.getTipo() == p.getReceptor() ? "" : "  [tipo compativel]");
            int obtidas = a.quantidade();
            if (obtidas == p.getQuantidade()) System.out.print("   status: ATENDIDO\n");
            else if (obtidas > 0)
                System.out.printf("   status: PARCIAL (%d de %d) - acionar outro hemocentro\n", obtidas, p.getQuantidade());
            else System.out.print("   status: NAO ATENDIDO\n");
            if (obtidas > 0) alocacoes.add(a);
        }

        System.out.print("\n=== 4. ROTA (Floyd-Warshall + vizinho mais proximo + cadeia fria) ===\n");
        Grafo grafo = new Grafo();
        Roteirizador.Parada[] paradas = new Roteirizador.Parada[alocacoes.size()];
        for (int i = 0; i < paradas.length; i++) {
            Pedido ped = alocacoes.get(i).getPedido();
            paradas[i] = new Roteirizador.Parada(ped.getHospital(), ped.getId(), ped.getJanelaMin());
        }
        int total = new Roteirizador(grafo).vizinhoMaisProximo(0, paradas);
        int anterior = 0, alertas = 0;
        for (int i = 0; i < paradas.length; i++) {
            Roteirizador.Parada pa = paradas[i];
            System.out.printf("%d. Pedido #%d em %s | chegada %3d min | janela %3d | %s | cadeia fria %s\n",
                    i + 1, pa.getPedidoId(), Grafo.NOME_LOCAL[pa.getLocal()], pa.getChegadaMin(),
                    pa.getJanelaMin(), pa.noPrazo() ? "NO PRAZO" : "ATRASO  ",
                    pa.cadeiaFriaOk() ? "OK" : "RISCO");
            System.out.printf("   trajeto: %s\n", grafo.caminho(anterior, pa.getLocal()));
            if (!pa.noPrazo() || !pa.cadeiaFriaOk()) alertas++;
            anterior = pa.getLocal();
        }
        System.out.printf("Retorno: %s\n", grafo.caminho(anterior, 0));
        System.out.printf("Tempo total do circuito: %d min | alertas: %d\n", total, alertas);
        if (alertas > 0)
            System.out.print("Obs.: vizinho mais proximo e heuristica gulosa; nao garante a janela de todos.\n");

        System.out.print("\n=== 5. RELATORIOS (ordenacao + buscas) ===\n");
        Bolsa[] restante = estoque.exportar();
        int n = restante.length;
        System.out.printf("Bolsas restantes: %d | descartadas por vencimento: %d\n", n, estoque.getDescartadas());
        String[] nomes = {"Bubble Sort", "Selection Sort", "Insertion Sort",
                          "Shell Sort", "Quick Sort", "Merge Sort"};
        Ordenacao ord = new Ordenacao();
        Bolsa[] copia = null;
        for (int alg = 0; alg < 6; alg++) {
            copia = restante.clone();
            ord.zerar();
            switch (alg) {
                case 0: ord.bubbleSort(copia); break;
                case 1: ord.selectionSort(copia); break;
                case 2: ord.insertionSort(copia); break;
                case 3: ord.shellSort(copia); break;
                case 4: ord.quickSort(copia); break;
                default: ord.mergeSort(copia); break;
            }
            System.out.printf("  %-15s comparacoes: %4d | ordenado: %s\n", nomes[alg], ord.getComparacoes(),
                    Ordenacao.estaOrdenado(copia) ? "sim" : "NAO");
        }
        int limite = HOJE + 7;
        int qIt = Busca.primeiroAposIterativa(copia, limite);
        int qRec = Busca.primeiroAposRecursiva(copia, 0, n, limite);
        System.out.printf("Vencem ate %s (busca binaria iterativa=%d, recursiva=%d):\n",
                Datas.formatar(limite), qIt, qRec);
        for (int i = 0; i < qIt; i++)
            System.out.printf("   bolsa %d (%s %s) validade %s\n", copia[i].getId(), copia[i].getComponente(),
                    copia[i].getTipo(), Datas.formatar(copia[i].getValidade()));

        int[] ids = {1008, 1002, 1005};
        System.out.print("Consulta por id (hash com sondagem linear x busca sequencial):\n");
        for (int id : ids) {
            int pos = estoque.buscarPorId(id);
            int seq = Busca.sequencialPorId(restante, id);
            if (pos != -1)
                System.out.printf("   %d: em estoque (slot %d do indice; posicao %d na busca sequencial)\n", id, pos, seq);
            else
                System.out.printf("   %d: fora do estoque (alocada ou descartada); sequencial=%d\n", id, seq);
        }

        System.exit(falhas == 0 ? 0 : 1);
    }
}
