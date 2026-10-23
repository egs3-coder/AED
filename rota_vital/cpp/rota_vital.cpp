/* ============================================================================
 *  ROTA VITAL - Gestão e distribuição de hemocomponentes na rede de sangue
 *  Projeto Integrador - Unidade 2 - VERSÃO C/C++
 *
 *  Algoritmos implementados "na mão" (sem containers da STL), no estilo das
 *  aulas 02 a 08:
 *
 *    1. Compatibilidade ABO/Rh ...... matriz 8x8 [doador][receptor]      O(1)
 *    2. Priorização FEFO ............ lista encadeada com inserção
 *                                     ordenada por validade               O(m)
 *    3. Índice de estoque por hash .. (a) encadeamento externo:
 *                                         chave (componente,tipo) -> lista FEFO
 *                                     (b) sondagem linear: id -> bolsa
 *    4. Roteirização ................ Floyd-Warshall (caminhos mínimos)
 *                                     + heurística do vizinho mais próximo
 *
 *    Apoio: fila FIFO de pedidos (aula 03), ordenações bubble/selection/
 *    insertion/shell/quick/merge (aulas 07-08), busca sequencial e binária
 *    iterativa + recursiva (aulas 02 e 06).
 *
 *  AVISO: dados 100% sintéticos (LGPD). A matriz ABO/Rh é DIDÁTICA e não
 *  substitui protocolo clínico/imuno-hematológico. Sem integração com
 *  sistemas oficiais da Hemorrede.
 *
 *  Compilar:  g++ -std=c++17 -Wall -Wextra -o rota_vital rota_vital.cpp
 *  Executar:  ./rota_vital
 * ==========================================================================*/
#include <cstdio>
#include <utility>   // std::swap

// ---------------------------------------------------------------------------
// 0. Datas sintéticas: "dia" = dia do ano (1 = 01/01/2026). Inteiro facilita
//    comparar validades (FEFO) com um único "<".
// ---------------------------------------------------------------------------
const char* data(int dia) {
    static char buffers[4][16];          // 4 buffers rotativos: permite usar
    static int proximo = 0;              // data() várias vezes no mesmo printf
    static const int DIAS_MES[12] = {31,28,31,30,31,30,31,31,30,31,30,31};
    char* buf = buffers[proximo];
    proximo = (proximo + 1) % 4;
    int ano = 2026;
    while (dia > 365) { dia -= 365; ano++; }      // simplificação didática
    int mes = 0;
    while (dia > DIAS_MES[mes]) { dia -= DIAS_MES[mes]; mes++; }
    snprintf(buf, 16, "%02d/%02d/%d", dia, mes + 1, ano);
    return buf;
}

bool g_verboso = true;   // desliga mensagens de descarte durante os testes

// ---------------------------------------------------------------------------
// 1. Tipos sanguíneos, componentes e MATRIZ DE COMPATIBILIDADE ABO/Rh
// ---------------------------------------------------------------------------
const int N_TIPOS = 8;
enum Tipo { O_NEG, O_POS, A_NEG, A_POS, B_NEG, B_POS, AB_NEG, AB_POS };
const char* NOME_TIPO[N_TIPOS] = {"O-", "O+", "A-", "A+", "B-", "B+", "AB-", "AB+"};

const int N_COMP = 3;
enum Componente { CH, PFC, PLQ };   // concentrado de hemácias, plasma, plaquetas
const char* NOME_COMP[N_COMP] = {"CH", "PFC", "PLQ"};

// Hemácias (e, didaticamente, plaquetas): o DOADOR não pode levar antígeno
// (A, B ou D/Rh) que o RECEPTOR não tenha. O- = doador universal.
const int COMPAT_HEMACIAS[N_TIPOS][N_TIPOS] = {
    //            receptor: O-  O+  A-  A+  B-  B+ AB- AB+
    /* doador O-  */      { 1,  1,  1,  1,  1,  1,  1,  1 },
    /* doador O+  */      { 0,  1,  0,  1,  0,  1,  0,  1 },
    /* doador A-  */      { 0,  0,  1,  1,  0,  0,  1,  1 },
    /* doador A+  */      { 0,  0,  0,  1,  0,  0,  0,  1 },
    /* doador B-  */      { 0,  0,  0,  0,  1,  1,  1,  1 },
    /* doador B+  */      { 0,  0,  0,  0,  0,  1,  0,  1 },
    /* doador AB- */      { 0,  0,  0,  0,  0,  0,  1,  1 },
    /* doador AB+ */      { 0,  0,  0,  0,  0,  0,  0,  1 },
};

// Plasma: lógica INVERTIDA (o plasma leva anticorpos). AB = doador universal
// de plasma; o Rh não é considerado para plasma.
const int COMPAT_PLASMA[N_TIPOS][N_TIPOS] = {
    //            receptor: O-  O+  A-  A+  B-  B+ AB- AB+
    /* doador O-  */      { 1,  1,  0,  0,  0,  0,  0,  0 },
    /* doador O+  */      { 1,  1,  0,  0,  0,  0,  0,  0 },
    /* doador A-  */      { 1,  1,  1,  1,  0,  0,  0,  0 },
    /* doador A+  */      { 1,  1,  1,  1,  0,  0,  0,  0 },
    /* doador B-  */      { 1,  1,  0,  0,  1,  1,  0,  0 },
    /* doador B+  */      { 1,  1,  0,  0,  1,  1,  0,  0 },
    /* doador AB- */      { 1,  1,  1,  1,  1,  1,  1,  1 },
    /* doador AB+ */      { 1,  1,  1,  1,  1,  1,  1,  1 },
};

// O(1): dois índices de array, sem laço.
bool compativel(Componente comp, Tipo doador, Tipo receptor) {
    if (comp == PFC) return COMPAT_PLASMA[doador][receptor] == 1;
    return COMPAT_HEMACIAS[doador][receptor] == 1;
}

