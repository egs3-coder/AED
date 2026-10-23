package rotavital.web;

/**
 * Página única servida em http://localhost:8080 (HTML + CSS + JavaScript).
 * Camada de apresentação: não tem equivalente em C++. Toda decisão
 * (compatibilidade, FEFO, hash, rota, ordenação) é feita no servidor Java,
 * pelas mesmas classes traduzidas do C++; a página só mostra o resultado.
 *
 * Texto em bloco (""" ... """) exige Java 15 ou mais novo.
 */
final class PaginaWeb {
    private PaginaWeb() { }

    static final String HTML = """
<!doctype html>
<html lang="pt-BR">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Rota Vital</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link href="https://fonts.googleapis.com/css2?family=Barlow:wght@400;500;600;700&family=Barlow+Condensed:ital,wght@1,700;1,800&display=swap" rel="stylesheet">
<style>
  :root { --creme:#FDF6EC; --creme2:#F3E7D6; --laranja:#F26A1B; --preto:#1A1A1A; --ouro:#F2B233; --cinza:#6B645B; --linha:#E6D8C4; --ok:#2E7D4F; --ruim:#C0392B; }
  * { box-sizing:border-box; }
  body { margin:0; background:var(--creme); color:var(--preto); font-family:Barlow, system-ui, sans-serif; font-size:15px; }
  header { padding:22px 28px 16px; display:flex; flex-wrap:wrap; align-items:flex-end; gap:18px 32px; border-bottom:1px solid var(--linha); position:relative; overflow:hidden; }
  header::after { content:''; position:absolute; right:-60px; top:-60px; width:150px; height:150px; border-radius:50%; background:var(--laranja); }
  header::before { content:''; position:absolute; right:-25px; top:-25px; width:70px; height:70px; border-radius:50%; background:var(--preto); z-index:1; }
  h1 { font-family:'Barlow Condensed', Barlow, sans-serif; font-style:italic; font-weight:800; color:var(--laranja); font-size:44px; line-height:.95; margin:0; }
  .sub { color:var(--cinza); font-size:13px; letter-spacing:.12em; text-transform:uppercase; font-weight:600; margin-top:4px; }
  .stats { display:flex; gap:10px; flex-wrap:wrap; margin-right:110px; }
  .stat { background:#fff; border:1px solid var(--linha); border-radius:12px; padding:8px 14px; min-width:96px; }
  .stat b { display:block; font-family:'Barlow Condensed', sans-serif; font-style:italic; font-size:26px; color:var(--laranja); line-height:1; }
  .stat span { font-size:12px; color:var(--cinza); }
  main { padding:20px 28px 40px; display:grid; gap:18px; grid-template-columns:repeat(12, 1fr); }
  .card { background:#fff; border:1px solid var(--linha); border-radius:16px; padding:16px 18px; min-width:0; }
  .c4 { grid-column:span 4; } .c5 { grid-column:span 5; } .c6 { grid-column:span 6; } .c7 { grid-column:span 7; } .c8 { grid-column:span 8; } .c12 { grid-column:span 12; }
  @media (max-width:1100px) { .c4,.c5,.c6,.c7,.c8 { grid-column:span 12; } }
  h2 { font-family:'Barlow Condensed', sans-serif; font-style:italic; font-weight:800; font-size:22px; margin:0 0 4px; display:flex; align-items:center; gap:8px; }
  h2 .n { background:var(--preto); color:var(--creme); font-size:13px; border-radius:0 12px 12px 0; padding:2px 10px 2px 6px; font-style:italic; }
  .dica { color:var(--cinza); font-size:12.5px; margin:0 0 12px; }
  .dica code, td code { background:var(--creme2); padding:0 4px; border-radius:4px; font-size:12px; }
  label { display:block; font-size:12px; font-weight:600; color:var(--cinza); margin-bottom:3px; }
  select, input { width:100%; padding:8px 10px; border:1.5px solid var(--linha); border-radius:9px; font:inherit; background:var(--creme); }
  select:focus, input:focus { outline:2px solid var(--laranja); outline-offset:0; border-color:var(--laranja); }
  .form { display:grid; grid-template-columns:1fr 1fr; gap:10px; }
  .form .full { grid-column:span 2; }
  button { font:inherit; font-weight:700; border:0; border-radius:10px; padding:9px 14px; cursor:pointer; background:var(--preto); color:#fff; }
  button.lar { background:var(--laranja); }
  button.sec { background:var(--creme2); color:var(--preto); }
  button:hover { filter:brightness(1.1); }
  .botoes { display:flex; flex-wrap:wrap; gap:8px; margin-top:12px; }
  table { width:100%; border-collapse:collapse; font-size:13.5px; }
  th { text-align:left; font-family:'Barlow Condensed', sans-serif; font-weight:700; font-size:14px; background:var(--preto); color:var(--creme); padding:6px 8px; }
  td { border-bottom:1px solid var(--linha); padding:6px 8px; vertical-align:top; }
  .vazio { color:var(--cinza); font-style:italic; padding:10px 0; }
  .tag { display:inline-block; font-size:11.5px; font-weight:700; padding:1px 8px; border-radius:20px; background:var(--creme2); margin:1px 2px 1px 0; white-space:nowrap; }
  .tag.lar { background:var(--laranja); color:#fff; } .tag.preto { background:var(--preto); color:#fff; }
  .tag.ok { background:#DDEFE3; color:var(--ok); } .tag.ruim { background:#F7DAD6; color:var(--ruim); } .tag.parcial { background:#FBEBC8; color:#8A5A00; }
  .slot { display:flex; align-items:flex-start; gap:6px; margin:6px 0; flex-wrap:wrap; }
  .slot-i { width:30px; height:30px; flex:none; background:var(--preto); color:var(--creme); border-radius:7px; display:flex; align-items:center; justify-content:center; font-family:'Barlow Condensed', sans-serif; font-weight:800; font-size:17px; }
  .ent { border:1.5px solid var(--preto); border-radius:8px; padding:3px 8px; background:#fff; font-size:12.5px; }
  .ent b { font-family:'Barlow Condensed', sans-serif; font-size:15px; }
  .ent small { display:block; color:var(--cinza); font-family:ui-monospace, Consolas, monospace; font-size:11px; }
  .seta { color:var(--laranja); font-weight:800; align-self:center; }
  .null { color:var(--cinza); font-family:ui-monospace, Consolas, monospace; font-size:11px; align-self:center; }
  .heat td, .heat th { text-align:center; padding:4px 0; width:11%; }
  .heat td { border:2px solid #fff; font-weight:700; }
  .heat td.s { background:var(--laranja); color:#fff; } .heat td.n { background:var(--creme2); color:#B5A893; }
  .heat td.foco { outline:3px solid var(--preto); outline-offset:-3px; }
  .heat th.foco { background:var(--laranja); }
  svg text { font-family:Barlow, sans-serif; }
  .barra { height:10px; background:var(--laranja); border-radius:6px; display:inline-block; vertical-align:middle; margin-left:6px; }
  .resumo { display:flex; gap:10px; flex-wrap:wrap; margin:8px 0 10px; }
  .toast { position:fixed; bottom:18px; left:50%; transform:translateX(-50%); background:var(--preto); color:#fff; padding:10px 18px; border-radius:12px; font-weight:600; opacity:0; transition:opacity .25s; pointer-events:none; z-index:10; max-width:90vw; }
  .toast.on { opacity:1; } .toast.erro { background:var(--ruim); }
  footer { color:var(--cinza); font-size:12px; text-align:center; padding:0 20px 24px; }
</style>
</head>
<body>
<header>
  <div>
    <h1>Rota Vital</h1>
    <div class="sub">Gestão e distribuição de hemocomponentes · localhost</div>
  </div>
  <div class="stats" id="stats"></div>
</header>

<main>
  <section class="card c4">
    <h2><span class="n">1</span>Novo pedido</h2>
    <p class="dica">Entra na <b>fila FIFO</b> (<code>FilaPedidos.enqueue</code>). Nada é alocado até você processar a fila.</p>
    <form id="formPedido" class="form">
      <div class="full"><label for="hospital">Hospital</label><select id="hospital" name="hospital"></select></div>
      <div><label for="componente">Componente</label>
        <select id="componente" name="componente">
          <option value="CH">CH · hemácias</option><option value="PFC">PFC · plasma</option><option value="PLQ">PLQ · plaquetas</option>
        </select></div>
      <div><label for="receptor">Tipo do paciente</label><select id="receptor" name="receptor"></select></div>
      <div><label for="quantidade">Quantidade</label><input id="quantidade" name="quantidade" type="number" min="1" max="10" value="1"></div>
      <div><label for="janela">Janela (min)</label><input id="janela" name="janela" type="number" min="1" max="600" value="60"></div>
    </form>
    <div class="botoes">
      <button class="lar" id="btnPedido">Adicionar à fila</button>
      <button class="sec" id="btnExemplo">Carregar 6 pedidos de exemplo</button>
    </div>
  </section>

  <section class="card c4">
    <h2><span class="n">2</span>Fila FIFO</h2>
    <p class="dica">Ordem de chegada. Processar faz <code>dequeue</code> até esvaziar e chama <code>ServicoAlocacao.alocar</code> para cada pedido.</p>
    <div id="fila"></div>
    <div class="botoes">
      <button class="lar" id="btnProcessar">Processar fila</button>
    </div>
  </section>

  <section class="card c4">
    <h2><span class="n">3</span>Operação do dia</h2>
    <p class="dica">Data simulada fixa, igual ao C++, para os resultados baterem com o console.</p>
    <div class="botoes" style="margin-top:0">
      <button id="btnVencidas">Varredura de vencidas</button>
      <button class="sec" id="btnReset">Reiniciar cenário</button>
    </div>
    <h2 style="margin-top:18px;font-size:18px">Consultar bolsa por id</h2>
    <p class="dica">Índice com <b>sondagem linear</b> (<code>IndicePorId</code>).</p>
    <div style="display:flex;gap:8px"><input id="idBolsa" type="number" placeholder="ex.: 1008"><button id="btnBolsa">Buscar</button></div>
    <div id="resBolsa" class="dica" style="margin-top:8px"></div>
  </section>

  <section class="card c7">
    <h2><span class="n">4</span>Alocações</h2>
    <p class="dica">Matriz ABO/Rh filtra os tipos → hash leva à lista → FEFO entrega a que vence primeiro (tipo idêntico antes do compatível).</p>
    <div id="alocacoes"></div>
  </section>

  <section class="card c5">
    <h2><span class="n">5</span>Rota de entrega</h2>
    <p class="dica">Floyd-Warshall (caminhos mínimos) + vizinho mais próximo. Confere janela e cadeia fria.</p>
    <svg id="mapa" viewBox="20 20 480 320" style="width:100%;max-height:300px"></svg>
    <div id="rota"></div>
  </section>

  <section class="card c7">
    <h2><span class="n">6</span>Estoque · hash por componente + tipo</h2>
    <p class="dica">Tabela de <span id="tamHash">7</span> posições com <b>encadeamento</b>; cada entrada guarda uma lista FEFO (ids na ordem de saída).</p>
    <div id="estoque"></div>
  </section>

  <section class="card c5">
    <h2><span class="n">7</span>Compatibilidade ABO/Rh</h2>
    <p class="dica">Matriz <code>[doador][receptor]</code>. A coluna destacada mostra quem pode doar para o tipo escolhido.</p>
    <div class="form" style="margin-bottom:10px">
      <div><label for="cComp">Componente</label><select id="cComp"><option value="CH">CH · hemácias</option><option value="PFC">PFC · plasma</option><option value="PLQ">PLQ · plaquetas</option></select></div>
      <div><label for="cRec">Receptor</label><select id="cRec"></select></div>
    </div>
    <div id="compat"></div>
  </section>

  <section class="card c12">
    <h2><span class="n">8</span>Relatório · ordenação e busca</h2>
    <p class="dica">As 6 ordenações do semestre sobre o estoque atual (critério FEFO) e a busca binária "o que vence nos próximos 7 dias".</p>
    <div id="relatorio"></div>
  </section>
</main>
<footer>Dados 100% sintéticos · matriz ABO/Rh didática, não substitui protocolo clínico · sem integração com a Hemorrede</footer>
<div class="toast" id="toast"></div>

<script>
const POS = {0:[70,165],1:[215,60],2:[250,180],3:[215,300],4:[420,105],5:[440,275]};
const CURTO = ['Hemocentro','Alfa','Beta','Gama','Delta','Epsilon'];
let estado = null;

const $ = id => document.getElementById(id);
const esc = s => String(s).replace(/[&<>"]/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c]));

async function api(caminho, metodo = 'GET', dados = null) {
  const op = { method: metodo };
  if (dados) { op.body = new URLSearchParams(dados); }
  const r = await fetch(caminho, op);
  const j = await r.json();
  if (!r.ok) throw new Error(j.erro || 'erro');
  return j;
}

function aviso(msg, erro = false) {
  const t = $('toast'); t.textContent = msg; t.className = 'toast on' + (erro ? ' erro' : '');
  clearTimeout(aviso.t); aviso.t = setTimeout(() => t.className = 'toast', 2600);
}

async function acao(fn) { try { await fn(); } catch (e) { aviso(e.message, true); } }

async function atualizar() {
  estado = await api('/api/estado');
  if (!$('hospital').options.length) preencherSelects();
  $('stats').innerHTML = [
    [estado.hoje, 'hoje (simulado)'], [estado.total, 'bolsas em estoque'], [estado.descartadas, 'descartadas'],
    [estado.colisoes, 'colisões no hash'], [estado.fila.length, 'na fila'], [estado.alocacoes.length, 'pedidos alocados']
  ].map(([v, l]) => `<div class="stat"><b>${esc(v)}</b><span>${l}</span></div>`).join('');
  $('tamHash').textContent = estado.tamanhoHash;
  desenharFila(); desenharAlocacoes(); desenharEstoque();
  await Promise.all([desenharRota(), desenharRelatorio(), desenharCompat()]);
}

function preencherSelects() {
  $('hospital').innerHTML = estado.hospitais.map(h => `<option value="${h.indice}">${esc(h.nome)}</option>`).join('');
  const tipos = estado.tipos.map(t => `<option value="${t.nome}">${esc(t.rotulo)}</option>`).join('');
  $('receptor').innerHTML = tipos; $('cRec').innerHTML = tipos;
  $('receptor').value = 'A_POS'; $('cRec').value = 'A_POS';
}

function desenharFila() {
  const f = estado.fila;
  $('fila').innerHTML = f.length ? `<table><tr><th>#</th><th>Hospital</th><th>Pedido</th><th>Janela</th></tr>` +
    f.map((p, i) => `<tr><td>${i === 0 ? '<span class="tag preto">início</span> ' : ''}${p.id}</td><td>${esc(p.hospitalNome)}</td>
      <td><span class="tag">${p.componente}</span> ${esc(p.receptor)} × ${p.quantidade}</td><td>${p.janela} min</td></tr>`).join('') + '</table>'
    : '<div class="vazio">Fila vazia. Adicione pedidos ou carregue o exemplo.</div>';
}

function desenharAlocacoes() {
  const a = estado.alocacoes;
  if (!a.length) { $('alocacoes').innerHTML = '<div class="vazio">Nenhum pedido alocado ainda.</div>'; return; }
  $('alocacoes').innerHTML = '<table><tr><th>Pedido</th><th>Hospital</th><th>Solicitado</th><th>Bolsas entregues (FEFO)</th><th>Status</th></tr>' +
    a.map(x => {
      const st = x.status === 'ATENDIDO' ? 'ok' : (x.status === 'PARCIAL' ? 'parcial' : 'ruim');
      const bolsas = x.bolsas.map(b => `<span class="tag ${b.tipoCompativel ? 'lar' : ''}" title="${b.tipoCompativel ? 'tipo compatível' : 'tipo idêntico'}">${b.bolsa.id} · ${esc(b.bolsa.tipo)} · ${b.bolsa.data}</span>`).join('');
      return `<tr><td>#${x.pedido.id}</td><td>${esc(x.pedido.hospitalNome)}</td><td>${x.pedido.componente} ${esc(x.pedido.receptor)} × ${x.pedido.quantidade}</td>
        <td>${bolsas}</td><td><span class="tag ${st}">${x.status === 'PARCIAL' ? `PARCIAL ${x.obtidas}/${x.pedido.quantidade}` : x.status}</span></td></tr>`;
    }).join('') + '</table><p class="dica" style="margin-top:8px"><span class="tag lar">laranja</span> = tipo compatível usado porque faltou o idêntico.</p>';
}

function desenharEstoque() {
  $('estoque').innerHTML = estado.slots.map(s => {
    const ents = s.entradas.map(e => `<span class="seta">→</span><span class="ent"><b>${e.componente} ${esc(e.tipo)}</b> · ${e.bolsas.length}
      <small>${e.bolsas.map(b => b.id).join(' → ') || 'vazia'}</small></span>`).join('');
    return `<div class="slot"><span class="slot-i">${s.indice}</span>${ents}<span class="seta">→</span><span class="null">NULL</span></div>`;
  }).join('');
}

async function desenharRota() {
  const r = await api('/api/rota');
  let svg = '';
  const usados = new Set();
  const arestas = [];
  r.paradas.forEach(p => arestas.push(p.vertices));
  if (r.paradas.length) arestas.push(r.verticesRetorno);
  arestas.forEach(vs => { for (let i = 1; i < vs.length; i++) { const k = [vs[i-1], vs[i]].sort().join('-'); if (!usados.has(k)) { usados.add(k);
    const [x1, y1] = POS[vs[i-1]], [x2, y2] = POS[vs[i]];
    svg += `<line x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}" stroke="#F26A1B" stroke-width="4"/>`; } } });
  const ordem = {};
  r.paradas.forEach(p => { (ordem[p.local] = ordem[p.local] || []).push(p.ordem); });
  for (const [n, [x, y]] of Object.entries(POS)) {
    const base = n === '0';
    svg += `<circle cx="${x}" cy="${y}" r="26" fill="${base ? '#1A1A1A' : '#fff'}" stroke="#1A1A1A" stroke-width="2"/>
      <text x="${x}" y="${y + 4}" text-anchor="middle" font-size="${base ? 8.5 : 11}" font-weight="700" fill="${base ? '#fff' : '#1A1A1A'}">${CURTO[n]}</text>`;
    if (ordem[n]) svg += `<circle cx="${x + 20}" cy="${y - 20}" r="11" fill="#F26A1B"/><text x="${x + 20}" y="${y - 16}" text-anchor="middle" font-size="11" font-weight="800" fill="#fff">${ordem[n].join(',')}</text>`;
  }
  $('mapa').innerHTML = svg;
  if (!r.paradas.length) { $('rota').innerHTML = '<div class="vazio">Sem pedidos alocados para entregar.</div>'; return; }
  $('rota').innerHTML = `<div class="resumo"><span class="tag preto">circuito ${r.total} min</span>
      <span class="tag ${r.alertas ? 'ruim' : 'ok'}">${r.alertas} alerta(s)</span><span class="tag">cadeia fria ≤ ${r.limiteCadeiaFria} min</span></div>
    <table><tr><th>#</th><th>Destino</th><th>Chegada</th><th>Janela</th><th>Situação</th></tr>` +
    r.paradas.map(p => `<tr title="${esc(p.trajeto)}"><td>${p.ordem}</td><td>${esc(p.nome)} <span class="dica">(pedido #${p.pedidoId})</span></td>
      <td>${p.chegada} min</td><td>${p.janela} min</td>
      <td><span class="tag ${p.noPrazo ? 'ok' : 'ruim'}">${p.noPrazo ? 'no prazo' : 'atraso'}</span>
          <span class="tag ${p.cadeiaFriaOk ? 'ok' : 'ruim'}">${p.cadeiaFriaOk ? 'frio OK' : 'risco'}</span></td></tr>`).join('') +
    `</table><p class="dica" style="margin-top:8px">Volta: ${esc(r.retorno)}. ${r.alertas ? 'O vizinho mais próximo é guloso: escolhe o destino mais perto sem olhar o prazo.' : ''}</p>`;
}

async function desenharCompat() {
  const comp = $('cComp').value, rec = $('cRec').value;
  const r = await api('/api/compat?componente=' + comp);
  const tipos = estado.tipos;
  const col = tipos.findIndex(t => t.nome === rec);
  let h = '<table class="heat"><tr><th style="background:none;color:var(--cinza);font-size:11px">doador ↓</th>' +
    tipos.map((t, j) => `<th class="${j === col ? 'foco' : ''}">${esc(t.rotulo)}</th>`).join('') + '</tr>';
  r.matriz.forEach((linha, i) => {
    h += `<tr><th>${esc(tipos[i].rotulo)}</th>` + linha.map((v, j) => `<td class="${v ? 's' : 'n'} ${j === col ? 'foco' : ''}">${v ? '✓' : '·'}</td>`).join('') + '</tr>';
  });
  const doadores = r.matriz.map((l, i) => l[col] ? tipos[i].rotulo : null).filter(Boolean);
  $('compat').innerHTML = h + `</table><p class="dica" style="margin-top:8px">${esc(tipos[col].rotulo)} pode receber ${comp} de: <b>${doadores.map(esc).join(', ')}</b>${comp === 'PFC' ? ' · no plasma a regra se inverte (AB é doador universal).' : ''}</p>`;
}

async function desenharRelatorio() {
  const r = await api('/api/relatorio');
  const max = Math.max(1, ...r.comparacoes.map(c => c.comparacoes));
  $('relatorio').innerHTML = `<div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(320px,1fr));gap:18px">
    <table><tr><th>Algoritmo (n = ${r.n})</th><th>Comparações</th></tr>` +
    r.comparacoes.map(c => `<tr><td>${c.algoritmo} ${c.ordenado ? '' : '<span class="tag ruim">erro</span>'}</td>
      <td>${c.comparacoes}<span class="barra" style="width:${c.comparacoes / max * 140}px"></span></td></tr>`).join('') + `</table>
    <div><table><tr><th>Vencem até ${r.limite}</th><th>Validade</th></tr>` +
    (r.vencendo.length ? r.vencendo.map(b => `<tr><td>${b.id} · ${b.componente} ${esc(b.tipo)}</td><td>${b.data}</td></tr>`).join('') : '<tr><td colspan="2" class="vazio">Nenhuma.</td></tr>') +
    `</table><p class="dica" style="margin-top:8px">Busca binária iterativa = ${r.iterativa} · recursiva = ${r.recursiva}</p></div></div>`;
}

$('btnPedido').onclick = () => acao(async () => {
  const p = await api('/api/pedido', 'POST', new FormData($('formPedido')));
  aviso(`Pedido #${p.pedido.id} entrou na fila (${p.naFila} aguardando)`); await atualizar();
});
$('btnExemplo').onclick = () => acao(async () => { await api('/api/exemplo', 'POST', {}); aviso('6 pedidos do cenário entraram na fila'); await atualizar(); });
$('btnProcessar').onclick = () => acao(async () => {
  if (!estado.fila.length) { aviso('A fila está vazia', true); return; }
  const r = await api('/api/processar', 'POST', {});
  aviso(`${r.resultados.length} pedido(s) processado(s)` + (r.descartadasAgora ? ` · ${r.descartadasAgora} vencida(s) descartada(s)` : '')); await atualizar();
});
$('btnVencidas').onclick = () => acao(async () => { const r = await api('/api/vencidas', 'POST', {}); aviso(`${r.descartadas} bolsa(s) vencida(s) descartada(s)`); await atualizar(); });
$('btnReset').onclick = () => acao(async () => { await api('/api/reset', 'POST', {}); aviso('Cenário reiniciado'); await atualizar(); });
$('btnBolsa').onclick = () => acao(async () => {
  const r = await api('/api/bolsa?id=' + encodeURIComponent($('idBolsa').value));
  $('resBolsa').innerHTML = r.emEstoque ? `Bolsa <b>${r.id}</b> em estoque · posição <b>${r.slot}</b> do índice (id % 101 = ${r.id % 101})` : `Bolsa <b>${r.id}</b> não está em estoque (alocada, descartada ou inexistente).`;
});
$('cComp').onchange = () => acao(desenharCompat);
$('cRec').onchange = () => acao(desenharCompat);

acao(atualizar);
</script>
</body>
</html>
""";
}
