package rotavital.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rotavital.Alocacao;
import rotavital.Bolsa;
import rotavital.Busca;
import rotavital.Componente;
import rotavital.Datas;
import rotavital.Estoque;
import rotavital.FilaPedidos;
import rotavital.Grafo;
import rotavital.HashEstoque;
import rotavital.MatrizCompatibilidade;
import rotavital.Ordenacao;
import rotavital.Pedido;
import rotavital.Roteirizador;
import rotavital.ServicoAlocacao;
import rotavital.TipoSanguineo;

/**
 * ROTA VITAL - servidor local (localhost) da versão Java.
 *
 * CAMADA WEB: não tem equivalente em C++ e NÃO contém algoritmo.
 * Ela só recebe requisições HTTP e chama as MESMAS classes usadas no console
 * (Estoque, ServicoAlocacao, FilaPedidos, Grafo, Roteirizador, Ordenacao,
 * Busca, MatrizCompatibilidade). Essas classes continuam idênticas à
 * tradução linha a linha do rota_vital.cpp.
 *
 *     C++ (console)   main() -> funções sobre structs -> printf
 *     Java (console)  RotaVitalApp.main() -> objetos -> System.out.printf
 *     Java (web)      ServidorRotaVital -> MESMOS objetos -> JSON -> navegador
 *
 * Usa apenas o servidor HTTP que já vem no JDK (com.sun.net.httpserver):
 * sem Spring, sem Maven, sem bibliotecas para baixar.
 *
 * Compilar:  javac -encoding UTF-8 -d out src/rotavital/*.java src/rotavital/web/*.java
 * Executar:  java -cp out rotavital.web.ServidorRotaVital      (porta 8080)
 * Abrir:     http://localhost:8080
 */
public class ServidorRotaVital {

    // ---- estado da aplicação (um único hemocentro simulado) ----
    private Estoque estoque;
    private ServicoAlocacao servico;
    private FilaPedidos fila;
    private final List<Pedido> aguardando = new ArrayList<>();   // espelho da fila só para EXIBIR
    private final List<Alocacao> alocacoes = new ArrayList<>();
    private final Grafo grafo = new Grafo();
    private int proximoId;

    private void reiniciar() {
        estoque = new Estoque();
        for (Bolsa b : CenarioSintetico.carga()) estoque.receber(b);
        servico = new ServicoAlocacao(estoque);
        fila = new FilaPedidos();
        aguardando.clear();
        alocacoes.clear();
        proximoId = 1;
    }

    // ------------------------------------------------------------ endpoints
    private String estado() {
        List<String> slots = new ArrayList<>();
        HashEstoque h = estoque.getPorTipo();
        for (int i = 0; i < HashEstoque.TAM_HASH; i++) {
            List<String> entradas = new ArrayList<>();
            for (HashEstoque.Entrada en = h.primeiraDoSlot(i); en != null; en = HashEstoque.proxima(en)) {
                List<String> bolsas = new ArrayList<>();
                en.getFefo().paraCada(b -> bolsas.add(bolsaJson(b)));
                entradas.add(Json.obj("componente", en.getComponente().name(), "tipo", en.getTipo().toString(),
                        "bolsas", Json.bruto(Json.arr(bolsas))));
            }
            slots.add(Json.obj("indice", i, "entradas", Json.bruto(Json.arr(entradas))));
        }
        List<String> filaJson = new ArrayList<>();
        for (Pedido p : aguardando) filaJson.add(pedidoJson(p));
        List<String> alocJson = new ArrayList<>();
        for (Alocacao a : alocacoes) alocJson.add(alocacaoJson(a));

        List<String> tipos = new ArrayList<>();
        for (TipoSanguineo t : TipoSanguineo.values())
            tipos.add(Json.obj("nome", t.name(), "rotulo", t.toString()));
        List<String> hospitais = new ArrayList<>();
        for (int i = 1; i < Grafo.V; i++) hospitais.add(Json.obj("indice", i, "nome", Grafo.NOME_LOCAL[i]));

        return Json.obj(
                "hoje", Datas.formatar(CenarioSintetico.HOJE),
                "total", estoque.getTotal(),
                "descartadas", estoque.getDescartadas(),
                "colisoes", h.getColisoes(),
                "tamanhoHash", HashEstoque.TAM_HASH,
                "slots", Json.bruto(Json.arr(slots)),
                "fila", Json.bruto(Json.arr(filaJson)),
                "alocacoes", Json.bruto(Json.arr(alocJson)),
                "tipos", Json.bruto(Json.arr(tipos)),
                "hospitais", Json.bruto(Json.arr(hospitais)));
    }