// ---------------------------------------------------------------------------
// 2. Bolsa e critério FEFO (First Expired, First Out)
// ---------------------------------------------------------------------------
struct Bolsa {
    int id;
    Tipo tipo;
    Componente comp;
    int validade;          // dia do ano
};

Bolsa novaBolsa(int id, Tipo t, Componente c, int validade) {
    Bolsa b;
    b.id = id; b.tipo = t; b.comp = c; b.validade = validade;
    return b;
}

// Ordem total: validade crescente; empate -> id crescente (desempate
// determinístico, deixa todos os algoritmos de ordenação com o mesmo resultado).
bool vemAntes(const Bolsa& a, const Bolsa& b) {
    if (a.validade != b.validade) return a.validade < b.validade;
    return a.id < b.id;
}

// ---------------------------------------------------------------------------
// 3. Lista FEFO: lista simplesmente encadeada mantida ORDENADA (aula 03)
// ---------------------------------------------------------------------------
struct NoBolsa {
    Bolsa bolsa;
    NoBolsa* prox;
};

// Inserção ordenada. Pior caso O(m), m = bolsas da lista (percorre até o fim).
NoBolsa* fefoInserir(NoBolsa* head, Bolsa b) {
    NoBolsa* novo = new NoBolsa;
    novo->bolsa = b;
    novo->prox = NULL;
    if (head == NULL || vemAntes(b, head->bolsa)) {   // vira a nova cabeça
        novo->prox = head;
        return novo;
    }
    NoBolsa* atual = head;
    while (atual->prox != NULL && !vemAntes(b, atual->prox->bolsa))
        atual = atual->prox;
    novo->prox = atual->prox;                          // religa os ponteiros
    atual->prox = novo;
    return head;
}

// Remove a cabeça (a que vence primeiro). O(1).
NoBolsa* fefoRemoverPrimeiro(NoBolsa* head, Bolsa* removida) {
    *removida = head->bolsa;
    NoBolsa* temp = head;
    head = head->prox;
    delete temp;
    return head;
}

// ---------------------------------------------------------------------------
// 4a. HASH DO ESTOQUE com ENCADEAMENTO (aula 06)
//     chave = componente * 8 + tipo  (24 chaves possíveis)
//     cada entrada guarda a lista FEFO daquela combinação
// ---------------------------------------------------------------------------
const int TAM_HASH = 7;   // primo pequeno de propósito: força colisões visíveis

struct EntradaEstoque {
    int chave;
    NoBolsa* fefo;          // cabeça = bolsa que vence primeiro
    int quantidade;
    EntradaEstoque* prox;   // próxima entrada na cadeia de colisão
};

struct HashEstoque {
    EntradaEstoque* slots[TAM_HASH];
    int colisoes;
};

void hashInicializar(HashEstoque* h) {
    for (int i = 0; i < TAM_HASH; i++) h->slots[i] = NULL;
    h->colisoes = 0;
}

int chaveEstoque(Componente c, Tipo t) { return (int)c * N_TIPOS + (int)t; }
int funcaoHash(int chave) { return chave % TAM_HASH; }

// O(1 + alfa), alfa = fator de carga (no máx. 24/7 ~ 3,4 -> constante)
EntradaEstoque* hashBuscar(HashEstoque* h, int chave) {
    EntradaEstoque* atual = h->slots[funcaoHash(chave)];
    while (atual != NULL) {
        if (atual->chave == chave) return atual;
        atual = atual->prox;
    }
    return NULL;
}

EntradaEstoque* hashObterOuCriar(HashEstoque* h, int chave) {
    EntradaEstoque* e = hashBuscar(h, chave);
    if (e != NULL) return e;
    int indice = funcaoHash(chave);
    if (h->slots[indice] != NULL) h->colisoes++;
    e = new EntradaEstoque;
    e->chave = chave;
    e->fefo = NULL;
    e->quantidade = 0;
    e->prox = h->slots[indice];          // insere no início da cadeia
    h->slots[indice] = e;
    return e;
}

// ---------------------------------------------------------------------------
// 4b. ÍNDICE POR ID com SONDAGEM LINEAR (endereçamento aberto, aula 06)
//     Consulta "onde está a bolsa X?" em O(1) médio.
// ---------------------------------------------------------------------------
const int TAM_INDICE = 101;   // primo; fator de carga baixo (< 70%)
const int VAZIO = -1;
const int REMOVIDO = -2;

struct IndiceId {
    int chave[TAM_INDICE];
    Bolsa bolsa[TAM_INDICE];
};

void indiceInicializar(IndiceId* x) {
    for (int i = 0; i < TAM_INDICE; i++) x->chave[i] = VAZIO;
}

int indiceHash(int id) { return id % TAM_INDICE; }

int indiceInserir(IndiceId* x, Bolsa b) {
    int base = indiceHash(b.id);
    for (int i = 0; i < TAM_INDICE; i++) {
        int t = (base + i) % TAM_INDICE;
        if (x->chave[t] == VAZIO || x->chave[t] == REMOVIDO) {
            x->chave[t] = b.id;
            x->bolsa[t] = b;
            return t;
        }
    }
    return -1;   // tabela cheia
}

int indiceBuscar(IndiceId* x, int id) {
    int base = indiceHash(id);
    for (int i = 0; i < TAM_INDICE; i++) {
        int t = (base + i) % TAM_INDICE;
        if (x->chave[t] == VAZIO) return -1;     // nunca inserido
        if (x->chave[t] == id) return t;         // REMOVIDO é "pulado"
    }
    return -1;
}

