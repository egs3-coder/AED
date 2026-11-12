package rotavital;

import static rotavital.Componente.*;
import static rotavital.TipoSanguineo.*;

/**
 * Testes automatizados (mesmos casos da versão C++).
 *
 * C++:  função verificar(bool, const char*) + contadores globais g_ok/g_falhas
 * Java: contadores como atributos. Sem dependência externa para rodar com
 *       "javac/java" puro; no projeto Spring Boot estes casos viram métodos
 *       @Test do JUnit 5 com assertTrue/assertEquals.
 */
public class TestesRotaVital {
    private int ok = 0, falhas = 0;

    private void verificar(boolean cond, String descricao) {
        if (cond) { ok++; System.out.printf("  [OK]     %s\n", descricao); }
        else { falhas++; System.out.printf("  [FALHOU] %s\n", descricao); }
    }

    private static boolean compat(Componente c, TipoSanguineo d, TipoSanguineo r) {
        return MatrizCompatibilidade.compativel(c, d, r);
    }

    public int executar() {
        System.out.print("=== TESTES AUTOMATIZADOS ===\n");

        System.out.print("-- Compatibilidade de hemacias (CH) --\n");
        verificar(compat(CH, O_NEG, AB_POS), "CH: O- doa para AB+ (doador universal)");
        verificar(compat(CH, O_NEG, O_NEG), "CH: O- doa para O-");
        verificar(compat(CH, A_NEG, A_POS), "CH: A- doa para A+");
        verificar(compat(CH, AB_POS, AB_POS), "CH: AB+ doa para AB+");
        verificar(!compat(CH, O_POS, O_NEG), "CH: O+ NAO doa para O- (Rh)");
        verificar(!compat(CH, A_POS, O_POS), "CH: A+ NAO doa para O+ (antigeno A)");
        verificar(!compat(CH, B_NEG, A_NEG), "CH: B- NAO doa para A- (antigeno B)");
        verificar(!compat(CH, AB_POS, A_POS), "CH: AB+ NAO doa para A+");
        int doadoresONeg = 0, doadoresABPos = 0;
        for (TipoSanguineo t : TipoSanguineo.values()) {
            if (compat(CH, t, O_NEG)) doadoresONeg++;
            if (compat(CH, t, AB_POS)) doadoresABPos++;
        }
        verificar(doadoresONeg == 1, "CH: receptor O- so recebe de O- (1 doador)");
        verificar(doadoresABPos == 8, "CH: receptor AB+ recebe de todos (8 doadores)");
        TipoSanguineo[] col = MatrizCompatibilidade.doadoresCompativeis(CH, B_POS);
        verificar(col.length == 4 && col[0] == O_NEG && col[1] == O_POS && col[2] == B_NEG && col[3] == B_POS,
                "CH: coluna B+ da matriz = O-, O+, B-, B+");
        // Gabarito independente: regra dos antigenos (A, B, Rh) nas 64 combinacoes
        boolean confere = true;
        int pares = 0;
        for (TipoSanguineo d : TipoSanguineo.values())
            for (TipoSanguineo r : TipoSanguineo.values()) {
                boolean dA = d == A_NEG || d == A_POS || d == AB_NEG || d == AB_POS;
                boolean dB = d == B_NEG || d == B_POS || d == AB_NEG || d == AB_POS;
                boolean rA = r == A_NEG || r == A_POS || r == AB_NEG || r == AB_POS;
                boolean rB = r == B_NEG || r == B_POS || r == AB_NEG || r == AB_POS;
                boolean esperado = (!dA || rA) && (!dB || rB) && (d.ordinal() % 2 == 0 || r.ordinal() % 2 == 1);
                if (esperado != compat(CH, d, r)) confere = false;
                if (esperado) pares++;
            }
        verificar(confere && pares == 27, "CH: matriz confere com a regra de antigenos (64 pares, 27 compativeis)");

        System.out.print("-- Compatibilidade de plasma (PFC) --\n");
        verificar(compat(PFC, AB_POS, O_NEG), "PFC: AB+ doa para O- (plasma universal)");
        verificar(compat(PFC, A_NEG, O_POS), "PFC: A- doa para O+");
        verificar(!compat(PFC, O_POS, A_POS), "PFC: O+ NAO doa para A+ (anti-A)");
        verificar(!compat(PFC, B_POS, AB_NEG), "PFC: B+ NAO doa para AB- (anti-A)");
        verificar(compat(CRIO, AB_NEG, O_POS) && !compat(CRIO, O_NEG, A_POS), "CRIO: segue a regra do plasma");

        System.out.print("-- Lista FEFO (insercao ordenada) --\n");
        ListaFefo lista = new ListaFefo();
        lista.inserir(new Bolsa(1, A_POS, CH, 300));
        lista.inserir(new Bolsa(2, A_POS, CH, 290));
        lista.inserir(new Bolsa(3, A_POS, CH, 310));
        lista.inserir(new Bolsa(4, A_POS, CH, 290));
        int[] ordem = new int[4];
        int[] k = {0};
        lista.paraCada(b -> ordem[k[0]++] = b.getId());
        verificar(k[0] == 4 && ordem[0] == 2 && ordem[1] == 4 && ordem[2] == 1 && ordem[3] == 3,
                "FEFO: ordem 2,4,1,3 (validade crescente, empate por id)");

        System.out.print("-- Hash do estoque e indice por id --\n");
        Estoque est = new Estoque();
        est.setVerboso(false);
        est.receber(new Bolsa(10, O_NEG, CH, 300));
        est.receber(new Bolsa(11, O_NEG, CH, 295));
        est.receber(new Bolsa(12, A_POS, CH, 299));
        est.receber(new Bolsa(13, B_NEG, CH, 280));   // vencida
        est.receber(new Bolsa(14, AB_POS, PFC, 500));
        HashEstoque.Entrada en = est.getPorTipo().buscar(HashEstoque.chave(CH, O_NEG));
        verificar(en != null && en.getFefo().tamanho() == 2 && en.getFefo().primeira().getId() == 11,
                "Hash: [CH O-] tem 2 bolsas e a cabeca e a 11 (vence antes)");
        verificar(est.getPorTipo().buscar(HashEstoque.chave(PLQ, O_POS)) == null,
                "Hash: chave inexistente retorna NULL");
        est.receber(new Bolsa(15, A_POS, PLQ, 400));   // chave 1*8+3 = 11
        HashEstoque.Entrada chONeg = est.getPorTipo().buscar(HashEstoque.chave(CH, O_NEG));
        HashEstoque.Entrada plqAPos = est.getPorTipo().buscar(HashEstoque.chave(PLQ, A_POS));
        verificar(HashEstoque.funcaoHash(HashEstoque.chave(CH, O_NEG)) == 0
                        && HashEstoque.funcaoHash(HashEstoque.chave(PLQ, A_POS)) == 0
                        && chONeg != null && plqAPos != null && plqAPos.getFefo().primeira().getId() == 15,
                "Hash: CH O- (chave 0) e PLQ A+ (chave 11) colidem no slot 0 e ambas sao achadas");
        verificar(est.buscarPorId(12) != -1, "Indice: bolsa 12 encontrada por id");
        verificar(est.buscarPorId(99) == -1, "Indice: bolsa 99 inexistente");

        System.out.print("-- Alocacao integrada (matriz + hash + FEFO) --\n");
        ServicoAlocacao servico = new ServicoAlocacao(est);
        Alocacao a = servico.alocar(new Pedido(1, 1, A_POS, CH, 1, 60), 292);
        verificar(a.quantidade() == 1 && a.getBolsas().get(0).getId() == 11,
                "Alocacao: A+ recebe a compativel que vence antes (O- 11, nao a A+ 12)");
        a = servico.alocar(new Pedido(2, 1, A_POS, CH, 1, 60), 292);
        verificar(a.quantidade() == 1 && a.getBolsas().get(0).getId() == 12,
                "Alocacao: depois, A+ 12 (vence antes de O- 10)");
        verificar(est.buscarPorId(11) == -1, "Alocacao: bolsa alocada sai do indice por id");
        a = servico.alocar(new Pedido(3, 1, B_NEG, CH, 3, 60), 292);
        verificar(a.quantidade() == 0 && a.getDisponiveis() == 1 && est.getDescartadas() == 1
                        && est.buscarPorId(10) != -1,
                "Alocacao: B- descarta a vencida (13); 1 compativel para 3 pedidas -> nada sai (tudo ou nada)");
        Pedido p3b = new Pedido(7, 1, B_NEG, CH, 1, 60);
        a = servico.alocar(p3b, 292);
        verificar(a.quantidade() == 1 && a.getBolsas().get(0).getId() == 10, "Alocacao: B- x1 recebe O- (10)");
        boolean todasCompat = true;
        for (Bolsa b : a.getBolsas())
            if (!compat(b.getComponente(), b.getTipo(), p3b.getReceptor())) todasCompat = false;
        verificar(todasCompat, "Alocacao: nenhuma bolsa incompativel foi entregue");
        a = servico.alocar(new Pedido(4, 1, O_NEG, PFC, 1, 60), 292);
        verificar(a.quantidade() == 1 && a.getBolsas().get(0).getTipo() == AB_POS,
                "Alocacao: plasma O- aceita AB+ (regra invertida)");
        a = servico.alocar(new Pedido(5, 1, O_NEG, PLQ, 1, 60), 292);
        verificar(a.quantidade() == 0, "Alocacao: sem estoque compativel -> 0 bolsas (PLQ A+ nao serve para O-)");

        System.out.print("-- Ordenacoes e buscas --\n");
        Bolsa[] base = {
            new Bolsa(1, O_POS, CH, 310), new Bolsa(2, O_POS, CH, 295), new Bolsa(3, O_POS, CH, 300),
            new Bolsa(4, O_POS, CH, 295), new Bolsa(5, O_POS, CH, 320), new Bolsa(6, O_POS, CH, 290),
            new Bolsa(7, O_POS, CH, 305), new Bolsa(8, O_POS, CH, 299)
        };
        Bolsa[][] v = new Bolsa[6][];
        for (int alg = 0; alg < 6; alg++) v[alg] = base.clone();   // C++: laço copiando struct a struct
        Ordenacao ord = new Ordenacao();
        ord.bubbleSort(v[0]); ord.selectionSort(v[1]); ord.insertionSort(v[2]);
        ord.shellSort(v[3]); ord.quickSort(v[4]); ord.mergeSort(v[5]);
        boolean iguais = true;
        for (int alg = 0; alg < 6; alg++) {
            if (!Ordenacao.estaOrdenado(v[alg])) iguais = false;
            for (int i = 0; i < 8; i++) if (v[alg][i].getId() != v[0][i].getId()) iguais = false;
        }
        verificar(iguais, "Ordenacao: os 6 algoritmos produzem a mesma ordem FEFO");
        int it = Busca.primeiroAposIterativa(v[0], 300);
        int rc = Busca.primeiroAposRecursiva(v[0], 0, 8, 300);
        int seq = 0;
        for (Bolsa b : base) if (b.getValidade() <= 300) seq++;
        verificar(it == 5 && rc == 5 && seq == 5,
                "Busca binaria iterativa = recursiva = contagem sequencial (5)");
        verificar(Busca.sequencialPorId(base, 7) == 6, "Busca sequencial: id 7 na posicao 6");

        System.out.print("-- Grafo e rota (vizinho mais proximo) --\n");
        Roteirizador rot = new Roteirizador(new Grafo());
        Roteirizador.Rota r3 = rot.vizinhoMaisProximo(0, 3);
        int[] c3 = r3.getCaminho();
        verificar(r3.getTempoMin() == 45 && c3.length == 4 && c3[1] == 1 && c3[2] == 2 && c3[3] == 3,
                "Vizinho: Hemocentro -> Alfa -> Beta -> Gama = 45 min");
        Roteirizador.Rota r4 = rot.vizinhoMaisProximo(0, 4);
        verificar(r4.getTempoMin() == 81 && r4.getCaminho().length == 6,
                "Vizinho e guloso: ate Delta da 81 min (via Alfa direto seriam 45)");
        int[][] beco = new int[Grafo.V][Grafo.V];
        for (int i = 0; i < Grafo.V; i++)
            for (int j = 0; j < Grafo.V; j++) beco[i][j] = (i == j) ? 0 : Grafo.INF;
        beco[0][1] = beco[1][0] = 2;     // 1 e beco sem saida
        beco[0][2] = beco[2][0] = 6;
        beco[2][3] = beco[3][2] = 4;
        Roteirizador rotBeco = new Roteirizador(new Grafo(beco));
        Roteirizador.Rota rb = rotBeco.vizinhoMaisProximo(0, 3);
        verificar(rb.getTempoMin() == 10 && rb.getCaminho().length == 3 && rb.getCaminho()[1] == 2,
                "Vizinho: beco sem saida em 1 -> volta e vai por 2 (10 min)");
        verificar(rotBeco.vizinhoMaisProximo(0, 4) == null, "Vizinho: destino sem ligacao -> -1");

        System.out.printf("Resultado: %d OK, %d falha(s)\n\n", ok, falhas);
        return falhas;
    }

    public static void main(String[] args) {
        System.exit(new TestesRotaVital().executar() == 0 ? 0 : 1);
    }
}