    /** POST /api/pedido : entra na FILA FIFO (enqueue), como no console. */
    private String novoPedido(Map<String, String> f) {
        int hospital = inteiro(f, "hospital", 1, Grafo.V - 1);
        TipoSanguineo receptor = enumDe(TipoSanguineo.class, f.get("receptor"), "receptor");
        Componente comp = enumDe(Componente.class, f.get("componente"), "componente");
        int qtd = inteiro(f, "quantidade", 1, ServicoAlocacao.MAX_POR_PEDIDO);
        int janela = inteiro(f, "janela", 1, 600);
        Pedido p = new Pedido(proximoId++, hospital, receptor, comp, qtd, janela);
        fila.enqueue(p);
        aguardando.add(p);
        return Json.obj("ok", true, "pedido", Json.bruto(pedidoJson(p)), "naFila", fila.tamanho());
    }

    /** POST /api/exemplo : enfileira os 6 pedidos do cenário do console. */
    private String pedidosExemplo() {
        for (Pedido p : CenarioSintetico.pedidosExemplo(proximoId)) {
            fila.enqueue(p);
            aguardando.add(p);
            proximoId++;
        }
        return Json.obj("ok", true, "naFila", fila.tamanho());
    }

    /** POST /api/processar : dequeue até esvaziar -> ServicoAlocacao.alocar(). */
    private String processarFila() {
        int descartadasAntes = estoque.getDescartadas();
        List<String> resultados = new ArrayList<>();
        Pedido p;
        while ((p = fila.dequeue()) != null) {           // mesmo laço do RotaVitalApp
            Alocacao a = servico.alocar(p, CenarioSintetico.HOJE);
            resultados.add(alocacaoJson(a));
            if (a.quantidade() > 0) alocacoes.add(a);
        }
        aguardando.clear();
        return Json.obj("ok", true, "resultados", Json.bruto(Json.arr(resultados)),
                "descartadasAgora", estoque.getDescartadas() - descartadasAntes);
    }

    /** POST /api/vencidas : varredura diária (Estoque.descartarTodasVencidas). */
    private String varrerVencidas() {
        int n = estoque.descartarTodasVencidas(CenarioSintetico.HOJE);
        return Json.obj("ok", true, "descartadas", n, "total", estoque.getTotal());
    }

    /** GET /api/rota : Floyd-Warshall (no Grafo) + vizinho mais próximo. */
    private String rota() {
        Roteirizador.Parada[] paradas = new Roteirizador.Parada[alocacoes.size()];
        for (int i = 0; i < paradas.length; i++) {
            Pedido ped = alocacoes.get(i).getPedido();
            paradas[i] = new Roteirizador.Parada(ped.getHospital(), ped.getId(), ped.getJanelaMin());
        }
        int total = new Roteirizador(grafo).vizinhoMaisProximo(0, paradas);
        List<String> lista = new ArrayList<>();
        int anterior = 0, alertas = 0;
        for (int i = 0; i < paradas.length; i++) {
            Roteirizador.Parada pa = paradas[i];
            if (!pa.noPrazo() || !pa.cadeiaFriaOk()) alertas++;
            lista.add(Json.obj("ordem", i + 1, "pedidoId", pa.getPedidoId(), "local", pa.getLocal(),
                    "nome", Grafo.NOME_LOCAL[pa.getLocal()], "chegada", pa.getChegadaMin(),
                    "janela", pa.getJanelaMin(), "noPrazo", pa.noPrazo(), "cadeiaFriaOk", pa.cadeiaFriaOk(),
                    "trajeto", grafo.caminho(anterior, pa.getLocal()),
                    "vertices", Json.bruto(verticesDoCaminho(anterior, pa.getLocal()))));
            anterior = pa.getLocal();
        }
        return Json.obj("paradas", Json.bruto(Json.arr(lista)), "total", total, "alertas", alertas,
                "retorno", grafo.caminho(anterior, 0),
                "verticesRetorno", Json.bruto(verticesDoCaminho(anterior, 0)),
                "limiteCadeiaFria", Roteirizador.LIMITE_CADEIA_FRIA_MIN);
    }