bool indiceRemover(IndiceId* x, int id) {
    int pos = indiceBuscar(x, id);
    if (pos == -1) return false;
    x->chave[pos] = REMOVIDO;   // marcação: não quebra a cadeia de sondagem
    return true;
}

// ---------------------------------------------------------------------------
// 5. ESTOQUE = hash por (componente,tipo) + índice por id
// ---------------------------------------------------------------------------
struct Estoque {
    HashEstoque porTipo;
    IndiceId porId;
    int total;
    int descartadas;
};

void estoqueInicializar(Estoque* e) {
    hashInicializar(&e->porTipo);
    indiceInicializar(&e->porId);
    e->total = 0;
    e->descartadas = 0;
}

// O(1) para achar a lista + O(m) para a inserção ordenada
void estoqueReceber(Estoque* e, Bolsa b) {
    EntradaEstoque* en = hashObterOuCriar(&e->porTipo, chaveEstoque(b.comp, b.tipo));
    en->fefo = fefoInserir(en->fefo, b);
    en->quantidade++;
    indiceInserir(&e->porId, b);
    e->total++;
}

// Como a lista está em ordem FEFO, as vencidas formam um PREFIXO:
// basta remover pela cabeça enquanto venceu. O(v), v = vencidas.
int estoqueDescartarVencidas(Estoque* e, EntradaEstoque* en, int hoje) {
    int n = 0;
    while (en->fefo != NULL && en->fefo->bolsa.validade < hoje) {
        Bolsa b;
        en->fefo = fefoRemoverPrimeiro(en->fefo, &b);
        en->quantidade--;
        e->total--;
        e->descartadas++;
        indiceRemover(&e->porId, b.id);
        if (g_verboso)
            printf("   [descarte] bolsa %d (%s %s) vencida em %s\n",
                   b.id, NOME_COMP[b.comp], NOME_TIPO[b.tipo], data(b.validade));
        n++;
    }
    return n;
}

// Varredura diária de todo o estoque: O(TAM_HASH + chaves + v)
int estoqueDescartarTodasVencidas(Estoque* e, int hoje) {
    int n = 0;
    for (int i = 0; i < TAM_HASH; i++)
        for (EntradaEstoque* en = e->porTipo.slots[i]; en != NULL; en = en->prox)
            n += estoqueDescartarVencidas(e, en, hoje);
    return n;
}

Bolsa estoqueRetirar(Estoque* e, EntradaEstoque* en) {
    Bolsa b;
    en->fefo = fefoRemoverPrimeiro(en->fefo, &b);
    en->quantidade--;
    e->total--;
    indiceRemover(&e->porId, b.id);
    return b;
}

void estoqueImprimir(Estoque* e) {
    printf("Estoque: %d bolsas | hash %d slots | colisoes no hash: %d\n",
           e->total, TAM_HASH, e->porTipo.colisoes);
    for (int i = 0; i < TAM_HASH; i++) {
        printf("  slot %d:", i);
        EntradaEstoque* en = e->porTipo.slots[i];
        if (en == NULL) printf(" (vazio)");
        printf("\n");
        while (en != NULL) {
            Componente c = (Componente)(en->chave / N_TIPOS);
            Tipo t = (Tipo)(en->chave % N_TIPOS);
            printf("    [%s %s] qtd=%d:", NOME_COMP[c], NOME_TIPO[t], en->quantidade);
            for (NoBolsa* no = en->fefo; no != NULL; no = no->prox)
                printf(" %d(%s)", no->bolsa.id, data(no->bolsa.validade));
            printf("\n");
            en = en->prox;
        }
    }
}

// Copia o estoque para um vetor (para relatórios). O(n + TAM_HASH).
int estoqueExportar(Estoque* e, Bolsa saida[], int max) {
    int n = 0;
    for (int i = 0; i < TAM_HASH; i++)
        for (EntradaEstoque* en = e->porTipo.slots[i]; en != NULL; en = en->prox)
            for (NoBolsa* no = en->fefo; no != NULL && n < max; no = no->prox)
                saida[n++] = no->bolsa;
    return n;
}

void estoqueLiberar(Estoque* e) {
    for (int i = 0; i < TAM_HASH; i++) {
        EntradaEstoque* en = e->porTipo.slots[i];
        while (en != NULL) {
            NoBolsa* no = en->fefo;
            while (no != NULL) { NoBolsa* t = no; no = no->prox; delete t; }
            EntradaEstoque* t = en; en = en->prox; delete t;
        }
        e->porTipo.slots[i] = NULL;
    }
}

// ---------------------------------------------------------------------------
// 6. PEDIDOS em FILA FIFO encadeada (aula 03: enqueue/dequeue)
// ---------------------------------------------------------------------------
struct Pedido {
    int id;
    int hospital;           // vértice do grafo
    Tipo receptor;
    Componente comp;
    int quantidade;
    int janelaMin;          // prazo máximo de entrega (min após a saída)
};

struct NoPedido { Pedido pedido; NoPedido* prox; };
struct FilaPedidos { NoPedido* inicio; NoPedido* fim; int tamanho; };

void filaInicializar(FilaPedidos* f) { f->inicio = f->fim = NULL; f->tamanho = 0; }
bool filaVazia(FilaPedidos* f) { return f->inicio == NULL; }

void enqueue(FilaPedidos* f, Pedido p) {            // O(1): insere no fim
    NoPedido* novo = new NoPedido;
    novo->pedido = p;
    novo->prox = NULL;
    if (f->fim != NULL) f->fim->prox = novo;
    else f->inicio = novo;
    f->fim = novo;
    f->tamanho++;
}

bool dequeue(FilaPedidos* f, Pedido* saida) {       // O(1): remove do início
    if (f->inicio == NULL) return false;
    NoPedido* temp = f->inicio;
    *saida = temp->pedido;
    f->inicio = temp->prox;
    if (f->inicio == NULL) f->fim = NULL;
    delete temp;
    f->tamanho--;
    return true;
}

