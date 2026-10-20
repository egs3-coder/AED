"""Gera docs/Rota_Vital_U2.pdf a partir do código-fonte real (C++ e Java).

Os trechos de código são extraídos dos arquivos .cpp/.java no momento da
geração, então o PDF nunca fica dessincronizado do código entregue.

Uso: python3 docs/build_pdf.py   (requer pygments, playwright, pypdf)
"""
import base64
import pathlib
import re
import sys
import textwrap

from pygments import highlight
from pygments.formatters import HtmlFormatter
from pygments.lexers import CppLexer, JavaLexer

RAIZ = pathlib.Path(__file__).resolve().parent.parent
CPP = (RAIZ / "cpp/rota_vital.cpp").read_text(encoding="utf-8")
JAVA_DIR = RAIZ / "java/src/rotavital"
SAIDA = (RAIZ / "cpp/saida_cpp.txt").read_text(encoding="utf-8")
FONTES = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else pathlib.Path("/tmp/claude-0/fonts")
OUT = RAIZ / "docs/Rota_Vital_U2.pdf"


# ----------------------------------------------------------------- extração
def extrair(texto, inicio):
    """Do primeiro trecho que começa com `inicio` até as chaves fecharem."""
    linhas = texto.splitlines()
    for i, l in enumerate(linhas):
        if l.strip().startswith(inicio):
            prof, abriu, out = 0, False, []
            for l2 in linhas[i:]:
                out.append(l2)
                sem_coment = re.sub(r"//.*", "", l2)
                prof += sem_coment.count("{") - sem_coment.count("}")
                if "{" in sem_coment:
                    abriu = True
                if abriu and prof <= 0:
                    break
            return textwrap.dedent("\n".join(out))
    raise KeyError(inicio)


def cpp(inicio):
    return extrair(CPP, inicio)


def java(arquivo, inicio):
    return extrair((JAVA_DIR / arquivo).read_text(encoding="utf-8"), inicio)


def hl(codigo, lang):
    lexer = CppLexer() if lang == "cpp" else JavaLexer()
    return highlight(codigo, lexer, HtmlFormatter(nowrap=True))


def lado_a_lado(c, j, titulo_c="C/C++", titulo_j="Java"):
    return f"""
<div class="dual">
  <div class="code"><div class="code-h cpp">{titulo_c}</div><pre>{hl(c, 'cpp')}</pre></div>
  <div class="code"><div class="code-h java">{titulo_j}</div><pre>{hl(j, 'java')}</pre></div>
</div>"""


def notas(itens):
    """Tabela de tradução comentada: (C++, Java, por quê)."""
    linhas = "".join(
        f"<tr><td><code>{a}</code></td><td><code>{b}</code></td><td>{c}</td></tr>" for a, b, c in itens)
    return f"""<table class="trad"><thead><tr><th>Em C/C++</th><th>Em Java</th>
<th>Raciocínio da tradução</th></tr></thead><tbody>{linhas}</tbody></table>"""


def complexidade(itens):
    linhas = "".join(f"<tr><td>{a}</td><td class='big'>{b}</td><td>{c}</td></tr>" for a, b, c in itens)
    return f"""<table class="cx"><thead><tr><th>Operação</th><th>Custo</th>
<th>Justificativa pela implementação</th></tr></thead><tbody>{linhas}</tbody></table>"""


def secao(num, titulo, aula, corpo):
    return f"""
<section>
  <div class="sec-top"><span class="sec-num">{num:02d}</span><span class="sec-aula">{aula}</span></div>
  <h1>{titulo}</h1>
  {corpo}
</section>"""


def b64(path):
    return base64.b64encode(pathlib.Path(path).read_bytes()).decode()


# ------------------------------------------------------------ dados reais
def bloco_saida(titulo_inicio, titulo_fim=None):
    ini = SAIDA.index(titulo_inicio)
    fim = SAIDA.index(titulo_fim, ini) if titulo_fim else len(SAIDA)
    return SAIDA[ini:fim].rstrip()


TESTES = re.findall(r"\[(OK|FALHOU)\]\s+(.*)", SAIDA)
N_OK = sum(1 for s, _ in TESTES if s == "OK")
COMPARACOES = re.findall(r"  (\w+ Sort)\s+comparacoes:\s+(\d+)", SAIDA)

# Floyd-Warshall em Python só para desenhar a matriz de distâncias no PDF
INF = 10 ** 6
EST = [[0, 15, 25, 40, INF, INF], [15, 0, 12, INF, 30, INF], [25, 12, 0, 18, 22, INF],
       [40, INF, 18, 0, INF, 20], [INF, 30, 22, INF, 0, 16], [INF, INF, INF, 20, 16, 0]]
DIST = [r[:] for r in EST]
for k in range(6):
    for i in range(6):
        for j in range(6):
            DIST[i][j] = min(DIST[i][j], DIST[i][k] + DIST[k][j])
LOCAIS = ["Hemocentro", "Alfa", "Beta", "Gama", "Delta", "Epsilon"]
TIPOS = ["O-", "O+", "A-", "A+", "B-", "B+", "AB-", "AB+"]


def matriz_compat(nome_cpp):
    m = cpp(nome_cpp)
    linhas = re.findall(r"\{\s*([01](?:\s*,\s*[01]){7})\s*\}", m)
    head = "".join(f"<th>{t}</th>" for t in TIPOS)
    corpo = ""
    for t, l in zip(TIPOS, linhas):
        vals = [v.strip() for v in l.split(",")]
        corpo += f"<tr><th>{t}</th>" + "".join(
            f"<td class='{'s' if v == '1' else 'n'}'>{'✓' if v == '1' else '·'}</td>" for v in vals) + "</tr>"
    return f"<table class='heat'><thead><tr><th class='corner'>doador ↓ / receptor →</th>{head}</tr></thead><tbody>{corpo}</tbody></table>"


def diagrama_hash():
    bloco = bloco_saida("=== 1. ESTOQUE INICIAL", "=== 2.")
    slots = re.split(r"\n  slot (\d+):", "\n" + bloco.split("\n", 2)[2])
    html = ""
    for i in range(1, len(slots), 2):
        idx, conteudo = slots[i], slots[i + 1]
        entradas = re.findall(r"\[(\w+ [A-Z]+[+-])\] qtd=(\d+):(.*)", conteudo)
        cadeia = ""
        for chave, qtd, bolsas in entradas:
            ids = " → ".join(b.split("(")[0] for b in bolsas.split())
            cadeia += f"<span class='arrow'>→</span><span class='ent'><b>{chave}</b><small>FEFO: {ids}</small></span>"
        html += f"<div class='slot'><span class='slot-i'>{idx}</span>{cadeia}<span class='arrow'>→</span><span class='null'>NULL</span></div>"
    return html


def grafo_svg():
    pos = {0: (70, 165), 1: (215, 60), 2: (250, 180), 3: (215, 300), 4: (420, 105), 5: (440, 275)}
    arestas = [(0, 1, 15), (0, 2, 25), (0, 3, 40), (1, 2, 12), (1, 4, 30), (2, 3, 18),
               (2, 4, 22), (3, 5, 20), (4, 5, 16)]
    rota = {(0, 1), (1, 2), (2, 3), (3, 5), (5, 4), (4, 1), (1, 0)}
    s = ""
    for a, b, w in arestas:
        (x1, y1), (x2, y2) = pos[a], pos[b]
        on = (a, b) in rota or (b, a) in rota
        s += (f"<line x1='{x1}' y1='{y1}' x2='{x2}' y2='{y2}' stroke='{'#F26A1B' if on else '#C9BFB0'}' "
              f"stroke-width='{4 if on else 2}'/>")
        mx, my = (x1 + x2) / 2, (y1 + y2) / 2
        s += f"<rect x='{mx-15}' y='{my-10}' width='30' height='20' rx='10' fill='#FDF6EC' stroke='#1A1A1A' stroke-width='1'/>"
        s += f"<text x='{mx}' y='{my+4}' text-anchor='middle' font-size='11' font-weight='700'>{w}</text>"
    for n, (x, y) in pos.items():
        cor = "#1A1A1A" if n == 0 else "#FFFFFF"
        txt = "#FFFFFF" if n == 0 else "#1A1A1A"
        s += f"<circle cx='{x}' cy='{y}' r='26' fill='{cor}' stroke='#1A1A1A' stroke-width='2'/>"
        s += f"<text x='{x}' y='{y-2}' text-anchor='middle' font-size='13' font-weight='800' fill='{txt}'>{n}</text>"
        s += f"<text x='{x}' y='{y+11}' text-anchor='middle' font-size='8.5' fill='{txt}'>{LOCAIS[n]}</text>"
    return f"<svg viewBox='20 20 480 320' class='grafo' font-family='Barlow'>{s}</svg>"


def matriz_dist():
    head = "".join(f"<th>{i}</th>" for i in range(6))
    corpo = ""
    for i in range(6):
        corpo += f"<tr><th>{i} {LOCAIS[i]}</th>"
        for j in range(6):
            v = DIST[i][j]
            mudou = EST[i][j] != v
            corpo += f"<td class='{'via' if mudou else ''}'>{v}</td>"
        corpo += "</tr>"
    return f"<table class='dist'><thead><tr><th></th>{head}</tr></thead><tbody>{corpo}</tbody></table>"


def lista_testes():
    grupos = re.split(r"\n-- (.*?) --", "\n" + bloco_saida("-- Compatibilidade de hemacias", "Resultado:"))
    html = ""
    for i in range(1, len(grupos), 2):
        itens = re.findall(r"\[(OK|FALHOU)\]\s+(.*)", grupos[i + 1])
        lis = "".join(f"<li><span class='ok'>{'✔' if s == 'OK' else '✘'}</span>{d}</li>" for s, d in itens)
        html += f"<div class='tgrupo'><h4>{grupos[i]}</h4><ul>{lis}</ul></div>"
    return html