    /** GET /api/compat?componente=CH&receptor=A_POS : linha/coluna da matriz. */
    private String compat(Map<String, String> q) {
        Componente comp = enumDe(Componente.class, q.getOrDefault("componente", "CH"), "componente");
        List<String> linhas = new ArrayList<>();
        for (TipoSanguineo d : TipoSanguineo.values()) {
            List<String> l = new ArrayList<>();
            for (TipoSanguineo r : TipoSanguineo.values())
                l.add(String.valueOf(MatrizCompatibilidade.compativel(comp, d, r)));
            linhas.add(Json.arr(l));
        }
        return Json.obj("componente", comp.name(), "matriz", Json.bruto(Json.arr(linhas)));
    }

    /** GET /api/relatorio : as 6 ordenações + busca binária (iterativa e recursiva). */
    private String relatorio() {
        Bolsa[] restante = estoque.exportar();
        String[] nomes = {"Bubble Sort", "Selection Sort", "Insertion Sort", "Shell Sort", "Quick Sort", "Merge Sort"};
        Ordenacao ord = new Ordenacao();
        List<String> cmps = new ArrayList<>();
        Bolsa[] copia = restante.clone();
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
            cmps.add(Json.obj("algoritmo", nomes[alg], "comparacoes", ord.getComparacoes(),
                    "ordenado", Ordenacao.estaOrdenado(copia)));
        }
        int limite = CenarioSintetico.HOJE + 7;
        int qIt = Busca.primeiroAposIterativa(copia, limite);
        int qRec = Busca.primeiroAposRecursiva(copia, 0, copia.length, limite);
        List<String> vencendo = new ArrayList<>();
        for (int i = 0; i < qIt; i++) vencendo.add(bolsaJson(copia[i]));
        return Json.obj("n", restante.length, "comparacoes", Json.bruto(Json.arr(cmps)),
                "limite", Datas.formatar(limite), "iterativa", qIt, "recursiva", qRec,
                "vencendo", Json.bruto(Json.arr(vencendo)));
    }

    /** GET /api/bolsa?id=1008 : índice por id (hash com sondagem linear). */
    private String buscarBolsa(Map<String, String> q) {
        int id = inteiro(q, "id", 0, Integer.MAX_VALUE);
        int slot = estoque.buscarPorId(id);
        return Json.obj("id", id, "emEstoque", slot != -1, "slot", slot);
    }

    // ------------------------------------------------------------ JSON
    private static String bolsaJson(Bolsa b) {
        return Json.obj("id", b.getId(), "tipo", b.getTipo().toString(), "componente", b.getComponente().name(),
                "validade", b.getValidade(), "data", Datas.formatar(b.getValidade()));
    }

    private static String pedidoJson(Pedido p) {
        return Json.obj("id", p.getId(), "hospital", p.getHospital(), "hospitalNome", Grafo.NOME_LOCAL[p.getHospital()],
                "receptor", p.getReceptor().toString(), "componente", p.getComponente().name(),
                "quantidade", p.getQuantidade(), "janela", p.getJanelaMin());
    }

    private static String alocacaoJson(Alocacao a) {
        List<String> bolsas = new ArrayList<>();
        for (Bolsa b : a.getBolsas())
            bolsas.add(Json.obj("bolsa", Json.bruto(bolsaJson(b)),
                    "tipoCompativel", b.getTipo() != a.getPedido().getReceptor()));
        String status = a.atendidoTotalmente() ? "ATENDIDO" : (a.quantidade() > 0 ? "PARCIAL" : "NAO ATENDIDO");
        return Json.obj("pedido", Json.bruto(pedidoJson(a.getPedido())), "bolsas", Json.bruto(Json.arr(bolsas)),
                "obtidas", a.quantidade(), "status", status);
    }

    /** Converte "Hemocentro Central -> Hospital Alfa" em [0,1] para o desenho. */
    private String verticesDoCaminho(int u, int v) {
        List<String> ids = new ArrayList<>();
        for (String nome : grafo.caminho(u, v).split(" -> "))
            for (int i = 0; i < Grafo.V; i++)
                if (Grafo.NOME_LOCAL[i].equals(nome)) ids.add(String.valueOf(i));
        return Json.arr(ids);
    }

    // ------------------------------------------------------------ HTTP
    private static final class ErroRequisicao extends RuntimeException {
        private static final long serialVersionUID = 1L;
        ErroRequisicao(String msg) { super(msg); }
    }

    private interface Acao { String executar(Map<String, String> params); }

    private void rota(HttpServer srv, String caminho, String metodo, Acao acao) {
        srv.createContext(caminho, ex -> {
            try {
                if (!ex.getRequestURI().getPath().equals(caminho)) { responder(ex, 404, Json.obj("erro", "não encontrado")); return; }
                if (!ex.getRequestMethod().equals(metodo)) { responder(ex, 405, Json.obj("erro", "use " + metodo)); return; }
                Map<String, String> params = parametros(ex);
                String json;
                synchronized (this) { json = acao.executar(params); }   // um pedido por vez no estoque
                responder(ex, 200, json);
            } catch (ErroRequisicao e) {
                responder(ex, 400, Json.obj("erro", e.getMessage()));
            } catch (RuntimeException e) {
                e.printStackTrace();
                responder(ex, 500, Json.obj("erro", "erro interno: " + e));
            }
        });
    }

    private static Map<String, String> parametros(HttpExchange ex) throws IOException {
        Map<String, String> m = new HashMap<>();
        String query = ex.getRequestURI().getRawQuery();
        if (query != null) lerFormulario(query, m);
        if (ex.getRequestMethod().equals("POST")) {
            try (InputStream in = ex.getRequestBody()) {
                lerFormulario(new String(in.readAllBytes(), StandardCharsets.UTF_8), m);
            }
        }
        return m;
    }

    private static void lerFormulario(String texto, Map<String, String> m) {
        for (String par : texto.split("&")) {
            if (par.isEmpty()) continue;
            int eq = par.indexOf('=');
            String k = eq < 0 ? par : par.substring(0, eq);
            String v = eq < 0 ? "" : par.substring(eq + 1);
            m.put(URLDecoder.decode(k, StandardCharsets.UTF_8), URLDecoder.decode(v, StandardCharsets.UTF_8));
        }
    }

    private static void responder(HttpExchange ex, int status, String corpo) throws IOException {
        responder(ex, status, corpo, "application/json; charset=utf-8");
    }

    private static void responder(HttpExchange ex, int status, String corpo, String tipo) throws IOException {
        byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", tipo);
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = ex.getResponseBody()) { out.write(bytes); }
    }

    private static int inteiro(Map<String, String> m, String campo, int min, int max) {
        try {
            int v = Integer.parseInt(m.getOrDefault(campo, "").trim());
            if (v < min || v > max) throw new ErroRequisicao(campo + " deve estar entre " + min + " e " + max);
            return v;
        } catch (NumberFormatException e) {
            throw new ErroRequisicao(campo + " inválido");
        }
    }

    private static <E extends Enum<E>> E enumDe(Class<E> tipo, String valor, String campo) {
        try {
            return Enum.valueOf(tipo, valor == null ? "" : valor);
        } catch (IllegalArgumentException e) {
            throw new ErroRequisicao(campo + " inválido: " + valor);
        }
    }

    // ------------------------------------------------------------ main
    public static void main(String[] args) throws IOException {
        int porta = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        ServidorRotaVital app = new ServidorRotaVital();
        app.reiniciar();

        // 127.0.0.1: só aceita conexões desta máquina (localhost)
        HttpServer srv = HttpServer.create(new InetSocketAddress("127.0.0.1", porta), 0);
        srv.createContext("/", ex -> {
            if (ex.getRequestURI().getPath().equals("/"))
                responder(ex, 200, PaginaWeb.HTML, "text/html; charset=utf-8");
            else
                responder(ex, 404, "não encontrado", "text/plain; charset=utf-8");
        });
        app.rota(srv, "/api/estado", "GET", p -> app.estado());
        app.rota(srv, "/api/pedido", "POST", app::novoPedido);
        app.rota(srv, "/api/exemplo", "POST", p -> app.pedidosExemplo());
        app.rota(srv, "/api/processar", "POST", p -> app.processarFila());
        app.rota(srv, "/api/vencidas", "POST", p -> app.varrerVencidas());
        app.rota(srv, "/api/rota", "GET", p -> app.rota());
        app.rota(srv, "/api/compat", "GET", app::compat);
        app.rota(srv, "/api/relatorio", "GET", p -> app.relatorio());
        app.rota(srv, "/api/bolsa", "GET", app::buscarBolsa);
        app.rota(srv, "/api/reset", "POST", p -> { app.reiniciar(); return Json.obj("ok", true); });
        srv.start();

        System.out.println("ROTA VITAL - servidor local no ar");
        System.out.println("Abra no navegador: http://localhost:" + porta);
        System.out.println("Para parar: Ctrl+C");
    }
}