// ---------------------------------------------------------------------------
// 7. ALOCAÇÃO = matriz ABO/Rh + hash + FEFO trabalhando juntos
//    Regra: 1º tipo idêntico (FEFO); se faltar, entre os tipos compatíveis
//    pega a bolsa que vence primeiro (comparando só as CABEÇAS das listas).
// ---------------------------------------------------------------------------
const int MAX_POR_PEDIDO = 10;

struct Alocacao {
    Pedido pedido;
    Bolsa bolsas[MAX_POR_PEDIDO];
    int qtd;
};

int alocar(Estoque* e, Pedido p, int hoje, Alocacao* out) {
    out->pedido = p;
    out->qtd = 0;
    EntradaEstoque* candidatas[N_TIPOS];
    int nc = 0;
    EntradaEstoque* identica = NULL;

    // (a) 8 consultas O(1) na matriz + 8 consultas O(1) no hash
    for (int t = 0; t < N_TIPOS; t++) {
        if (!compativel(p.comp, (Tipo)t, p.receptor)) continue;
        EntradaEstoque* en = hashBuscar(&e->porTipo, chaveEstoque(p.comp, (Tipo)t));
        if (en == NULL) continue;
        estoqueDescartarVencidas(e, en, hoje);      // vencidas nunca saem
        candidatas[nc++] = en;
        if (t == (int)p.receptor) identica = en;
    }

    // (b) cada bolsa: compara no máx. 8 cabeças de lista -> O(1) por bolsa
    while (out->qtd < p.quantidade && out->qtd < MAX_POR_PEDIDO) {
        EntradaEstoque* escolhida = NULL;
        if (identica != NULL && identica->fefo != NULL) {
            escolhida = identica;
        } else {
            for (int i = 0; i < nc; i++) {
                if (candidatas[i]->fefo == NULL) continue;
                if (escolhida == NULL ||
                    vemAntes(candidatas[i]->fefo->bolsa, escolhida->fefo->bolsa))
                    escolhida = candidatas[i];
            }
        }
        if (escolhida == NULL) break;               // sem estoque compatível
        out->bolsas[out->qtd++] = estoqueRetirar(e, escolhida);
    }
    return out->qtd;
}

// ---------------------------------------------------------------------------
// 8. GRAFO + CAMINHOS MÍNIMOS (Floyd-Warshall) + VIZINHO MAIS PRÓXIMO
// ---------------------------------------------------------------------------
const int V = 6;
const int INF = 1000000;
const char* NOME_LOCAL[V] = {
    "Hemocentro Central", "Hospital Alfa", "Hospital Beta",
    "Hospital Gama", "Hospital Delta", "Hospital Epsilon"
};

// Tempo de deslocamento (min) por estrada direta - dados sintéticos
const int ESTRADAS[V][V] = {
    //  0    1    2    3    4    5
    {   0,  15,  25,  40, INF, INF },   // 0 Hemocentro Central
    {  15,   0,  12, INF,  30, INF },   // 1 Hospital Alfa
    {  25,  12,   0,  18,  22, INF },   // 2 Hospital Beta
    {  40, INF,  18,   0, INF,  20 },   // 3 Hospital Gama
    { INF,  30,  22, INF,   0,  16 },   // 4 Hospital Delta
    { INF, INF, INF,  20,  16,   0 },   // 5 Hospital Epsilon
};

struct Grafo {
    int dist[V][V];
    int prox[V][V];     // próximo vértice no caminho mínimo (reconstrução)
};

// O(V^3): três laços aninhados de V. Com V = 6 -> 216 relaxamentos.
void floydWarshall(Grafo* g) {
    for (int i = 0; i < V; i++)
        for (int j = 0; j < V; j++) {
            g->dist[i][j] = ESTRADAS[i][j];
            g->prox[i][j] = (ESTRADAS[i][j] < INF) ? j : -1;
        }
    for (int k = 0; k < V; k++)
        for (int i = 0; i < V; i++)
            for (int j = 0; j < V; j++)
                if (g->dist[i][k] + g->dist[k][j] < g->dist[i][j]) {
                    g->dist[i][j] = g->dist[i][k] + g->dist[k][j];
                    g->prox[i][j] = g->prox[i][k];
                }
}

void imprimirCaminho(const Grafo* g, int u, int v) {
    if (g->prox[u][v] == -1) { printf("(sem caminho)"); return; }
    printf("%s", NOME_LOCAL[u]);
    while (u != v) {
        u = g->prox[u][v];
        printf(" -> %s", NOME_LOCAL[u]);
    }
}

const int LIMITE_CADEIA_FRIA_MIN = 120;   // caixa térmica validada (sintético)

struct Parada {
    int local;
    int pedidoId;
    int janelaMin;
    int chegadaMin;
};

// Vizinho mais próximo: a cada passo escolhe, entre as paradas restantes, a
// mais perto da posição atual (mesma estrutura do Selection Sort!).
// O(k^2) com k paradas. Retorna o tempo total incluindo a volta à base.
int rotaVizinhoMaisProximo(const Grafo* g, int origem, Parada paradas[], int k) {
    int atual = origem;
    int tempo = 0;
    for (int pos = 0; pos < k; pos++) {
        int melhor = pos;
        for (int i = pos + 1; i < k; i++)
            if (g->dist[atual][paradas[i].local] < g->dist[atual][paradas[melhor].local])
                melhor = i;
        std::swap(paradas[pos], paradas[melhor]);
        tempo += g->dist[atual][paradas[pos].local];
        paradas[pos].chegadaMin = tempo;
        atual = paradas[pos].local;
    }
    return tempo + g->dist[atual][origem];
}