# ------------------------------------------------------------ conteúdo
EQUIPE = [
    ("ewerton", "Ewerton Guilherme da Silva", "Product Owner"),
    ("pablo", "Pablo Arthur Eustáquio de Lima", "Backend Developer"),
    ("joao", "João Ricardo Alves de Brito", "Backend Developer"),
    ("lucas", "Lucas Aprigio dos Santos", "Backend Developer"),
    ("saulo", "Saulo Eduardo Almeida dos Santos", "Backend Developer"),
    ("thiago", "Thiago Cardozo da Conceição", "Frontend / UI Designer"),
    ("eloi", "Eloi de Lima Sousa", "Frontend / UI Designer"),
]


def pagina_equipe():
    cards = "".join(
        f"<div class='membro'><img src='data:image/png;base64,{b64(RAIZ / f'docs/equipe/{k}.png')}'/>"
        f"<b>{n}</b><span>{p}</span></div>" for k, n, p in EQUIPE)
    sumario = [
        "O problema e a visão do sistema", "Método da tradução comentada C/C++ → Java",
        "Compatibilidade ABO/Rh — matriz", "Priorização FEFO — lista com inserção ordenada",
        "Índice de estoque — hash com encadeamento", "Índice por id — hash com sondagem linear",
        "Fila de pedidos — FIFO encadeada", "Alocação integrada — o coração do sistema",
        "Grafo e caminhos mínimos — Floyd-Warshall", "Roteirização — vizinho mais próximo",
        "Ordenação — os seis métodos do semestre", "Busca — sequencial, binária iterativa e recursiva",
        "Quadro-resumo de complexidade", "Dicionário geral de tradução C/C++ → Java",
        "Testes e evidências de execução", "Atendimento aos critérios e à regra geral",
        "Como executar · limites e cuidados"]
    itens = "".join(f"<li><span>{i+2:02d}</span>{t}</li>" for i, t in enumerate(sumario))
    return f"""
<section>
  <div class="sec-top"><span class="sec-num">01</span><span class="sec-aula">Quem fez</span></div>
  <h1>Equipe</h1>
  <div class="equipe">{cards}</div>
  <h2>Sumário</h2>
  <ol class="sumario">{itens}</ol>
</section>"""


def capa_html(fontes_css):
    return f"""<!doctype html><html><head><meta charset="utf-8"><style>{fontes_css}
@page {{ size: 297mm 210mm; margin: 0; }}
html,body {{ margin:0; }}
.capa {{ width:297mm; height:210mm; background:#FDF6EC; position:relative; overflow:hidden; font-family:Barlow; color:#1A1A1A; }}
.marca {{ position:absolute; left:26mm; top:22mm; font-family:'Barlow Condensed'; font-weight:700; letter-spacing:.32em; font-size:10pt; }}
.marca b {{ display:block; letter-spacing:.06em; font-size:17pt; font-weight:600; margin-top:-1mm; }}
.titulo {{ position:absolute; left:26mm; top:58mm; width:150mm; }}
.titulo h1 {{ font-family:'Barlow Condensed'; font-style:italic; font-weight:800; color:#F26A1B; font-size:54pt; line-height:.98; margin:0; }}
.titulo h2 {{ font-family:'Barlow Condensed'; font-style:italic; font-weight:700; color:#F26A1B; font-size:22pt; margin:5mm 0 0; }}
.titulo p {{ font-size:11.5pt; margin:4mm 0 0; max-width:125mm; line-height:1.4; }}
.autoria {{ position:absolute; left:26mm; bottom:22mm; font-size:12pt; line-height:1.45; }}
.autoria b {{ font-weight:600; }}
.blob {{ position:absolute; right:0; top:0; width:112mm; height:210mm; }}
</style></head><body><div class="capa">
  <div class="marca">CESAR<b>school</b></div>
  <div class="titulo">
    <h1>Rota Vital</h1>
    <h2>Projeto Integrador · Unidade 2</h2>
    <p>Algoritmos que dão inteligência à gestão e distribuição de hemocomponentes:
       compatibilidade ABO/Rh, FEFO, índice por hash e roteirização — implementados em
       C/C++ e traduzidos, comentados, para Java.</p>
  </div>
  <div class="autoria"><b>Equipe Rota Vital</b> · Programação Orientada a Objetos e Estruturas de Dados<br>
     Recife, outubro de 2026 · dados 100% sintéticos</div>
  <svg class="blob" viewBox="0 0 112 210">
    <path d="M30 0 H112 V210 H30 C10 190 18 170 26 150 C34 128 10 118 12 96 C14 74 34 64 30 42 C27 24 14 14 30 0 Z" fill="#1A1A1A"/>
    <g font-family="Barlow Condensed" font-weight="800" font-style="italic" fill="#FDF6EC" opacity=".08" font-size="40">
      <text x="34" y="44">O-</text><text x="62" y="84">AB+</text><text x="36" y="124">A+</text><text x="70" y="164">B-</text></g>
    <g fill="none" stroke="#FDF6EC" stroke-width=".5" opacity=".55">
      <circle cx="62" cy="58" r="3" fill="#F26A1B" stroke="none"/><circle cx="88" cy="96" r="3" fill="#FDF6EC" stroke="none"/>
      <circle cx="58" cy="128" r="3" fill="#FDF6EC" stroke="none"/><circle cx="92" cy="150" r="3" fill="#FDF6EC" stroke="none"/>
      <path d="M62 58 L88 96 L58 128 L92 150"/></g>
    <path d="M112 96 A30 30 0 0 0 82 126 H112 Z" fill="#3A3A3A"/>
    <path d="M112 128 A34 34 0 0 0 78 162 H112 Z" fill="#F26A1B"/>
  </svg>
  <svg style="position:absolute;left:120mm;bottom:0;width:56mm;height:56mm" viewBox="0 0 56 56">
    <path d="M1 1 A54 54 0 0 1 55 55" fill="none" stroke="#F2B233" stroke-width=".6"/></svg>
</div></body></html>"""


def fontes_css():
    def ff(fam, arq, peso, estilo):
        return (f"@font-face{{font-family:'{fam}';src:url(data:font/woff2;base64,{b64(arq)}) format('woff2');"
                f"font-weight:{peso};font-style:{estilo};}}")
    bc = FONTES / "fontsource-barlow-condensed-5.0.13/package/files"
    ba = FONTES / "fontsource-barlow-5.0.13/package/files"
    jm = FONTES / "fontsource-jetbrains-mono-5.0.20/package/files"
    css = ""
    for p in (600, 700, 800):
        for e in ("normal", "italic"):
            css += ff("Barlow Condensed", bc / f"barlow-condensed-latin-{p}-{e}.woff2", p, e)
    for p in (400, 500, 600, 700):
        css += ff("Barlow", ba / f"barlow-latin-{p}-normal.woff2", p, "normal")
    css += ff("Barlow", ba / "barlow-latin-400-italic.woff2", 400, "italic")
    for p in (400, 700):
        css += ff("JetBrains Mono", jm / f"jetbrains-mono-latin-{p}-normal.woff2", p, "normal")
    return css


