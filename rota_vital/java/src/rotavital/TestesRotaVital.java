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

        System.out.print("-- Compatibilidade de plasma (PFC) --\n");
        verificar(compat(PFC, AB_POS, O_NEG), "PFC: AB+ doa para O- (plasma universal)");
        verificar(compat(PFC, A_NEG, O_POS), "PFC: A- doa para O+");
        verificar(!compat(PFC, O_POS, A_POS), "PFC: O+ NAO doa para A+ (anti-A)");
        verificar(!compat(PFC, B_POS, AB_NEG), "PFC: B+ NAO doa para AB- (anti-A)");

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
        verificar(est.buscarPorId(12) != -1, "Indice: bolsa 12 encontrada por id");
        verificar(est.buscarPorId(99) == -1, "Indice: bolsa 99 inexistente");

        System.out.print("-- Alocacao integrada (matriz + hash + FEFO) --\n");
        ServicoAlocacao servico = new ServicoAlocacao(est);
        Alocacao a = servico.alocar(new Pedido(1, 1, A_POS, CH, 1, 60), 292);
        verificar(a.quantidade() == 1 && a.getBolsas().get(0).getId() == 12,
                "Alocacao: A+ recebe primeiro o tipo identico (12)");
        a = servico.alocar(new Pedido(2, 1, A_POS, CH, 1, 60), 292);
        verificar(a.quantidade() == 1 && a.getBolsas().get(0).getId() == 11,
                "Alocacao: sem A+, usa O- compativel que vence antes (11)");
        verificar(est.buscarPorId(11) == -1, "Alocacao: bolsa alocada sai do indice por id");
        Pedido p3 = new Pedido(3, 1, B_NEG, CH, 3, 60);
        a = servico.alocar(p3, 292);
        verificar(a.quantidade() == 1 && a.getBolsas().get(0).getId() == 10 && est.getDescartadas() == 1,
                "Alocacao: B- descarta a vencida (13) e atende parcial com O- (10)");
        boolean todasCompat = true;
        for (Bolsa b : a.getBolsas())
            if (!compat(b.getComponente(), b.getTipo(), p3.getReceptor())) todasCompat = false;
        verificar(todasCompat, "Alocacao: nenhuma bolsa incompativel foi entregue");
        a = servico.alocar(new Pedido(4, 1, O_NEG, PFC, 1, 60), 292);
        verificar(a.quantidade() == 1 && a.getBolsas().get(0).getTipo() == AB_POS,
                "Alocacao: plasma O- aceita AB+ (regra invertida)");
        a = servico.alocar(new Pedido(5, 1, AB_POS, PLQ, 1, 60), 292);
        verificar(a.quantidade() == 0, "Alocacao: sem estoque compativel -> 0 bolsas");

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

        System.out.print("-- Grafo e rota --\n");
        Grafo g = new Grafo();
        verificar(g.dist(0, 5) == 60, "Floyd: Hemocentro -> Epsilon = 60 min (via Gama)");
        verificar(g.dist(1, 3) == 30, "Floyd: Alfa -> Gama = 30 min (via Beta)");
        Roteirizador.Parada[] pr = {
            new Roteirizador.Parada(3, 1, 999), new Roteirizador.Parada(1, 2, 999)
        };
        int total = new Roteirizador(g).vizinhoMaisProximo(0, pr);
        verificar(pr[0].getLocal() == 1 && pr[1].getLocal() == 3 && total == 85,
                "Vizinho mais proximo: visita Alfa e depois Gama, total 85 min");

        System.out.printf("Resultado: %d OK, %d falha(s)\n\n", ok, falhas);
        return falhas;
    }

    public static void main(String[] args) {
        System.exit(new TestesRotaVital().executar() == 0 ? 0 : 1);
    }
}