// ---------------------------------------------------------------------------
// 9. ORDENAÇÕES (aulas 07 e 08) - por validade (FEFO), com contador de
//    comparações para comparar o custo real de cada algoritmo
// ---------------------------------------------------------------------------
long long g_comparacoes = 0;

bool menor(const Bolsa& a, const Bolsa& b) {
    g_comparacoes++;
    return vemAntes(a, b);
}

void bubbleSort(Bolsa arr[], int n) {                     // O(n^2)
    for (int i = 0; i < n - 1; i++) {
        bool trocou = false;
        for (int j = 0; j < n - i - 1; j++)
            if (menor(arr[j + 1], arr[j])) { std::swap(arr[j], arr[j + 1]); trocou = true; }
        if (!trocou) break;                               // já ordenado: O(n)
    }
}

void selectionSort(Bolsa arr[], int n) {                  // O(n^2) sempre
    for (int i = 0; i < n - 1; i++) {
        int minIdx = i;
        for (int j = i + 1; j < n; j++)
            if (menor(arr[j], arr[minIdx])) minIdx = j;
        std::swap(arr[minIdx], arr[i]);
    }
}

void insertionSort(Bolsa arr[], int n) {                  // O(n^2), O(n) se quase ordenado
    for (int i = 1; i < n; i++) {
        Bolsa chave = arr[i];
        int j = i - 1;
        while (j >= 0 && menor(chave, arr[j])) { arr[j + 1] = arr[j]; j--; }
        arr[j + 1] = chave;
    }
}

void shellSort(Bolsa arr[], int n) {                      // gaps n/2: pior O(n^2)
    for (int gap = n / 2; gap > 0; gap /= 2)
        for (int i = gap; i < n; i++) {
            Bolsa temp = arr[i];
            int j;
            for (j = i; j >= gap && menor(temp, arr[j - gap]); j -= gap)
                arr[j] = arr[j - gap];
            arr[j] = temp;
        }
}

int particionar(Bolsa arr[], int low, int high) {          // Lomuto, pivô = último
    Bolsa pivo = arr[high];
    int i = low - 1;
    for (int j = low; j <= high - 1; j++)
        if (menor(arr[j], pivo)) { i++; std::swap(arr[i], arr[j]); }
    std::swap(arr[i + 1], arr[high]);
    return i + 1;
}

void quickSort(Bolsa arr[], int low, int high) {          // médio O(n log n), pior O(n^2)
    if (low < high) {
        int pi = particionar(arr, low, high);
        quickSort(arr, low, pi - 1);
        quickSort(arr, pi + 1, high);
    }
}

void intercalar(Bolsa arr[], int esq, int meio, int dir) {
    int n = dir - esq + 1;
    Bolsa* aux = new Bolsa[n];                            // memória extra O(n)
    int i = esq, j = meio + 1, k = 0;
    while (i <= meio && j <= dir) {
        if (menor(arr[j], arr[i])) aux[k++] = arr[j++];
        else aux[k++] = arr[i++];                         // empate: esquerda (estável)
    }
    while (i <= meio) aux[k++] = arr[i++];
    while (j <= dir) aux[k++] = arr[j++];
    for (k = 0; k < n; k++) arr[esq + k] = aux[k];
    delete[] aux;
}

void mergeSort(Bolsa arr[], int esq, int dir) {           // O(n log n) sempre
    if (esq >= dir) return;
    int meio = esq + (dir - esq) / 2;
    mergeSort(arr, esq, meio);
    mergeSort(arr, meio + 1, dir);
    intercalar(arr, esq, meio, dir);
}

bool estaOrdenado(Bolsa arr[], int n) {
    for (int i = 1; i < n; i++)
        if (vemAntes(arr[i], arr[i - 1])) return false;
    return true;
}

// ---------------------------------------------------------------------------
// 10. BUSCAS (aulas 02 e 06)
// ---------------------------------------------------------------------------
int buscaSequencialPorId(Bolsa arr[], int n, int id) {    // O(n)
    for (int i = 0; i < n; i++)
        if (arr[i].id == id) return i;
    return -1;
}

// Binária ITERATIVA: 1º índice com validade > limite, no vetor ORDENADO.
// Resultado = quantas bolsas vencem até "limite". O(log n).
int primeiroAposIterativa(Bolsa arr[], int n, int limite) {
    int inicio = 0, fim = n;                  // intervalo semiaberto [inicio, fim)
    while (inicio < fim) {
        int meio = inicio + (fim - inicio) / 2;
        if (arr[meio].validade <= limite) inicio = meio + 1;
        else fim = meio;
    }
    return inicio;
}

// Mesma busca na versão RECURSIVA (comparação aula 02). O(log n) tempo e pilha.
int primeiroAposRecursiva(Bolsa arr[], int inicio, int fim, int limite) {
    if (inicio >= fim) return inicio;         // caso base
    int meio = inicio + (fim - inicio) / 2;
    if (arr[meio].validade <= limite)
        return primeiroAposRecursiva(arr, meio + 1, fim, limite);
    return primeiroAposRecursiva(arr, inicio, meio, limite);
}

// ---------------------------------------------------------------------------
// 11. TESTES
// ---------------------------------------------------------------------------
int g_ok = 0, g_falhas = 0;

void verificar(bool cond, const char* descricao) {
    if (cond) { g_ok++; printf("  [OK]     %s\n", descricao); }
    else { g_falhas++; printf("  [FALHOU] %s\n", descricao); }
}