CSS = """
@page { size: 297mm 210mm; margin: 15mm 18mm 16mm 18mm; }
:root { --creme:#FDF6EC; --creme2:#F6EADA; --laranja:#F26A1B; --preto:#1A1A1A; --ouro:#F2B233; --cinza:#6B645B; }
html { background: var(--creme); }
body { font-family: Barlow; color: var(--preto); font-size: 9.6pt; line-height: 1.42; margin: 0; background: var(--creme);
       -webkit-print-color-adjust: exact; print-color-adjust: exact; }
.deco { position: fixed; top: -15mm; right: -18mm; width: 34mm; height: 34mm; }
section { page-break-before: always; position: relative; }
section:first-of-type { page-break-before: auto; }
.sec-top { display:flex; align-items:center; gap:4mm; margin-bottom:1mm; }
.sec-num { font-family:'Barlow Condensed'; font-weight:800; font-style:italic; font-size:13pt; color:var(--creme);
           background:var(--preto); border-radius: 0 10mm 10mm 0; padding:.5mm 5mm .5mm 3mm; }
.sec-aula { font-family:'Barlow Condensed'; font-weight:600; letter-spacing:.14em; text-transform:uppercase; font-size:8.5pt; color:var(--cinza); }
h1 { font-family:'Barlow Condensed'; font-style:italic; font-weight:800; color:var(--laranja); font-size:27pt; line-height:1; margin:1mm 0 3mm; }
h2 { font-family:'Barlow Condensed'; font-style:italic; font-weight:700; font-size:14pt; margin:4mm 0 1.5mm; color:var(--preto); }
h2::before { content:''; display:inline-block; width:3.2mm; height:3.2mm; border-radius:50%; background:var(--laranja); margin-right:2mm; }
h4 { font-family:'Barlow Condensed'; font-weight:700; font-size:10.5pt; margin:0 0 1mm; }
p { margin: 0 0 2mm; }
.lead { font-size:10.4pt; max-width:235mm; }
code { font-family:'JetBrains Mono'; font-size:7.6pt; background:var(--creme2); padding:0 .8mm; border-radius:1mm; }
.cols { display:grid; grid-template-columns: 1fr 1fr; gap:7mm; }
.cols3 { display:grid; grid-template-columns: 1fr 1fr 1fr; gap:5mm; }
.card { background:#FFFFFF; border:1px solid #E8DCCB; border-radius:3mm; padding:3.5mm 4mm; break-inside: avoid; }
.card.escuro { background:var(--preto); color:var(--creme); border:none; }
.card.escuro h4 { color:var(--laranja); }
.card.laranja { background:var(--laranja); color:#fff; border:none; }
.pill { display:inline-block; font-family:'Barlow Condensed'; font-weight:700; font-size:8.5pt; padding:.3mm 2.5mm;
        border-radius:5mm; background:var(--preto); color:var(--creme); margin-right:1.5mm; }
.pill.l { background:var(--laranja); color:#fff; }
/* código lado a lado */
.dual { display:grid; grid-template-columns: 1fr 1fr; gap:4mm; margin: 2mm 0 3mm; }
.code { background:#1D1B19; border-radius:3mm; overflow:hidden; break-inside: avoid; }
.code-h { font-family:'Barlow Condensed'; font-weight:700; font-size:9pt; letter-spacing:.08em; padding:1.3mm 4mm; color:#fff; }
.code-h.cpp { background:#3A3632; } .code-h.java { background:var(--laranja); }
pre, code { font-variant-ligatures: none; font-feature-settings: "calt" 0, "liga" 0; }
pre { margin:0; padding:2.5mm 4mm 3mm; font-family:'JetBrains Mono'; font-size:7pt; line-height:1.42; color:#EDE6DA;
      white-space:pre-wrap; word-break:break-word; }
pre .c1, pre .cm, pre .c, pre .cs, pre .cpf { color:#9C9286; font-style:normal; }
pre .k, pre .kd, pre .kn, pre .kr, pre .kc { color:#FF9A57; font-weight:700; }
pre .kt { color:#F2B233; }
pre .nf, pre .fm { color:#8FD1C7; }
pre .nc, pre .nn { color:#F2B233; }
pre .mi, pre .mf, pre .mh { color:#C9A6F2; }
pre .s, pre .s2, pre .sc, pre .se, pre .sa { color:#B9D98A; }
pre .cp, pre .cpf { color:#FF9A57; }
pre .na { color:#8FD1C7; }
/* tabelas */
table { border-collapse:collapse; width:100%; }
tr { break-inside:avoid; }
.trad th, .cx th { font-family:'Barlow Condensed'; font-weight:700; text-align:left; font-size:9pt; background:var(--preto);
                   color:var(--creme); padding:1.4mm 2.5mm; }
.trad td, .cx td { border-bottom:1px solid #E8DCCB; padding:.9mm 2.5mm; vertical-align:top; background:#fff; font-size:8.6pt; }
.trad td:nth-child(1), .trad td:nth-child(2) { width:24%; }
.trad td code { background:none; padding:0; font-size:7.2pt; }
.cx td:first-child { width:24%; font-weight:600; }
.cx td.big { font-family:'Barlow Condensed'; font-weight:800; font-size:11.5pt; color:var(--laranja); width:13%; white-space:nowrap; }
.heat { width:auto; font-size:8pt; }
.heat th { font-family:'Barlow Condensed'; font-weight:700; padding:.8mm 1.8mm; }
.heat th.corner { font-size:7pt; color:var(--cinza); font-weight:600; }
.heat td { width:7.2mm; height:5.6mm; text-align:center; border:1.5px solid var(--creme); font-weight:700; }
.heat td.s { background:var(--laranja); color:#fff; } .heat td.n { background:#EFE3D2; color:#B5A893; }
.dist { width:auto; font-size:8.4pt; }
.dist th { font-family:'Barlow Condensed'; text-align:left; padding:.8mm 2mm; font-weight:700; }
.dist td { text-align:center; padding:.8mm 2.4mm; background:#fff; border:1px solid #E8DCCB; }
.dist td.via { background:var(--laranja); color:#fff; font-weight:700; }
/* diagramas */
.slot { display:flex; align-items:center; flex-wrap:nowrap; margin:1.1mm 0; }
.slot-i { width:8mm; height:8mm; background:var(--preto); color:var(--creme); display:flex; align-items:center; justify-content:center;
          font-family:'Barlow Condensed'; font-weight:800; border-radius:1.5mm; flex:none; }
.ent { display:inline-flex; flex-direction:column; background:#fff; border:1.5px solid var(--preto); border-radius:1.5mm; padding:.6mm 2mm; }
.ent b { font-family:'Barlow Condensed'; font-size:9pt; } .ent small { font-family:'JetBrains Mono'; font-size:6pt; color:var(--cinza); }
.arrow { color:var(--laranja); font-weight:800; margin:0 1.4mm; }
.null { font-family:'JetBrains Mono'; font-size:6.5pt; color:var(--cinza); }
.nos { display:flex; align-items:center; flex-wrap:wrap; gap:0; margin:2mm 0; }
.no { display:inline-flex; border:1.5px solid var(--preto); border-radius:1.5mm; overflow:hidden; background:#fff; font-size:8pt; }
.no span { padding:1mm 2mm; } .no span:last-child { background:var(--creme2); border-left:1.5px solid var(--preto); font-family:'JetBrains Mono'; font-size:6.5pt; }
.no.novo { border-color:var(--laranja); } .no.novo span:first-child { background:var(--laranja); color:#fff; }
.cab { font-family:'Barlow Condensed'; font-weight:700; margin-right:2mm; }
.vetor { display:flex; margin:2mm 0; } .vetor div { width:6.8mm; border:1px solid var(--preto); margin-left:-1px; text-align:center; font-size:6.2pt; background:#fff; padding:.6mm 0; }
.vetor div small { display:block; color:var(--cinza); font-size:6pt; } .vetor div.oc { background:var(--preto); color:var(--creme); }
.vetor div.oc small { color:#bbb; } .vetor div.hit { background:var(--laranja); color:#fff; }
.grafo { width:100%; max-height:62mm; }
.fluxo { display:flex; align-items:stretch; gap:0; margin:2mm 0; }
.fluxo .passo { flex:1; background:#fff; border:1.5px solid var(--preto); border-radius:3mm; padding:2.5mm 3mm; position:relative; }
.fluxo .passo b { font-family:'Barlow Condensed'; font-size:11pt; display:block; color:var(--laranja); }
.fluxo .passo span { font-size:8.2pt; }
.fluxo .passo.dark { background:var(--preto); color:var(--creme); }
.fluxo .seta { display:flex; align-items:center; color:var(--laranja); font-weight:800; font-size:14pt; padding:0 1.2mm; }
.saida { background:#1D1B19; color:#EDE6DA; font-family:'JetBrains Mono'; font-size:6.6pt; padding:3mm 4mm; border-radius:3mm; white-space:pre; line-height:1.4; }
.equipe { display:grid; grid-template-columns: repeat(7, 1fr); gap:3.5mm; margin:3mm 0 4mm; }
.membro { background:#fff; border-radius:3mm; overflow:hidden; text-align:center; border:1px solid #E8DCCB; padding-bottom:2mm; }
.membro img { width:100%; aspect-ratio:1; object-fit:cover; display:block; border-bottom:2.5mm solid var(--laranja); }
.membro b { display:block; font-family:'Barlow Condensed'; font-size:10.5pt; line-height:1.1; margin:2mm 1.5mm 1mm; }
.membro span { font-size:7.6pt; color:var(--cinza); }
.sumario { columns:2; column-gap:12mm; list-style:none; padding:0; margin:0; }
.sumario li { padding:1mm 0; border-bottom:1px dashed #D9CBB6; break-inside:avoid; font-size:9.6pt; }
.sumario li span { font-family:'Barlow Condensed'; font-weight:800; font-style:italic; color:var(--laranja); margin-right:3mm; }
.tgrupo { break-inside:avoid; margin-bottom:2mm; }
.tgrupo ul { list-style:none; margin:0; padding:0; } .tgrupo li { font-size:7.5pt; padding:.15mm 0; }
.ok { color:var(--laranja); font-weight:800; margin-right:1.5mm; }
.check li { list-style:none; margin:1.2mm 0; } .check { padding:0; }
.check li::before { content:'✔'; color:var(--laranja); font-weight:800; margin-right:2mm; }
.stat { font-family:'Barlow Condensed'; font-weight:800; font-style:italic; font-size:24pt; color:var(--laranja); line-height:1; }
.stat small { display:block; font-family:Barlow; font-style:normal; font-weight:500; font-size:8.5pt; color:inherit; }
.barra { height:3mm; background:var(--laranja); border-radius:2mm; display:inline-block; vertical-align:middle; }
.aviso { border-left:2mm solid var(--ouro); background:#fff; padding:2.5mm 4mm; border-radius:0 3mm 3mm 0; font-size:8.8pt; }
"""

DECO = """<svg class="deco" viewBox="0 0 34 34"><path d="M34 0 V24 A24 24 0 0 1 10 0 Z" fill="#F26A1B"/>
<path d="M34 0 V12 A12 12 0 0 1 22 0 Z" fill="#1A1A1A"/></svg>"""


def corpo():
    S = []
    S.append(pagina_equipe())

    # 02 problema
    S.append(secao(2, "O problema e a visão do sistema", "Ficha do projeto", f"""
<p class="lead">A rede de sangue precisa garantir <b>o componente certo</b> (compatível e dentro da validade),
<b>no lugar certo</b>, <b>no tempo certo</b> e <b>na temperatura certa</b>. Falhas geram desabastecimento,
descarte por vencimento e risco ao paciente. O fluxo inspirado na Hemorrede/SUS é: coleta e doação →
hemocentro de processamento → estoque → hospitais.</p>
<h2>Do pedido à entrega: o fluxo que implementamos</h2>
<div class="fluxo">
 <div class="passo dark"><b>1 · Fila FIFO</b><span>Requisições hospitalares entram na ordem de chegada.</span></div><div class="seta">→</div>
 <div class="passo"><b>2 · Matriz ABO/Rh</b><span>8 consultas O(1) dizem quais tipos doadores servem.</span></div><div class="seta">→</div>
 <div class="passo"><b>3 · Hash</b><span>(componente, tipo) leva direto à lista daquela combinação.</span></div><div class="seta">→</div>
 <div class="passo"><b>4 · FEFO</b><span>Descarta vencidas e retira a bolsa que vence primeiro.</span></div><div class="seta">→</div>
 <div class="passo"><b>5 · Floyd-Warshall</b><span>Menor tempo entre todos os pares de pontos.</span></div><div class="seta">→</div>
 <div class="passo dark"><b>6 · Vizinho + próximo</b><span>Ordem de visita, janela de tempo e cadeia fria.</span></div>
</div>
<div class="cols3">
 <div class="card"><h4>🩸 Componente certo</h4>Matriz de compatibilidade (hemácias/plaquetas e plasma com regra invertida)
 + descarte automático de vencidas antes de qualquer alocação.</div>
 <div class="card"><h4>⏱ Tempo certo</h4>FEFO faz a bolsa mais próxima do vencimento sair primeiro; a rota verifica a
 janela de cada hospital e emite alerta quando ela é violada.</div>
 <div class="card"><h4>❄ Temperatura certa</h4>Limite simulado de 120 min de caixa térmica; cada parada é marcada como
 "cadeia fria OK" ou "RISCO".</div>
</div>
<h2>Onde cada algoritmo da disciplina aparece</h2>
<table class="cx"><thead><tr><th>Assunto (aula)</th><th>Estrutura no Rota Vital</th><th>Para que serve no problema</th></tr></thead><tbody>
<tr><td>Recursão (02)</td><td>Busca binária recursiva, Quick e Merge Sort</td><td>Relatório "o que vence nos próximos 7 dias" e ordenação de relatórios.</td></tr>
<tr><td>Listas, pilhas e filas (03)</td><td>Lista FEFO encadeada · fila FIFO de pedidos</td><td>Estoque sempre ordenado por validade · atendimento justo por ordem de chegada.</td></tr>
<tr><td>Ponteiros (03.1)</td><td><code>NoBolsa*</code>, <code>EntradaEstoque*</code>, parâmetros de saída</td><td>Encadeamento manual em C++; vira referência em Java.</td></tr>
<tr><td>Árvores / grafos (04–05)</td><td>Grafo de 6 vértices + Floyd-Warshall</td><td>Caminho mínimo entre hemocentro e hospitais.</td></tr>
<tr><td>Busca e hash (06)</td><td>Hash com encadeamento + hash com sondagem linear · busca sequencial e binária</td><td>Achar o estoque de um tipo e uma bolsa por id em O(1) médio.</td></tr>
<tr><td>Ordenação (07–08)</td><td>Bubble, Selection, Insertion, Shell, Quick, Merge</td><td>Relatórios FEFO; comparação de custo real (contador de comparações).</td></tr>
</tbody></table>"""))

    # 03 método
    S.append(secao(3, "Método da tradução comentada", "Como ler este documento e o código", f"""
<p class="lead">A disciplina avalia o <b>raciocínio</b>, não só o programa rodando. Por isso escrevemos primeiro a
versão C/C++ (structs, ponteiros, <code>new/delete</code>, funções que recebem a estrutura por ponteiro) e só depois
traduzimos para Java orientado a objetos — mantendo <b>o mesmo algoritmo, os mesmos nomes de variáveis e a mesma
saída</b>. A tradução é verificável: os dois programas imprimem exatamente o mesmo texto.</p>
<div class="cols3">
 <div class="card escuro"><h4>① Bloco de cabeçalho</h4>Todo arquivo Java abre com um bloco <b>"C++ → Java"</b> que mostra a
 declaração original em C++ e explica o que mudou na versão orientada a objetos.</div>
 <div class="card escuro"><h4>② Comentário na linha</h4>Toda linha cuja forma mudou leva <code style="background:#333;color:#fff">// C++: ...</code>
 com a linha original, por exemplo <code style="background:#333;color:#fff">atual = atual.prox; // C++: atual = atual-&gt;prox;</code></div>
 <div class="card escuro"><h4>③ Mesma lógica, mesmos nomes</h4><code style="background:#333;color:#fff">fefoInserir</code> →
 <code style="background:#333;color:#fff">ListaFefo.inserir</code>, <code style="background:#333;color:#fff">hashBuscar</code> →
 <code style="background:#333;color:#fff">HashEstoque.buscar</code>. Os laços, condições e o desempate são os mesmos.</div>
</div>
<h2>As quatro regras de tradução que seguimos</h2>
<div class="cols">
<div class="card"><h4>1. A estrutura com suas funções vira uma classe</h4>Em C++, <code>struct HashEstoque</code> e as funções
<code>hashBuscar(HashEstoque* h, ...)</code> ficam separadas. Em Java, os dados e as operações ficam juntos na
classe; o primeiro parâmetro <code>h</code> some porque passa a ser o <code>this</code> implícito.</div>
<div class="card"><h4>2. Ponteiro vira referência; <code>delete</code> some</h4><code>NoBolsa* prox</code> vira
<code>No prox</code>, <code>-&gt;</code> vira <code>.</code> e <code>NULL</code> vira <code>null</code>. Não existe
<code>delete</code>: quando nenhum objeto aponta mais para um nó, o coletor de lixo o libera. Por isso
<code>estoqueLiberar()</code> não tem equivalente em Java.</div>
<div class="card"><h4>3. Parâmetro de saída vira retorno</h4><code>bool dequeue(FilaPedidos* f, Pedido* saida)</code> escreve
no ponteiro. Java não tem ponteiro de saída, então <code>dequeue()</code> devolve o <code>Pedido</code> ou
<code>null</code>. A cabeça de lista retornada por <code>fefoInserir</code> vira atributo do objeto.</div>
<div class="card"><h4>4. Nada de biblioteca pronta no lugar do algoritmo</h4>Não usamos <code>HashMap</code>,
<code>PriorityQueue</code>, <code>LinkedList</code> nem <code>Arrays.sort</code>: a função hash, o tratamento de colisão,
a inserção ordenada e as ordenações estão escritos à mão nas duas linguagens. <code>List</code> só aparece como
recipiente do resultado.</div>
</div>
<div class="aviso" style="margin-top:3mm"><b>Diferença semântica mais importante:</b> em C++, <code>Bolsa b = outra;</code>
<b>copia</b> a struct. Em Java a variável guarda uma <b>referência</b> para o mesmo objeto. Como a mesma bolsa fica ao mesmo
tempo na lista FEFO, no índice por id e nos vetores de relatório, tornamos <code>Bolsa</code> <b>imutável</b> (campos
<code>final</code>). Assim, compartilhar a referência tem o mesmo efeito que copiar em C++, sem efeito colateral.</div>"""))

    # 04 ABO
    S.append(secao(4, "Compatibilidade ABO/Rh — matriz", "Estruturas de dados · arrays bidimensionais", f"""
<div class="cols" style="grid-template-columns: 1.05fr 1fr; align-items:start">
<div>
<p>A compatibilidade é uma <b>relação fixa</b> entre 8 tipos doadores e 8 tipos receptores, então a guardamos em uma matriz
8×8 indexada por <code>[doador][receptor]</code>. Responder "posso usar esta bolsa?" vira dois acessos a um vetor, sem
nenhum <code>if</code> encadeado de regras.</p>
<p><b>Hemácias e plaquetas:</b> o doador não pode levar um antígeno (A, B ou D/Rh) que o receptor não tenha; O− é o doador
universal. <b>Plasma:</b> a lógica se inverte porque o plasma leva anticorpos; AB é o doador universal e o Rh não conta.</p>
<div style="display:flex; gap:5mm; margin-top:2mm">
<div><h4>Hemácias / plaquetas</h4>{matriz_compat('const int COMPAT_HEMACIAS')}</div>
<div><h4>Plasma (PFC)</h4>{matriz_compat('const int COMPAT_PLASMA')}</div></div>
<p style="margin-top:2mm;font-size:8pt;color:#6B645B">As matrizes acima foram lidas diretamente do arquivo <code>rota_vital.cpp</code> ao gerar este PDF.
Matriz didática: não substitui protocolo imuno-hematológico.</p>
</div>
<div>
{notas([
 ('enum Tipo { O_NEG, ... }', 'enum TipoSanguineo { O_NEG("O-"), ... }', 'Em C++ o enum já é um inteiro e serve de índice. Em Java o enum é um objeto: usamos <code>ordinal()</code> para obter o mesmo índice e o rótulo "O-" passa a morar dentro do enum (no C++ era um vetor <code>NOME_TIPO[]</code> paralelo).'),
 ('const int COMPAT[8][8]', 'private static final int[][]', '<code>const</code> global vira <code>static final</code> privado: a tabela pertence à classe e não pode ser alterada de fora.'),
 ('bool compativel(...)', 'public static boolean compativel(...)', 'Não há estado por objeto, então o método é estático (classe utilitária com construtor privado).'),
])}
</div></div>
{lado_a_lado(cpp('bool compativel('), java('MatrizCompatibilidade.java', 'public static boolean compativel('))}
{complexidade([('Consultar compatibilidade', 'O(1)', 'Um <code>if</code> escolhe a matriz e um acesso <code>[doador][receptor]</code> responde. Não há laço. As matrizes têm tamanho fixo (64 inteiros), portanto o espaço também é O(1).'),
               ('Listar doadores de um receptor', 'O(T) = O(1)', 'Percorre as 8 linhas da coluna do receptor. T = 8 é constante do domínio — não cresce com o estoque.')])}
<p style="margin-top:2mm"><span class="pill l">Testes</span>14 casos: 4 de compatibilidade e 4 de incompatibilidade para hemácias
(incluindo O+ → O− barrado pelo Rh), 2 de contagem (receptor O− só tem 1 doador; AB+ tem 8) e 4 de plasma (AB+ → O− permitido; O+ → A+ barrado).</p>"""))

    # 05 FEFO
    S.append(secao(5, "Priorização FEFO — lista com inserção ordenada", "Aula 03 · listas encadeadas e ponteiros", f"""
<p class="lead"><b>FEFO</b> (First Expired, First Out): a bolsa que vence primeiro sai primeiro. Escolhemos manter a lista
<b>sempre ordenada</b>, inserindo cada bolsa já na posição certa. Pagamos um pouco mais na entrada (recebimento,
que acontece uma vez) para que a saída (pedido urgente) e o descarte sejam imediatos.</p>
<div class="cols" style="align-items:start">
<div>
<h4>Inserindo a bolsa 1007 (24/10) em [CH A+]</h4>
<div class="nos"><span class="cab">head →</span>
<span class="no"><span>1021</span><span>21/10</span></span><span class="arrow">→</span>
<span class="no novo"><span>1007</span><span>24/10</span></span><span class="arrow">→</span>
<span class="no"><span>1006</span><span>01/11</span></span><span class="arrow">→</span><span class="null">NULL</span></div>
<p style="font-size:8.6pt">O laço avança enquanto a nova bolsa <b>não</b> vence antes do próximo nó; ao parar, religa dois
ponteiros: <code>novo-&gt;prox = atual-&gt;prox</code> e <code>atual-&gt;prox = novo</code>. Empate de validade é
desempatado pelo id, o que deixa a ordem determinística nas duas linguagens.</p>
<div class="card"><h4>Por que as vencidas saem em O(v)</h4>Como a lista está ordenada por validade, as bolsas vencidas são sempre
um <b>prefixo</b> da lista. O descarte só remove pela cabeça e para na primeira bolsa válida — nunca percorre a lista
toda. No cenário, a varredura diária descartou 1005, 1019 e 1012.</div>
</div><div>
{notas([
 ('struct NoBolsa { ...; NoBolsa* prox; }', 'private static class No { ...; No prox; }', 'O nó vira classe interna privada: ninguém fora da lista manipula ponteiros.'),
 ('NoBolsa* fefoInserir(NoBolsa* head, Bolsa b)', 'void inserir(Bolsa b)', 'Em C++ a função devolve a nova cabeça porque ela pode mudar. Em Java a cabeça é atributo, então o método é <code>void</code>.'),
 ('delete temp;', '(sem equivalente)', 'O coletor de lixo libera o nó quando <code>cabeca</code> deixa de apontar para ele.'),
 ('bool vemAntes(const Bolsa&amp;, const Bolsa&amp;)', 'a.venceAntesDe(b)', 'Função livre vira método de instância. <code>const&amp;</code> evitava cópia; em Java já é referência.'),
])}
</div></div>
{lado_a_lado(cpp('NoBolsa* fefoInserir('), java('ListaFefo.java', 'public void inserir('))}
{lado_a_lado(cpp('NoBolsa* fefoRemoverPrimeiro('), java('ListaFefo.java', 'public Bolsa removerPrimeiro('))}
{complexidade([('Inserção ordenada', 'O(m)', 'm = bolsas daquela chave. Pior caso: a nova bolsa tem a maior validade e o <code>while</code> visita os m nós. Melhor caso O(1): ela vira a nova cabeça (primeiro <code>if</code>).'),
               ('Retirar a que vence primeiro', 'O(1)', 'Só lê e troca a cabeça. A ordenação já foi paga na inserção.'),
               ('Descartar vencidas', 'O(v)', 'v = vencidas. Remove pela cabeça enquanto <code>validade &lt; hoje</code>; o laço para na primeira válida.')])}"""))

    # 06 hash encadeamento
    S.append(secao(6, "Índice de estoque — hash com encadeamento", "Aula 06 · busca por espalhamento", f"""
<p class="lead">Todo pedido pergunta "quais bolsas de <b>CH A+</b> eu tenho?". A chave é <code>componente × 8 + tipo</code>
(24 chaves possíveis) e a função hash é <code>chave % 7</code>. Cada entrada guarda a <b>lista FEFO</b> daquela
combinação — o hash diz <i>onde</i> está e a lista já está na <i>ordem</i> certa.</p>
<div class="cols" style="grid-template-columns: 1fr 1.15fr; align-items:start">
<div>
<h4>Estado real da tabela após carregar as 22 bolsas</h4>
{diagrama_hash()}
<p style="font-size:8pt;color:#6B645B;margin-top:1.5mm">Gerado a partir da saída do programa. Escolhemos de propósito
um primo pequeno (7) para que as <b>8 colisões</b> fiquem visíveis; cada colisão é resolvida inserindo a nova entrada no
início da cadeia.</p>
</div>
<div>
{notas([
 ('struct HashEstoque { EntradaEstoque* slots[7]; }', 'private final Entrada[] slots = new Entrada[TAM_HASH];', 'Java já inicializa o array com <code>null</code>, por isso <code>hashInicializar()</code> não precisa de laço; ele vira o construtor implícito.'),
 ('hashBuscar(HashEstoque* h, int chave)', 'buscar(int chave)', 'O ponteiro <code>h</code> passa a ser o <code>this</code>. O <code>while (atual != NULL)</code> é idêntico com <code>null</code>.'),
 ('NoBolsa* fefo; int quantidade;', 'final ListaFefo fefo = new ListaFefo();', 'Em C++ a entrada guarda a cabeça e o contador soltos; em Java guarda um objeto <code>ListaFefo</code> que sabe o próprio tamanho.'),
 ('(não usamos HashMap)', '(não usamos HashMap)', 'Usar <code>java.util.HashMap</code> esconderia exatamente o que a aula 06 pede: função hash e tratamento de colisão.'),
])}</div></div>
{lado_a_lado(cpp('EntradaEstoque* hashBuscar('), java('HashEstoque.java', 'public Entrada buscar('))}
{lado_a_lado(cpp('EntradaEstoque* hashObterOuCriar('), java('HashEstoque.java', 'public Entrada obterOuCriar('))}
{complexidade([('Buscar a lista de um tipo', 'O(1 + α)', 'Calcula <code>chave % 7</code> e percorre só a cadeia do slot. α = chaves/slots ≤ 24/7 ≈ 3,4, constante do domínio → O(1) na prática. Pior caso teórico O(chaves) se tudo colidisse no mesmo slot.'),
               ('Receber bolsa', 'O(m)', 'O(1) no hash + O(m) da inserção ordenada na lista FEFO da chave.')])}"""))

    # 07 sondagem
    ocup = list(range(92, 101)) + list(range(0, 13))
    cells = ""
    for s in list(range(90, 101)) + [None] + list(range(10, 15)):
        if s is None:
            cells += "<div style='border:none;background:none'>…<small>&nbsp;</small></div>"
            continue
        idv = 909 + s if s >= 92 else (1010 + s if s <= 12 else None)
        cls = "oc" if s in ocup else ""
        if s == 13:
            cls = "hit"
        cells += f"<div class='{cls}'>{idv if idv else ('1102' if s == 13 else '—')}<small>{s}</small></div>"
    S.append(secao(7, "Índice por id — hash com sondagem linear", "Aula 06 · endereçamento aberto", f"""
<p class="lead">Para "onde está a bolsa 1008?" usamos uma segunda tabela, agora com <b>endereçamento aberto</b>: um vetor de
101 posições (primo), <code>h(id) = id % 101</code>, e em caso de colisão testamos a próxima posição
<code>(base + i) % 101</code>. Ter as duas técnicas no projeto é o "depende!" da aula 06 aplicado.</p>
<div class="cols" style="grid-template-columns: 1fr 1.15fr; align-items:start">
<div>
<div class="card"><h4>Por que duas técnicas diferentes?</h4>
<b>Encadeamento</b> no estoque: poucas chaves (24), cada uma com uma lista de tamanho variável e remoções a toda hora.<br>
<b>Sondagem linear</b> no índice por id: uma bolsa por chave, fator de carga baixo (22/101 ≈ 22%, abaixo dos 70%
recomendados) e memória contígua.</div>
<h4 style="margin-top:3mm">Remoção com marcação <code>REMOVIDO</code></h4>
<p style="font-size:8.6pt">Se esvaziássemos a posição (<code>VAZIO</code>), uma busca posterior pararia ali e não acharia
elementos que foram empurrados para frente por colisão. Por isso a remoção marca <code>REMOVIDO</code>: a busca
"pula" a marca, e a inserção pode reaproveitá-la.</p>
</div><div>
<h4>Análise crítica: agrupamento primário</h4>
<p style="font-size:8.6pt">Os ids sintéticos são sequenciais (1001…1022) e ocupam um bloco contínuo: posições 92–100 e 0–12.
No cenário não há colisão, mas uma bolsa de id <b>1102</b> (1102 % 101 = 92) testaria 92, 93, …, 100, 0, …, 12 e só
pararia na posição <b>13</b>: 23 sondagens.</p>
<div class="vetor">{cells}</div>
<p style="font-size:8pt;color:#6B645B">Preto = ocupado no cenário · laranja = onde o id 1102 cairia. Melhorias
possíveis: hash duplo ou chave embaralhada antes do módulo.</p>
{notas([
 ('void indiceInicializar(...) { for ... = VAZIO; }', 'Arrays.fill(chaves, VAZIO);', 'Mesmo efeito, só que dentro do construtor.'),
 ('x->chave[pos] = REMOVIDO;', 'chaves[pos] = REMOVIDO; bolsas[pos] = null;', 'Em Java também soltamos a referência à bolsa para o coletor de lixo poder liberá-la; em C++ a struct ficava no vetor sem custo extra.'),
])}</div></div>
{lado_a_lado(cpp('int indiceBuscar('), java('IndicePorId.java', 'public int buscar('))}
{lado_a_lado(cpp('bool indiceRemover('), java('IndicePorId.java', 'public boolean remover('))}
{complexidade([('Buscar / inserir / remover', 'O(1) médio', 'Com fator de carga de 22%, quase sempre 1 acesso. Pior caso O(TAM) quando há agrupamento (exemplo do id 1102: 23 sondagens).')])}"""))

    # 08 fila
    S.append(secao(8, "Fila de pedidos — FIFO encadeada", "Aula 03 · filas", f"""
<p class="lead">Requisições são atendidas na ordem de chegada. Mantemos ponteiros para <b>início</b> e <b>fim</b>: inserir no fim
e remover do início custam O(1), sem deslocar elementos como aconteceria em um vetor.</p>
{lado_a_lado(cpp('void enqueue(') + "\n\n" + cpp('bool dequeue('), java('FilaPedidos.java', 'public void enqueue(') + "\n\n" + java('FilaPedidos.java', 'public Pedido dequeue('))}
<div class="cols">
{notas([
 ('bool dequeue(FilaPedidos* f, Pedido* saida)', 'public Pedido dequeue()', 'C++ devolve o sucesso em <code>bool</code> e o dado pelo ponteiro <code>saida</code>. Java não tem ponteiro de saída: devolve o próprio <code>Pedido</code>, ou <code>null</code> quando a fila está vazia.'),
 ('while (dequeue(&amp;fila, &amp;p))', 'while ((p = fila.dequeue()) != null)', 'O laço do <code>main</code> muda de forma, mas faz a mesma coisa.'),
 ('Pedido p = {1, 3, A_POS, CH, 3, 90};', 'new Pedido(1, 3, A_POS, CH, 3, 90)', 'A inicialização por chaves da struct vira construtor com os parâmetros na mesma ordem.'),
])}
<div>{complexidade([('enqueue', 'O(1)', 'Liga o novo nó em <code>fim-&gt;prox</code> e move <code>fim</code>. Nenhum laço.'),
                    ('dequeue', 'O(1)', 'Lê <code>inicio</code>, avança para <code>inicio-&gt;prox</code> e zera <code>fim</code> se a fila esvaziou.')])}
<div class="card" style="margin-top:3mm"><h4>No cenário</h4>6 pedidos entram na ordem #1 (Gama) … #6 (Alfa) e são alocados exatamente nessa ordem.
A ordem de <b>entrega</b>, porém, quem decide é a roteirização (seção 11).</div></div>
</div>"""))

    # 09 alocação
    S.append(secao(9, "Alocação integrada — o coração do sistema", "Matriz + hash + FEFO trabalhando juntos", f"""
<p class="lead">Dado um pedido, o sistema precisa entregar <b>a bolsa compatível que vence primeiro</b>. É aqui que as estruturas
se encontram: a <b>matriz</b> filtra os tipos, o <b>hash</b> leva a cada lista, o <b>FEFO</b> oferece a candidata de cada
tipo na cabeça, e comparamos só essas cabeças.</p>
<div class="cols" style="align-items:start">
<div>
<div class="card escuro"><h4>Passo a passo do pedido #4 · Hospital Beta · CH O+ ×3</h4>
1. A matriz diz que O+ recebe hemácias de <b>O−</b> e <b>O+</b>.<br>
2. O hash devolve as listas [CH O−] = 1022 e [CH O+] = 1004 → 1003 (as O− 1002 e 1001 já tinham ido para o pedido #2).<br>
3. Existe tipo idêntico, então saem <b>1004</b> (20/10) e <b>1003</b> (06/11) por FEFO.<br>
4. [CH O+] esvaziou: compara as cabeças compatíveis restantes e escolhe <b>1022 (O−)</b>, marcada como
"tipo compatível".</div>
<h4 style="margin-top:3mm">Regra de prioridade e por quê</h4>
<p style="font-size:8.6pt">Primeiro o <b>tipo idêntico</b>, depois o compatível que vence antes. Assim preservamos o O−
(doador universal, escasso) para quem só pode receber O−, e ainda evitamos perder bolsas por vencimento.</p>
</div><div>
{notas([
 ('int alocar(Estoque* e, Pedido p, int hoje, Alocacao* out)', 'Alocacao alocar(Pedido p, int hoje)', 'O estoque é injetado no construtor de <code>ServicoAlocacao</code> (o mesmo padrão de um <code>@Service</code> no Spring Boot) e o resultado volta como objeto, não por ponteiro.'),
 ('Bolsa bolsas[10]; int qtd;', 'List&lt;Bolsa&gt; bolsas', 'Lista só como recipiente do resultado; <code>qtd</code> vira <code>size()</code>.'),
 ('for (int t = 0; t &lt; N_TIPOS; t++)', 'for (TipoSanguineo t : values())', 'O for-each sobre o enum percorre os mesmos 8 tipos na mesma ordem.'),
])}
</div></div>
{lado_a_lado(cpp('int alocar('), java('ServicoAlocacao.java', 'public Alocacao alocar('))}
{complexidade([('Alocar um pedido', 'O(v + q)', 'Laço fixo de T = 8 tipos com matriz O(1) e hash O(1); descarte das v vencidas; para cada uma das q bolsas compara no máximo 8 cabeças e remove em O(1). Como T é constante, O(T + v + q·T) = O(v + q). <b>Não depende do tamanho n do estoque</b> — esse é o ganho de combinar hash e FEFO.')])}"""))

    # 10 grafo
    S.append(secao(10, "Grafo e caminhos mínimos — Floyd-Warshall", "Aulas 04–05 · generalizações de árvores: grafos", f"""
<div class="cols" style="grid-template-columns: 1fr 1.1fr; align-items:start">
<div>
<p>A rede tem <b>6 vértices</b> (hemocentro + 5 hospitais) e 9 estradas com tempo em minutos — escopo controlado, como pede a
ficha. Como a rota consulta a distância entre <b>vários pares</b> de pontos, calculamos todos de uma vez com
Floyd-Warshall e guardamos <code>prox[i][j]</code> para reconstruir o trajeto.</p>
{grafo_svg()}
<p style="font-size:8pt;color:#6B645B">Em laranja, as estradas usadas pela rota calculada (seção 11).</p>
<h4>Matriz de distâncias mínimas (min)</h4>
{matriz_dist()}
<p style="font-size:8pt;color:#6B645B;margin-top:1mm">Células laranja: o caminho mínimo passa por um vértice intermediário, por
exemplo Hemocentro → Epsilon = 60 min via Gama; não existe estrada direta.</p>
</div>
<div>
{notas([
 ('struct Grafo { int dist[V][V]; int prox[V][V]; }', 'private final int[][] dist, prox', 'Matrizes viram atributos da classe.'),
 ('void floydWarshall(Grafo* g)', 'construtor Grafo()', 'Calcular no construtor garante que nenhum <code>Grafo</code> exista sem as distâncias prontas.'),
 ('void imprimirCaminho(...) { printf(...) }', 'String caminho(int u, int v)', 'Em Java devolvemos o texto (<code>StringBuilder</code>) em vez de imprimir, para reaproveitar numa API REST.'),
 ('const int INF = 1000000;', 'INF = 1_000_000;', 'Valor alto, mas sem estourar o <code>int</code> quando somamos dois INF.'),
])}
{complexidade([('Floyd-Warshall', 'O(V³)', 'Três <code>for</code> aninhados de V. Com V = 6: 216 relaxamentos, uma única vez. Espaço O(V²) para <code>dist</code> e <code>prox</code>.'),
               ('Distância entre dois pontos', 'O(1)', 'Leitura de <code>dist[u][v]</code> já calculado.'),
               ('Reconstruir trajeto', 'O(V)', 'Segue <code>prox[u][v]</code>; um caminho simples tem no máximo V vértices.')])}
</div></div>
{lado_a_lado(cpp('void floydWarshall('), java('Grafo.java', 'public Grafo() {'))}"""))

    # 11 rota
    rota = bloco_saida("=== 4. ROTA", "=== 5.")
    S.append(secao(11, "Roteirização — vizinho mais próximo", "Heurística gulosa · janelas de tempo · cadeia fria", f"""
<div class="cols" style="grid-template-columns: 1fr 1fr; align-items:start">
<div>
<p>Com as distâncias prontas, decidimos a <b>ordem de visita</b>: saindo do hemocentro, vamos sempre à parada
<b>ainda não visitada mais próxima</b> da posição atual. A estrutura é a mesma do <b>Selection Sort</b> (procura o mínimo
no restante e troca para a posição atual), o que deixa a complexidade fácil de justificar.</p>
{notas([
 ('std::swap(paradas[pos], paradas[melhor]);', 'Parada tmp = paradas[pos]; ...', 'Java não tem <code>std::swap</code>; trocamos com uma variável temporária. Em Java se trocam <b>referências</b>; em C++ se copiam as structs.'),
 ('Parada paradas[], int k', 'Parada[] paradas', 'O array Java conhece o próprio tamanho (<code>length</code>).'),
])}
</div>
<div>
<h4>Resultado real no cenário</h4>
<div class="saida" style="font-size:5.9pt">{rota}</div>
<div class="card laranja" style="margin-top:3mm"><h4 style="color:#fff">Limitação que o próprio sistema mostra</h4>
O Hospital Delta chega aos <b>81 min</b> com janela de <b>75</b>. O algoritmo guloso escolheu Epsilon antes de Delta
porque estava mais perto naquele momento, sem olhar o prazo. O sistema <b>detecta e sinaliza</b> o atraso. Uma
evolução seria priorizar por janela (ex.: visitar primeiro quem tem menor folga).</div>
{complexidade([('Vizinho mais próximo', 'O(k²)', 'Para cada uma das k posições, varre as restantes: k + (k−1) + … + 1 = k(k+1)/2 consultas O(1) a <code>dist</code>. Com k = 6 pedidos: 21 comparações.'),
               ('Total da roteirização', 'O(V³ + k²)', 'Floyd uma vez + ordem de visita. Não garante a rota ótima (o problema exato é o do caixeiro-viajante).')])}
</div></div>"""))

    # 12 ordenação
    linhas_cmp = ""
    maxc = max(int(c) for _, c in COMPARACOES)
    cx_ord = {"Bubble Sort": ("O(n²)", "O(n)", "Para com a flag <code>trocou</code> se uma passada não troca."),
              "Selection Sort": ("O(n²)", "O(n²)", "O laço interno sempre vai até o fim: n(n−1)/2 = 28 com n = 8."),
              "Insertion Sort": ("O(n²)", "O(n)", "O <code>while</code> para cedo: o vetor vem quase ordenado, pois é exportado de listas FEFO."),
              "Shell Sort": ("O(n²)", "O(n log n)", "Insertion Sort por gaps n/2, n/4…; no caso médio fica abaixo de n²."),
              "Quick Sort": ("O(n²)", "O(n log n)", "Lomuto com pivô = último; vetor já ordenado gera partições 0 / n−1."),
              "Merge Sort": ("O(n log n)", "O(n log n)", "log n níveis × O(n) de intercalação; usa vetor auxiliar O(n). Estável.")}
    for nome, c in COMPARACOES:
        p, m, j = cx_ord[nome]
        linhas_cmp += (f"<tr><td>{nome}</td><td class='big'>{p}</td><td class='big' style='color:#1A1A1A'>{m}</td>"
                       f"<td><b>{c}</b> <span class='barra' style='width:{int(c)/maxc*28}mm'></span></td><td>{j}</td></tr>")
    S.append(secao(12, "Ordenação — os seis métodos do semestre", "Aulas 07–08 · Bubble, Selection, Insertion, Shell, Quick, Merge", f"""
<p class="lead">Os relatórios precisam do estoque restante em ordem FEFO. Implementamos os seis métodos sobre o mesmo vetor de bolsas,
com o <b>mesmo critério</b> (<code>vemAntes</code>: validade, depois id) e um <b>contador de comparações</b> para medir o custo real.</p>
<table class="cx"><thead><tr><th>Método</th><th>Pior caso</th><th>Melhor / médio</th><th>Comparações reais (n = 8)</th><th>Justificativa pelo código</th></tr></thead>
<tbody>{linhas_cmp}</tbody></table>
<p style="margin-top:2mm;font-size:8.6pt">Leitura dos números: o vetor exportado do estoque já vem <b>em blocos ordenados</b> (cada lista FEFO), então
Insertion (16) e Merge (15) se saem melhor e o Selection (28) não tira proveito disso, exatamente como a teoria prevê.</p>
{lado_a_lado(cpp('void intercalar(') + "\n\n" + cpp('void mergeSort('), java('Ordenacao.java', 'private void intercalar(') + "\n\n" + java('Ordenacao.java', 'private void mergeSort('), 'C/C++ · Merge Sort', 'Java · Merge Sort')}
{notas([
 ('Bolsa* aux = new Bolsa[n]; ... delete[] aux;', 'Bolsa[] aux = new Bolsa[...];', 'Mesmo vetor auxiliar O(n); em Java não há <code>delete[]</code>.'),
 ('for (k...) arr[esq+k] = aux[k];', 'System.arraycopy(aux, 0, arr, esq, aux.length);', 'Cópia equivalente, feita pela biblioteca padrão (não substitui o algoritmo, só copia).'),
 ('std::swap(arr[i], arr[j])', 'swap(arr, i, j)', 'Java não passa elemento de array por referência; o método recebe o array e os dois índices.'),
 ('long long g_comparacoes (global)', 'private long comparacoes (atributo)', 'Elimina estado global: cada objeto <code>Ordenacao</code> conta as próprias comparações.'),
 ('void quickSort(Bolsa arr[], int low, int high)', 'quickSort(Bolsa[] arr) + private quickSort(arr, low, high)', 'Sobrecarga: o método público esconde os índices; o recursivo é privado.'),
])}"""))

    # 13 busca
    S.append(secao(13, "Busca — sequencial, binária iterativa e recursiva", "Aulas 02 e 06 · recursão e busca", f"""
<p class="lead">Pergunta do relatório: <b>"quantas bolsas vencem até daqui a 7 dias?"</b>. Com o vetor ordenado, basta achar o primeiro índice
cuja validade passa do limite — esse índice <i>é</i> a quantidade. Fizemos a mesma busca binária nas duas formas da aula 02 para comparar.</p>
{lado_a_lado(cpp('int primeiroAposIterativa('), java('Busca.java', 'public static int primeiroAposIterativa('), 'C/C++ · iterativa', 'Java · iterativa')}
{lado_a_lado(cpp('int primeiroAposRecursiva('), java('Busca.java', 'public static int primeiroAposRecursiva('), 'C/C++ · recursiva', 'Java · recursiva')}
<div class="cols">
{complexidade([('Sequencial por id', 'O(n)', 'Percorre o vetor até achar. Usada para comparar com o índice hash O(1).'),
               ('Binária iterativa', 'O(log n)', 'O intervalo <code>[inicio, fim)</code> cai pela metade a cada volta. Espaço O(1).'),
               ('Binária recursiva', 'O(log n)', 'Mesma divisão, mas cada chamada ocupa um quadro na pilha: espaço O(log n). Caso base: <code>inicio &gt;= fim</code>.')])}
<div>{notas([
 ('Bolsa arr[], int n', 'Bolsa[] arr  (arr.length)', 'O parâmetro <code>n</code> some em Java.'),
 ('int meio = inicio + (fim-inicio)/2;', 'idêntico', 'Mesma fórmula nas duas linguagens: evita estouro de <code>int</code> que <code>(inicio+fim)/2</code> poderia causar.'),
])}
<div class="card" style="margin-top:2mm"><h4>Verificação cruzada</h4>No cenário, iterativa = <b>3</b> e recursiva = <b>3</b> (1017, 1020, 1009 vencem até 26/10). O teste
automático confere que as duas batem com uma contagem sequencial.</div></div>
</div>"""))

    # 14 resumo
    S.append(secao(14, "Quadro-resumo de complexidade", "n = estoque · m = bolsas da chave · q = pedidas · v = vencidas · V = vértices · k = paradas", f"""
{complexidade([
 ('Compatibilidade ABO/Rh', 'O(1)', 'Acesso direto <code>[doador][receptor]</code> na matriz 8×8.'),
 ('Inserção FEFO', 'O(m)', 'Percorre a lista até a posição; melhor caso O(1) ao virar cabeça.'),
 ('Retirada FEFO', 'O(1)', 'Remove a cabeça.'),
 ('Descarte de vencidas', 'O(v)', 'Vencidas são prefixo da lista ordenada.'),
 ('Hash do estoque (encadeamento)', 'O(1 + α)', 'α ≤ 24/7 ≈ 3,4 → constante.'),
 ('Índice por id (sondagem linear)', 'O(1) médio', 'Fator de carga 22%; pior caso O(TAM) com agrupamento primário.'),
 ('Fila FIFO', 'O(1)', 'Ponteiros de início e fim.'),
 ('Alocação de um pedido', 'O(v + q)', 'T = 8 constante; independe de n.'),
 ('Floyd-Warshall', 'O(V³)', 'Três laços de V; executa uma vez. Espaço O(V²).'),
 ('Vizinho mais próximo', 'O(k²)', 'k(k+1)/2 consultas à matriz de distâncias.'),
 ('Bubble / Selection / Insertion', 'O(n²)', 'Laços aninhados; Bubble e Insertion chegam a O(n) no melhor caso.'),
 ('Shell / Quick / Merge', 'O(n²) / O(n²) / O(n log n)', 'Quick e Shell são O(n log n) no caso médio; Merge sempre O(n log n) com O(n) de memória extra.'),
 ('Busca sequencial / binária', 'O(n) / O(log n)', 'Recursiva usa O(log n) de pilha.'),
])}
<div class="aviso" style="margin-top:3mm">Todas as justificativas citam a linha de código que gera o custo (o laço, o acesso direto, a
recursão). Os números de comparações das ordenações e o estado do hash vêm da execução real, não de material genérico.</div>"""))

    # 15 dicionário
    S.append(secao(15, "Dicionário geral de tradução C/C++ → Java", "Referência rápida para a banca", notas([
        ('struct Bolsa { ... };', 'public final class Bolsa', 'Dados com comportamento. Imutável porque em Java circula por referência.'),
        ('Bolsa novaBolsa(...)', 'new Bolsa(...)', 'Função fábrica vira construtor.'),
        ('NoBolsa* prox; NULL', 'No prox; null', 'Ponteiro vira referência.'),
        ('atual-&gt;prox', 'atual.prox', 'Java não distingue objeto de ponteiro, então só existe o ponto.'),
        ('new NoBolsa / delete temp', 'new No(b) / (nada)', 'O coletor de lixo libera a memória; some <code>estoqueLiberar()</code>.'),
        ('hashBuscar(HashEstoque* h, ...)', 'hashEstoque.buscar(...)', 'O ponteiro para a estrutura vira o <code>this</code> implícito.'),
        ('hashInicializar(&amp;h)', 'new HashEstoque()', 'A inicialização vira construtor; arrays Java já nascem com <code>null</code>/0.'),
        ('head = fefoInserir(head, b)', 'lista.inserir(b)', 'A cabeça vira atributo, e o método não precisa devolvê-la.'),
        ('bool dequeue(F* f, Pedido* saida)', 'Pedido dequeue()', 'O parâmetro de saída vira valor de retorno (ou <code>null</code>).'),
        ('enum Tipo + const char* NOME[]', 'enum com toString()', 'O rótulo fica no próprio enum; o índice vem de <code>ordinal()</code>.'),
        ('Bolsa arr[], int n', 'Bolsa[] arr', 'O array conhece o próprio tamanho (<code>length</code>).'),
        ('Bolsa bolsas[10]; int qtd;', 'List&lt;Bolsa&gt;', 'Apenas como recipiente do resultado.'),
        ('std::swap(a, b)', 'swap(arr, i, j)', 'Troca manual de referências no array.'),
        ('laço copiando struct a struct', 'arr.clone()', 'Cópia rasa basta porque <code>Bolsa</code> é imutável.'),
        ('g_verboso, g_comparacoes, g_ok', 'atributos de Estoque, Ordenacao, TestesRotaVital', 'Sem estado global: cada objeto tem o seu.'),
        ('const char* data(int) + buffers static', 'String Datas.formatar(int)', 'Java devolve uma <code>String</code> nova com segurança, sem buffer estático.'),
        ('printf("%4lld", ...)', 'System.out.printf("%4d", ...)', 'Mesmos formatos; <code>long</code> usa <code>%d</code> em Java.'),
        ('int main() com funções soltas', 'main() orquestrando objetos', 'Estoque, ServicoAlocacao, Grafo e Roteirizador colaboram; no Spring Boot viram <code>@Service</code> + <code>@RestController</code>.'),
    ])))

    # 16 testes
    S.append(secao(16, "Testes e evidências de execução", "Mesmos casos nas duas linguagens", f"""
<div class="cols3" style="margin-bottom:3mm">
<div class="card escuro"><div class="stat">{N_OK}/{len(TESTES)}<small>testes passando em C++ e em Java</small></div></div>
<div class="card escuro"><div class="stat">0<small>linhas diferentes entre as saídas (fora o cabeçalho com o nome da linguagem)</small></div></div>
<div class="card escuro"><div class="stat">18<small>casos de compatibilidade/incompatibilidade (matriz + alocação)</small></div></div>
</div>
<div class="cols3">{lista_testes()}</div>
<div class="aviso" style="margin-top:2mm">Evidência da tradução: <code>diff cpp/saida_cpp.txt java/saida_java.txt</code> mostra só a primeira linha
("Unidade 2 (C++)" × "Unidade 2 (Java)"). Os dois programas tomam as mesmas decisões de alocação, descarte e rota.</div>"""))

    # 17 critérios
    S.append(secao(17, "Atendimento aos critérios e à regra geral", "Checklist da entrega", """
<div class="cols">
<div class="card"><h4>Critérios da Unidade 2</h4><ul class="check">
<li><b>Compatibilidade ABO/Rh via matriz</b>, com testes de compatibilidade e de incompatibilidade (seções 04 e 16).</li>
<li><b>Integração completa em Java:</b> rota + validade + compatibilidade + consulta por hash no mesmo fluxo (<code>RotaVitalApp</code>, seção 09).</li>
<li><b>Complexidade correta e justificada</b> pela implementação real, com números da execução (seções 04–14).</li>
<li><b>Tradução comentada C/C++ ↔ Java:</b> bloco de cabeçalho em cada classe, comentário <code>// C++:</code> nas linhas alteradas,
código lado a lado e tabelas de raciocínio (seções 03–15).</li>
<li>Os quatro algoritmos centrais: roteirização (vizinho mais próximo), FEFO (inserção ordenada), hash e matriz.</li>
</ul></div>
<div class="card escuro"><h4>Regra geral do Projeto Integrador</h4>
<p>"Uma entrega só em Java, sem a versão em C/C++ correspondente e sem a tradução comentada, não é aceita — mesmo que o código Java
funcione."</p><ul class="check">
<li>Versão C/C++ completa: <code>cpp/rota_vital.cpp</code>, compilando sem avisos com <code>-Wall -Wextra</code>.</li>
<li>Versão Java correspondente: 19 classes em <code>java/src/rotavital</code>.</li>
<li>Tradução comentada no código e neste documento.</li>
<li>Mesma lógica nas duas: comprovada pelos mesmos 32 testes e pela saída idêntica.</li>
<li>Raciocínio exposto: decisões de estrutura, limitações (agrupamento primário, rota gulosa) e melhorias possíveis.</li>
</ul></div></div>"""))

    # 18 executar
    S.append(secao(18, "Como executar · limites e cuidados", "Guia rápido", f"""
<div class="cols">
<div>
<h2>C/C++</h2><div class="saida">cd rota_vital/cpp
g++ -std=c++17 -Wall -Wextra -o rota_vital rota_vital.cpp
./rota_vital          (Windows: rota_vital.exe)</div>
<h2>Java (JDK 11+)</h2><div class="saida">cd rota_vital/java
javac -encoding UTF-8 -d out src/rotavital/*.java      (Windows: src\\rotavital\\*.java)
java -cp out rotavital.RotaVitalApp       # cenário + testes
java -cp out rotavital.TestesRotaVital    # só os testes</div>
<h2>VS Code</h2><p>Abra a pasta <code>rota_vital/java</code> como raiz, ou mantenha <code>rota_vital</code> aberta: o arquivo
<code>.vscode/settings.json</code> incluído aponta <code>java/src</code> como código-fonte. Depois rode
<b>Java: Clean Java Language Server Workspace</b>.</p>
</div>
<div>
<h2>Limites e cuidados de escopo</h2>
<div class="card"><ul class="check">
<li><b>Apenas dados sintéticos</b>: nenhuma informação real de doadores ou pacientes (LGPD).</li>
<li><b>Matriz ABO/Rh didática</b>: não substitui protocolo clínico nem prova cruzada.</li>
<li><b>Telemetria simulada</b>: a cadeia fria é um limite fixo de 120 min, sem sensor real.</li>
<li><b>Grafo limitado</b>: 6 vértices e 9 arestas, como pede a ficha.</li>
<li><b>Sem integração</b> com sistemas oficiais da Hemorrede.</li>
</ul></div>
<h2>Próximos passos</h2>
<p>Expor <code>ServicoAlocacao</code> e <code>Roteirizador</code> como endpoints Spring Boot; rota que priorize janelas apertadas;
hash duplo no índice por id; testes migrados para JUnit 5.</p>
</div></div>"""))
    return "\n".join(S)