int executarTestes() {
    printf("=== TESTES AUTOMATIZADOS ===\n");
    g_verboso = false;

    printf("-- Compatibilidade de hemacias (CH) --\n");
    verificar(compativel(CH, O_NEG, AB_POS), "CH: O- doa para AB+ (doador universal)");
    verificar(compativel(CH, O_NEG, O_NEG), "CH: O- doa para O-");
    verificar(compativel(CH, A_NEG, A_POS), "CH: A- doa para A+");
    verificar(compativel(CH, AB_POS, AB_POS), "CH: AB+ doa para AB+");
    verificar(!compativel(CH, O_POS, O_NEG), "CH: O+ NAO doa para O- (Rh)");
    verificar(!compativel(CH, A_POS, O_POS), "CH: A+ NAO doa para O+ (antigeno A)");
    verificar(!compativel(CH, B_NEG, A_NEG), "CH: B- NAO doa para A- (antigeno B)");
    verificar(!compativel(CH, AB_POS, A_POS), "CH: AB+ NAO doa para A+");
    int doadoresONeg = 0, doadoresABPos = 0;
    for (int t = 0; t < N_TIPOS; t++) {
        if (compativel(CH, (Tipo)t, O_NEG)) doadoresONeg++;
        if (compativel(CH, (Tipo)t, AB_POS)) doadoresABPos++;
    }
    verificar(doadoresONeg == 1, "CH: receptor O- so recebe de O- (1 doador)");
    verificar(doadoresABPos == 8, "CH: receptor AB+ recebe de todos (8 doadores)");

    printf("-- Compatibilidade de plasma (PFC) --\n");
    verificar(compativel(PFC, AB_POS, O_NEG), "PFC: AB+ doa para O- (plasma universal)");
    verificar(compativel(PFC, A_NEG, O_POS), "PFC: A- doa para O+");
    verificar(!compativel(PFC, O_POS, A_POS), "PFC: O+ NAO doa para A+ (anti-A)");
    verificar(!compativel(PFC, B_POS, AB_NEG), "PFC: B+ NAO doa para AB- (anti-A)");

    printf("-- Lista FEFO (insercao ordenada) --\n");
    NoBolsa* lista = NULL;
    lista = fefoInserir(lista, novaBolsa(1, A_POS, CH, 300));
    lista = fefoInserir(lista, novaBolsa(2, A_POS, CH, 290));
    lista = fefoInserir(lista, novaBolsa(3, A_POS, CH, 310));
    lista = fefoInserir(lista, novaBolsa(4, A_POS, CH, 290));
    int ordem[4], k = 0;
    for (NoBolsa* no = lista; no != NULL; no = no->prox) ordem[k++] = no->bolsa.id;
    verificar(k == 4 && ordem[0] == 2 && ordem[1] == 4 && ordem[2] == 1 && ordem[3] == 3,
              "FEFO: ordem 2,4,1,3 (validade crescente, empate por id)");
    while (lista != NULL) { Bolsa b; lista = fefoRemoverPrimeiro(lista, &b); }

    printf("-- Hash do estoque e indice por id --\n");
    static Estoque est;
    estoqueInicializar(&est);
    estoqueReceber(&est, novaBolsa(10, O_NEG, CH, 300));
    estoqueReceber(&est, novaBolsa(11, O_NEG, CH, 295));
    estoqueReceber(&est, novaBolsa(12, A_POS, CH, 299));
    estoqueReceber(&est, novaBolsa(13, B_NEG, CH, 280));   // vencida
    estoqueReceber(&est, novaBolsa(14, AB_POS, PFC, 500));
    EntradaEstoque* en = hashBuscar(&est.porTipo, chaveEstoque(CH, O_NEG));
    verificar(en != NULL && en->quantidade == 2 && en->fefo->bolsa.id == 11,
              "Hash: [CH O-] tem 2 bolsas e a cabeca e a 11 (vence antes)");
    verificar(hashBuscar(&est.porTipo, chaveEstoque(PLQ, O_POS)) == NULL,
              "Hash: chave inexistente retorna NULL");
    verificar(indiceBuscar(&est.porId, 12) != -1, "Indice: bolsa 12 encontrada por id");
    verificar(indiceBuscar(&est.porId, 99) == -1, "Indice: bolsa 99 inexistente");

    printf("-- Alocacao integrada (matriz + hash + FEFO) --\n");
    Alocacao a;
    Pedido p1 = {1, 1, A_POS, CH, 1, 60};
    alocar(&est, p1, 292, &a);
    verificar(a.qtd == 1 && a.bolsas[0].id == 12, "Alocacao: A+ recebe primeiro o tipo identico (12)");
    Pedido p2 = {2, 1, A_POS, CH, 1, 60};
    alocar(&est, p2, 292, &a);
    verificar(a.qtd == 1 && a.bolsas[0].id == 11, "Alocacao: sem A+, usa O- compativel que vence antes (11)");
    verificar(indiceBuscar(&est.porId, 11) == -1, "Alocacao: bolsa alocada sai do indice por id");
    Pedido p3 = {3, 1, B_NEG, CH, 3, 60};
    alocar(&est, p3, 292, &a);
    verificar(a.qtd == 1 && a.bolsas[0].id == 10 && est.descartadas == 1,
              "Alocacao: B- descarta a vencida (13) e atende parcial com O- (10)");
    bool todasCompat = true;
    for (int i = 0; i < a.qtd; i++)
        if (!compativel(a.bolsas[i].comp, a.bolsas[i].tipo, p3.receptor)) todasCompat = false;
    verificar(todasCompat, "Alocacao: nenhuma bolsa incompativel foi entregue");
    Pedido p4 = {4, 1, O_NEG, PFC, 1, 60};
    alocar(&est, p4, 292, &a);
    verificar(a.qtd == 1 && a.bolsas[0].tipo == AB_POS, "Alocacao: plasma O- aceita AB+ (regra invertida)");
    Pedido p5 = {5, 1, AB_POS, PLQ, 1, 60};
    alocar(&est, p5, 292, &a);
    verificar(a.qtd == 0, "Alocacao: sem estoque compativel -> 0 bolsas");
    estoqueLiberar(&est);

    printf("-- Ordenacoes e buscas --\n");
    Bolsa base[8] = {
        novaBolsa(1, O_POS, CH, 310), novaBolsa(2, O_POS, CH, 295), novaBolsa(3, O_POS, CH, 300),
        novaBolsa(4, O_POS, CH, 295), novaBolsa(5, O_POS, CH, 320), novaBolsa(6, O_POS, CH, 290),
        novaBolsa(7, O_POS, CH, 305), novaBolsa(8, O_POS, CH, 299)
    };
    Bolsa v[6][8];
    for (int alg = 0; alg < 6; alg++)
        for (int i = 0; i < 8; i++) v[alg][i] = base[i];
    bubbleSort(v[0], 8); selectionSort(v[1], 8); insertionSort(v[2], 8);
    shellSort(v[3], 8); quickSort(v[4], 0, 7); mergeSort(v[5], 0, 7);
    bool iguais = true;
    for (int alg = 0; alg < 6; alg++) {
        if (!estaOrdenado(v[alg], 8)) iguais = false;
        for (int i = 0; i < 8; i++) if (v[alg][i].id != v[0][i].id) iguais = false;
    }
    verificar(iguais, "Ordenacao: os 6 algoritmos produzem a mesma ordem FEFO");
    int it = primeiroAposIterativa(v[0], 8, 300);
    int rc = primeiroAposRecursiva(v[0], 0, 8, 300);
    int seq = 0;
    for (int i = 0; i < 8; i++) if (base[i].validade <= 300) seq++;
    verificar(it == 5 && rc == 5 && seq == 5, "Busca binaria iterativa = recursiva = contagem sequencial (5)");
    verificar(buscaSequencialPorId(base, 8, 7) == 6, "Busca sequencial: id 7 na posicao 6");

    printf("-- Grafo e rota --\n");
    static Grafo g;
    floydWarshall(&g);
    verificar(g.dist[0][5] == 60, "Floyd: Hemocentro -> Epsilon = 60 min (via Gama)");
    verificar(g.dist[1][3] == 30, "Floyd: Alfa -> Gama = 30 min (via Beta)");
    Parada pr[2] = {{3, 1, 999, 0}, {1, 2, 999, 0}};
    int total = rotaVizinhoMaisProximo(&g, 0, pr, 2);
    verificar(pr[0].local == 1 && pr[1].local == 3 && total == 85,
              "Vizinho mais proximo: visita Alfa e depois Gama, total 85 min");

    g_verboso = true;
    printf("Resultado: %d OK, %d falha(s)\n\n", g_ok, g_falhas);
    return g_falhas;
}

// ---------------------------------------------------------------------------
// 12. PROGRAMA PRINCIPAL - cenário sintético completo
// ---------------------------------------------------------------------------
int main() {
    const int HOJE = 292;   // 19/10/2026
    printf("ROTA VITAL - Unidade 2 (C++) | hoje: %s | dados sinteticos\n\n", data(HOJE));

    int falhas = executarTestes();

    // --- Carga do estoque (sintético) ---
    static Estoque estoque;
    estoqueInicializar(&estoque);
    Bolsa carga[] = {
        novaBolsa(1001, O_NEG, CH, 300),  novaBolsa(1002, O_NEG, CH, 295),
        novaBolsa(1003, O_POS, CH, 310),  novaBolsa(1004, O_POS, CH, 293),
        novaBolsa(1005, O_POS, CH, 288),  novaBolsa(1006, A_POS, CH, 305),
        novaBolsa(1007, A_POS, CH, 297),  novaBolsa(1008, A_NEG, CH, 320),
        novaBolsa(1009, B_POS, CH, 299),  novaBolsa(1010, B_NEG, CH, 315),
        novaBolsa(1011, AB_POS, CH, 301), novaBolsa(1012, AB_NEG, CH, 290),
        novaBolsa(1013, A_POS, PFC, 600), novaBolsa(1014, AB_POS, PFC, 550),
        novaBolsa(1015, O_POS, PFC, 580), novaBolsa(1016, B_POS, PFC, 620),
        novaBolsa(1017, A_POS, PLQ, 294), novaBolsa(1018, O_POS, PLQ, 293),
        novaBolsa(1019, O_POS, PLQ, 291), novaBolsa(1020, AB_POS, PLQ, 296),
        novaBolsa(1021, A_POS, CH, 294),  novaBolsa(1022, O_NEG, CH, 302),
    };
    int nCarga = sizeof(carga) / sizeof(carga[0]);
    for (int i = 0; i < nCarga; i++) estoqueReceber(&estoque, carga[i]);
    printf("=== 1. ESTOQUE INICIAL (hash por componente+tipo, listas FEFO) ===\n");
    estoqueImprimir(&estoque);

    // --- Pedidos chegam em ordem e entram na FILA ---
    FilaPedidos fila;
    filaInicializar(&fila);
    Pedido pedidos[] = {
        {1, 3, A_POS, CH, 3, 90},     // Gama
        {2, 1, O_NEG, CH, 2, 60},     // Alfa
        {3, 4, AB_POS, PFC, 1, 75},   // Delta
        {4, 2, O_POS, CH, 3, 45},     // Beta
        {5, 5, B_NEG, CH, 2, 90},     // Epsilon
        {6, 1, O_POS, PLQ, 1, 60},    // Alfa
    };
    int nPed = sizeof(pedidos) / sizeof(pedidos[0]);
    for (int i = 0; i < nPed; i++) enqueue(&fila, pedidos[i]);

    printf("\n=== 2. VARREDURA DIARIA DE VENCIDAS ===\n");
    int vencidas = estoqueDescartarTodasVencidas(&estoque, HOJE);
    printf("Descartadas: %d | em estoque: %d\n", vencidas, estoque.total);

    printf("\n=== 3. ALOCACAO (fila FIFO -> matriz ABO/Rh -> hash -> FEFO) ===\n");
    Alocacao alocacoes[16];
    int nAloc = 0;
    Pedido p;
    while (dequeue(&fila, &p)) {
        printf("Pedido #%d | %s | %s %s x%d | janela %d min\n", p.id, NOME_LOCAL[p.hospital],
               NOME_COMP[p.comp], NOME_TIPO[p.receptor], p.quantidade, p.janelaMin);
        Alocacao* a = &alocacoes[nAloc];
        int obtidas = alocar(&estoque, p, HOJE, a);
        for (int i = 0; i < a->qtd; i++)
            printf("   -> bolsa %d (%s %s) validade %s%s\n", a->bolsas[i].id,
                   NOME_COMP[a->bolsas[i].comp], NOME_TIPO[a->bolsas[i].tipo],
                   data(a->bolsas[i].validade),
                   a->bolsas[i].tipo == p.receptor ? "" : "  [tipo compativel]");
        if (obtidas == p.quantidade) printf("   status: ATENDIDO\n");
        else if (obtidas > 0) printf("   status: PARCIAL (%d de %d) - acionar outro hemocentro\n", obtidas, p.quantidade);
        else printf("   status: NAO ATENDIDO\n");
        if (obtidas > 0) nAloc++;
    }

    printf("\n=== 4. ROTA (Floyd-Warshall + vizinho mais proximo + cadeia fria) ===\n");
    static Grafo grafo;
    floydWarshall(&grafo);
    Parada paradas[16];
    for (int i = 0; i < nAloc; i++) {
        paradas[i].local = alocacoes[i].pedido.hospital;
        paradas[i].pedidoId = alocacoes[i].pedido.id;
        paradas[i].janelaMin = alocacoes[i].pedido.janelaMin;
        paradas[i].chegadaMin = 0;
    }
    int total = rotaVizinhoMaisProximo(&grafo, 0, paradas, nAloc);
    int anterior = 0, alertas = 0;
    for (int i = 0; i < nAloc; i++) {
        bool noPrazo = paradas[i].chegadaMin <= paradas[i].janelaMin;
        bool frio = paradas[i].chegadaMin <= LIMITE_CADEIA_FRIA_MIN;
        printf("%d. Pedido #%d em %s | chegada %3d min | janela %3d | %s | cadeia fria %s\n",
               i + 1, paradas[i].pedidoId, NOME_LOCAL[paradas[i].local], paradas[i].chegadaMin,
               paradas[i].janelaMin, noPrazo ? "NO PRAZO" : "ATRASO  ",
               frio ? "OK" : "RISCO");
        printf("   trajeto: ");
        imprimirCaminho(&grafo, anterior, paradas[i].local);
        printf("\n");
        if (!noPrazo || !frio) alertas++;
        anterior = paradas[i].local;
    }
    printf("Retorno: ");
    imprimirCaminho(&grafo, anterior, 0);
    printf("\nTempo total do circuito: %d min | alertas: %d\n", total, alertas);
    if (alertas > 0)
        printf("Obs.: vizinho mais proximo e heuristica gulosa; nao garante a janela de todos.\n");

    printf("\n=== 5. RELATORIOS (ordenacao + buscas) ===\n");
    Bolsa restante[64];
    int n = estoqueExportar(&estoque, restante, 64);
    printf("Bolsas restantes: %d | descartadas por vencimento: %d\n", n, estoque.descartadas);
    const char* nomes[6] = {"Bubble Sort", "Selection Sort", "Insertion Sort",
                            "Shell Sort", "Quick Sort", "Merge Sort"};
    Bolsa copia[64];
    for (int alg = 0; alg < 6; alg++) {
        for (int i = 0; i < n; i++) copia[i] = restante[i];
        g_comparacoes = 0;
        switch (alg) {
            case 0: bubbleSort(copia, n); break;
            case 1: selectionSort(copia, n); break;
            case 2: insertionSort(copia, n); break;
            case 3: shellSort(copia, n); break;
            case 4: quickSort(copia, 0, n - 1); break;
            case 5: mergeSort(copia, 0, n - 1); break;
        }
        printf("  %-15s comparacoes: %4lld | ordenado: %s\n", nomes[alg], g_comparacoes,
               estaOrdenado(copia, n) ? "sim" : "NAO");
    }
    // 'copia' agora está ordenada (Merge Sort) -> busca binária
    int limite = HOJE + 7;
    int qIt = primeiroAposIterativa(copia, n, limite);
    int qRec = primeiroAposRecursiva(copia, 0, n, limite);
    printf("Vencem ate %s (busca binaria iterativa=%d, recursiva=%d):\n", data(limite), qIt, qRec);
    for (int i = 0; i < qIt; i++)
        printf("   bolsa %d (%s %s) validade %s\n", copia[i].id, NOME_COMP[copia[i].comp],
               NOME_TIPO[copia[i].tipo], data(copia[i].validade));

    int ids[3] = {1008, 1002, 1005};
    printf("Consulta por id (hash com sondagem linear x busca sequencial):\n");
    for (int i = 0; i < 3; i++) {
        int pos = indiceBuscar(&estoque.porId, ids[i]);
        int seq = buscaSequencialPorId(restante, n, ids[i]);
        if (pos != -1)
            printf("   %d: em estoque (slot %d do indice; posicao %d na busca sequencial)\n",
                   ids[i], pos, seq);
        else
            printf("   %d: fora do estoque (alocada ou descartada); sequencial=%d\n", ids[i], seq);
    }

    estoqueLiberar(&estoque);
    return falhas == 0 ? 0 : 1;
}