def main():
    from playwright.sync_api import sync_playwright
    from pypdf import PdfWriter, PdfReader

    fcss = fontes_css()
    html = f"""<!doctype html><html lang="pt-BR"><head><meta charset="utf-8"><title>Rota Vital U2</title>
<style>{fcss}{CSS}</style></head><body>{corpo()}</body></html>"""
    capa = capa_html(fcss)
    (RAIZ / "docs/_preview.html").write_text(html, encoding="utf-8")
    footer = """<div style="width:100%;font-family:Arial;font-size:7px;color:#6B645B;padding:0 18mm;display:flex;justify-content:space-between;">
<span>ROTA VITAL · Projeto Integrador U2 · C/C++ → Java</span><span><span class="pageNumber"></span> / <span class="totalPages"></span></span></div>"""
    with sync_playwright() as p:
        b = p.chromium.launch()
        pg = b.new_page()
        pg.set_content(capa, wait_until="networkidle")
        pg.pdf(path="/tmp/claude-0/capa.pdf", width="297mm", height="210mm", print_background=True)
        fundo = f"""<!doctype html><html><head><style>@page{{size:297mm 210mm;margin:0}}html,body{{margin:0}}
.f{{width:297mm;height:210mm;background:#FDF6EC;position:relative;overflow:hidden}}
.f svg{{position:absolute;top:0;right:0;width:30mm;height:30mm}}</style></head><body><div class="f">
<svg viewBox="0 0 34 34"><path d="M34 0 V24 A24 24 0 0 1 10 0 Z" fill="#F26A1B"/><path d="M34 0 V12 A12 12 0 0 1 22 0 Z" fill="#1A1A1A"/></svg>
</div></body></html>"""
        pg.set_content(fundo)
        pg.pdf(path="/tmp/claude-0/fundo.pdf", width="297mm", height="210mm", print_background=True)
        pg.set_content(html, wait_until="networkidle")
        pg.pdf(path="/tmp/claude-0/corpo.pdf", width="297mm", height="210mm", print_background=True,
               prefer_css_page_size=True, display_header_footer=True, header_template="<span></span>",
               footer_template=footer, margin={"top": "15mm", "bottom": "16mm", "left": "18mm", "right": "18mm"})
        b.close()
    w = PdfWriter()
    w.add_page(PdfReader("/tmp/claude-0/capa.pdf").pages[0])
    for page in PdfReader("/tmp/claude-0/corpo.pdf").pages:
        fundo = w.add_page(PdfReader("/tmp/claude-0/fundo.pdf").pages[0])
        fundo.merge_page(page)
    w.add_metadata({"/Title": "Rota Vital — Projeto Integrador U2", "/Author": "Equipe Rota Vital"})
    with open(OUT, "wb") as fh:
        w.write(fh)
    (RAIZ / "docs/_preview.html").unlink()
    print("PDF:", OUT, "páginas:", len(PdfReader(OUT).pages))


if __name__ == "__main__":
    main()
