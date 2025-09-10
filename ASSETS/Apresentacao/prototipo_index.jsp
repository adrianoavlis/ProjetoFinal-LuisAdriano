<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ page pageEncoding="UTF-8" %>
<%-- 
  Dashboard JSP – Cesta Básica (alinhado ao escopo do TCC)
  Tecnologias-alvo: Spring Boot 3.5.5 + JSP/HTML/CSS/JS + MS SQL Server.
  Este arquivo é um protótipo estático com dados mockados para apresentação.
  Próximos passos: substituir mocks por endpoints do backend (REST) e/ou integração KNIME.
--%>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>Cesta Básica – Painel Analítico (JSP)</title>
  <!-- Estilos mínimos (Tailwind via CDN para agilidade em protótipo) -->
  <script src="https://cdn.tailwindcss.com"></script>
  <!-- Chart.js para gráficos -->
  <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js"></script>
  <style>
    /* Melhorias sutis */
    .card { @apply bg-white border border-slate-200 rounded-2xl p-4; }
    .tab-active { @apply bg-slate-900 text-white border-slate-900; }
    .tab-idle { @apply bg-white text-slate-900 border border-slate-300 hover:bg-slate-100; }
  </style>
</head>
<body class="w-full min-h-screen bg-slate-50 text-slate-900">
  <!-- Header -->
  <header class="sticky top-0 z-10 bg-white/80 backdrop-blur border-b border-slate-200">
    <div class="max-w-7xl mx-auto px-4 py-4 flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold">Cesta Básica – Painel Analítico</h1>
        <p class="text-sm text-slate-500">Protótipo JSP alinhado ao escopo (DIEESE / IPCA / INPC)</p>
      </div>
      <div class="hidden md:flex items-center gap-2">
        <button class="px-3 py-2 rounded-xl border border-slate-300 hover:bg-slate-100 text-sm">Exportar CSV</button>
        <button class="px-3 py-2 rounded-xl bg-slate-900 text-white hover:bg-slate-800 text-sm">Gerar PDF</button>
      </div>
    </div>
  </header>

  <!-- Tabs -->
  <nav class="max-w-7xl mx-auto px-4 pt-4 flex flex-wrap gap-2">
    <button data-tab="evolucao" class="px-3 py-2 rounded-xl text-sm tab-active">Evolução dos preços</button>
    <button data-tab="regional" class="px-3 py-2 rounded-xl text-sm tab-idle">Comparação regional</button>
    <button data-tab="inflacao" class="px-3 py-2 rounded-xl text-sm tab-idle">Inflação x Cesta</button>
    <button data-tab="peso" class="px-3 py-2 rounded-xl text-sm tab-idle">Peso dos itens</button>
    <button data-tab="eventos" class="px-3 py-2 rounded-xl text-sm tab-idle">Eventos externos</button>
    <button data-tab="scraping" class="px-3 py-2 rounded-xl text-sm tab-idle">Scraping (itens)</button>
  </nav>

  <!-- Conteúdos -->
  <main class="max-w-7xl mx-auto px-4 py-4 space-y-4">
    <!-- Evolução -->
    <section id="tab-evolucao" class="space-y-4">
      <div class="grid grid-cols-1 md:grid-cols-3 gap-3">
        <div class="card">
          <p class="text-xs text-slate-500">Custo atual da cesta</p>
          <p id="kpi-atual" class="text-2xl font-semibold">R$ -</p>
        </div>
        <div class="card">
          <p class="text-xs text-slate-500">Variação mensal</p>
          <p id="kpi-mensal" class="text-2xl font-semibold">-</p>
        </div>
        <div class="card">
          <p class="text-xs text-slate-500">Variação anual</p>
          <p id="kpi-anual" class="text-2xl font-semibold">-</p>
        </div>
      </div>

      <div class="card">
        <div class="flex items-center justify-between mb-2">
          <h2 class="text-lg font-semibold">Série histórica – custo total da cesta</h2>
          <span class="text-xs text-slate-500">Fonte alvo: DIEESE (mock)</span>
        </div>
        <div class="h-80">
          <canvas id="chEvolucao"></canvas>
        </div>
      </div>
    </section>

    <!-- Regional -->
    <section id="tab-regional" class="hidden grid grid-cols-1 xl:grid-cols-2 gap-4">
      <div class="card">
        <div class="flex items-center justify-between mb-2">
          <h2 class="text-lg font-semibold">Custo da cesta por capital (R$)</h2>
          <span class="text-xs text-slate-500">Última apuração (mock)</span>
        </div>
        <div class="h-80">
          <canvas id="chRegional"></canvas>
        </div>
      </div>

      <div class="card">
        <div class="flex items-center justify-between mb-2">
          <h2 class="text-lg font-semibold">Comparar capital específica à média</h2>
          <select id="selCapital" class="px-2 py-1 rounded-lg border border-slate-300 text-sm"></select>
        </div>
        <div class="space-y-2 text-sm">
          <p><span class="text-slate-500">Selecionada:</span> <strong id="capValor">-</strong></p>
          <p><span class="text-slate-500">Média das capitais:</span> <strong id="capMedia">-</strong></p>
          <p><span class="text-slate-500">Diferença:</span> <strong id="capDif">-</strong></p>
        </div>
      </div>
    </section>

    <!-- Inflação x Cesta -->
    <section id="tab-inflacao" class="hidden card">
      <div class="flex items-center justify-between mb-2">
        <h2 class="text-lg font-semibold">Cesta vs IPCA/INPC</h2>
        <span class="text-xs text-slate-500">Taxas mensais de inflação (mock)</span>
      </div>
      <div class="h-80">
        <canvas id="chInflacao"></canvas>
      </div>
    </section>

    <!-- Peso dos itens -->
    <section id="tab-peso" class="hidden grid grid-cols-1 xl:grid-cols-2 gap-4">
      <div class="card">
        <div class="flex items-center justify-between mb-2">
          <h2 class="text-lg font-semibold">Peso dos itens na cesta</h2>
          <span class="text-xs text-slate-500">Participação % (mock)</span>
        </div>
        <div class="h-80">
          <canvas id="chPesoPizza"></canvas>
        </div>
      </div>

      <div class="card">
        <h2 class="text-lg font-semibold mb-2">Top itens que mais pesaram no mês</h2>
        <div class="h-80">
          <canvas id="chPesoBar"></canvas>
        </div>
      </div>
    </section>

    <!-- Eventos externos -->
    <section id="tab-eventos" class="hidden card">
      <div class="flex items-center justify-between mb-2">
        <h2 class="text-lg font-semibold">Linha do tempo com eventos externos</h2>
        <span class="text-xs text-slate-500">Rótulos e notas (mock)</span>
      </div>
      <div class="space-y-3">
        <div class="h-80"><canvas id="chEventos"></canvas></div>
        <ul id="listaEventos" class="text-sm text-slate-700 list-disc pl-5"></ul>
      </div>
    </section>

    <!-- Scraping -->
    <section id="tab-scraping" class="hidden card">
      <div class="flex items-center justify-between mb-2">
        <h2 class="text-lg font-semibold">Catálogo de itens (coleta externa)</h2>
        <div class="flex items-center gap-2">
          <button class="px-3 py-2 rounded-xl border border-slate-300 hover:bg-slate-100 text-sm">Importar CSV</button>
          <button class="px-3 py-2 rounded-xl bg-slate-900 text-white hover:bg-slate-800 text-sm">Atualizar (scraping)</button>
        </div>
      </div>

      <div class="overflow-auto border border-slate-200 rounded-2xl">
        <table class="min-w-full text-sm">
          <thead>
            <tr class="bg-slate-50 text-slate-600">
              <th class="text-left p-3">Nome</th>
              <th class="text-left p-3">Categoria</th>
              <th class="text-left p-3">Mercado</th>
              <th class="text-right p-3">Preço</th>
              <th class="text-left p-3">Fonte</th>
              <th class="text-left p-3">Coletado em</th>
            </tr>
          </thead>
          <tbody id="tbScraping"></tbody>
        </table>
      </div>
      <p class="text-xs text-slate-500 mt-3">Obs.: respeitar termos de uso, robots.txt e créditos de fonte nos relatórios.</p>
    </section>
  </main>

  <footer class="max-w-7xl mx-auto px-4 pb-12 text-center text-xs text-slate-500">
    Este protótipo cobre todos os eixos do escopo. Próximos passos: conectar API (Spring Boot/MS SQL), importar séries do DIEESE e índices (IPCA/INPC), e acoplar pipelines KNIME.
  </footer>

  <script>
    // --- Dados mock (mesmos conceitos do protótipo React) ---
    const evolucaoHistorica = [
      { mes: "2024-09", cesta: 298.3 },
      { mes: "2024-10", cesta: 301.1 },
      { mes: "2024-11", cesta: 304.9 },
      { mes: "2024-12", cesta: 306.2 },
      { mes: "2025-01", cesta: 309.7 },
      { mes: "2025-02", cesta: 312.1 },
      { mes: "2025-03", cesta: 314.6 },
      { mes: "2025-04", cesta: 316.0 },
      { mes: "2025-05", cesta: 318.4 },
      { mes: "2025-06", cesta: 320.0 },
      { mes: "2025-07", cesta: 322.4 },
      { mes: "2025-08", cesta: 323.9 },
      { mes: "2025-09", cesta: 321.8 },
    ];

    const capitais = ["São Paulo","Rio de Janeiro","Belo Horizonte","Porto Alegre","Salvador","Fortaleza","Manaus","Curitiba","Recife"];
    const comparacaoRegional = capitais.map((c, i) => ({ capital: c, custo: 280 + (i * 6) + (i % 2 ? 12 : 0) }));

    const inflacaoXCesta = [
      { mes: "2025-01", cesta: 309.7, ipca: 0.4, inpc: 0.5 },
      { mes: "2025-02", cesta: 312.1, ipca: 0.8, inpc: 0.7 },
      { mes: "2025-03", cesta: 314.6, ipca: 0.3, inpc: 0.4 },
      { mes: "2025-04", cesta: 316.0, ipca: 0.5, inpc: 0.6 },
      { mes: "2025-05", cesta: 318.4, ipca: 0.6, inpc: 0.5 },
      { mes: "2025-06", cesta: 320.0, ipca: 0.2, inpc: 0.3 },
      { mes: "2025-07", cesta: 322.4, ipca: 0.4, inpc: 0.4 },
      { mes: "2025-08", cesta: 323.9, ipca: 0.3, inpc: 0.3 },
      { mes: "2025-09", cesta: 321.8, ipca: -0.1, inpc: 0.0 },
    ];

    const pesoItens = [
      { name: "Carnes", value: 24 },
      { name: "Grãos", value: 20 },
      { name: "Laticínios", value: 18 },
      { name: "Hortifruti", value: 16 },
      { name: "Açúcar e Café", value: 12 },
      { name: "Higiene/Limpeza", value: 10 },
    ];

    const eventosExternos = [
      { data: "2025-02", rotulo: "Chuvas no Sul", comentario: "Alta hortifruti" },
      { data: "2025-05", rotulo: "Alta do dólar", comentario: "Importados/insumos" },
      { data: "2025-08", rotulo: "Geada no Centro-Sul", comentario: "Pressão em grãos" },
    ];

    const scrapingItens = [
      { nome: "Arroz 5kg", categoria: "Grãos", mercado: "Guanabara", preco: 22.9, url: "#", coletadoEm: "2025-09-01" },
      { nome: "Feijão 1kg", categoria: "Grãos", mercado: "Mundial", preco: 7.49, url: "#", coletadoEm: "2025-09-02" },
      { nome: "Leite 1L", categoria: "Laticínios", mercado: "Prezunic", preco: 4.79, url: "#", coletadoEm: "2025-09-03" },
      { nome: "Óleo 900ml", categoria: "Mercearia", mercado: "Supermarket", preco: 6.49, url: "#", coletadoEm: "2025-09-04" },
    ];

    // --- Utilitários ---
    const brl = (v) => v.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
    const pct = (v) => `${v.toFixed(1)}%`;

    function calcKPIs(serie) {
      const last = serie[serie.length - 1].cesta;
      const prev = serie[serie.length - 2]?.cesta ?? last;
      const anoAtras = serie[Math.max(serie.length - 13, 0)]?.cesta ?? last;
      return {
        atual: last,
        varMensal: ((last - prev) / prev) * 100,
        varAnual: ((last - anoAtras) / anoAtras) * 100
      };
    }

    // --- Charts ---
    let chEvolucao, chRegional, chInflacao, chPesoPizza, chPesoBar, chEventos;

    function buildEvolucao() {
      const k = calcKPIs(evolucaoHistorica);
      document.getElementById('kpi-atual').textContent = brl(k.atual);
      document.getElementById('kpi-mensal').textContent = pct(Math.abs(k.varMensal));
      document.getElementById('kpi-mensal').classList.toggle('text-emerald-600', k.varMensal >= 0);
      document.getElementById('kpi-mensal').classList.toggle('text-rose-600', k.varMensal < 0);
      document.getElementById('kpi-anual').textContent = pct(Math.abs(k.varAnual));
      document.getElementById('kpi-anual').classList.toggle('text-emerald-600', k.varAnual >= 0);
      document.getElementById('kpi-anual').classList.toggle('text-rose-600', k.varAnual < 0);

      const ctx = document.getElementById('chEvolucao');
      chEvolucao?.destroy();
      chEvolucao = new Chart(ctx, {
        type: 'line',
        data: {
          labels: evolucaoHistorica.map(x => x.mes),
          datasets: [{
            label: 'Cesta (R$)',
            data: evolucaoHistorica.map(x => x.cesta),
            borderWidth: 2,
            fill: true,
            tension: 0.35
          }]
        },
        options: { responsive: true, maintainAspectRatio: false }
      });
    }

    function buildRegional() {
      const ctx = document.getElementById('chRegional');
      chRegional?.destroy();
      chRegional = new Chart(ctx, {
        type: 'bar',
        data: {
          labels: comparacaoRegional.map(x => x.capital),
          datasets: [{ label: 'Custo (R$)', data: comparacaoRegional.map(x => x.custo), borderWidth: 1 }]
        },
        options: { responsive: true, maintainAspectRatio: false }
      });

      // preencher select e cálculos
      const sel = document.getElementById('selCapital');
      sel.innerHTML = comparacaoRegional.map((x) => `<option>${x.capital}</option>`).join('');
      const media = comparacaoRegional.reduce((a,b)=>a+b.custo,0) / comparacaoRegional.length;
      const updateComp = () => {
        const val = sel.value;
        const linha = comparacaoRegional.find(x => x.capital === val);
        const dif = (linha?.custo ?? 0) - media;
        document.getElementById('capValor').textContent = brl(linha?.custo ?? 0);
        document.getElementById('capMedia').textContent = brl(media);
        const difEl = document.getElementById('capDif');
        difEl.textContent = (dif>=0? '+':'-') + brl(Math.abs(dif));
        difEl.classList.toggle('text-rose-600', dif>=0);
        difEl.classList.toggle('text-emerald-600', dif<0);
      };
      sel.onchange = updateComp;
      updateComp();
    }

    function buildInflacao() {
      const ctx = document.getElementById('chInflacao');
      chInflacao?.destroy();
      chInflacao = new Chart(ctx, {
        type: 'line',
        data: {
          labels: inflacaoXCesta.map(x => x.mes),
          datasets: [
            { label: 'Cesta (R$)', data: inflacaoXCesta.map(x => x.cesta), borderWidth: 2, yAxisID: 'y1', tension: 0.35 },
            { label: 'IPCA (%)', data: inflacaoXCesta.map(x => x.ipca), borderWidth: 2, yAxisID: 'y2', tension: 0.35 },
            { label: 'INPC (%)', data: inflacaoXCesta.map(x => x.inpc), borderWidth: 2, yAxisID: 'y2', tension: 0.35 }
          ]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          scales: {
            y1: { type: 'linear', position: 'left' },
            y2: { type: 'linear', position: 'right' }
          }
        }
      });
    }

    function buildPeso() {
      const ctx1 = document.getElementById('chPesoPizza');
      chPesoPizza?.destroy();
      chPesoPizza = new Chart(ctx1, {
        type: 'pie',
        data: {
          labels: pesoItens.map(x => x.name),
          datasets: [{ data: pesoItens.map(x => x.value) }]
        },
        options: { responsive: true, maintainAspectRatio: false }
      });

      const ctx2 = document.getElementById('chPesoBar');
      chPesoBar?.destroy();
      chPesoBar = new Chart(ctx2, {
        type: 'bar',
        data: {
          labels: pesoItens.map(x => x.name),
          datasets: [{ label: 'Participação (%)', data: pesoItens.map(x => x.value) }]
        },
        options: {
          indexAxis: 'y',
          responsive: true,
          maintainAspectRatio: false
        }
      });
    }

    function buildEventos() {
      // Linha com marcadores (usando mesma série da evolução)
      const ctx = document.getElementById('chEventos');
      chEventos?.destroy();
      chEventos = new Chart(ctx, {
        type: 'line',
        data: { labels: evolucaoHistorica.map(x=>x.mes), datasets: [{ label:'Cesta (R$)', data: evolucaoHistorica.map(x=>x.cesta), borderWidth:2, tension:0.35 }] },
        options: { responsive:true, maintainAspectRatio:false }
      });

      const ul = document.getElementById('listaEventos');
      ul.innerHTML = eventosExternos.map(ev => `<li><span class="font-medium">${ev.data} – ${ev.rotulo}:</span> ${ev.comentario}</li>`).join('');
    }

    function buildScraping() {
      const tbody = document.getElementById('tbScraping');
      tbody.innerHTML = scrapingItens.map(x => `
        <tr class="border-t border-slate-100 hover:bg-slate-50">
          <td class="p-3 font-medium">${x.nome}</td>
          <td class="p-3">${x.categoria}</td>
          <td class="p-3">${x.mercado}</td>
          <td class="p-3 text-right">${brl(x.preco)}</td>
          <td class="p-3"><a class="text-sky-600 hover:underline" href="${x.url}">link</a></td>
          <td class="p-3">${x.coletadoEm}</td>
        </tr>
      `).join('');
    }

    // --- Controle de tabs ---
    const tabButtons = document.querySelectorAll('button[data-tab]');
    const sections = {
      evolucao: document.getElementById('tab-evolucao'),
      regional: document.getElementById('tab-regional'),
      inflacao: document.getElementById('tab-inflacao'),
      peso: document.getElementById('tab-peso'),
      eventos: document.getElementById('tab-eventos'),
      scraping: document.getElementById('tab-scraping')
    };

    function showTab(id) {
      Object.keys(sections).forEach(k => {
        if (k === id) { sections[k].classList.remove('hidden'); }
        else { sections[k].classList.add('hidden'); }
      });

      tabButtons.forEach(btn => {
        const active = btn.getAttribute('data-tab') === id;
        btn.classList.toggle('tab-active', active);
        btn.classList.toggle('tab-idle', !active);
      });

      // Render preguiçoso para evitar custo inicial alto
      if (id === 'evolucao') buildEvolucao();
      if (id === 'regional') buildRegional();
      if (id === 'inflacao') buildInflacao();
      if (id === 'peso') buildPeso();
      if (id === 'eventos') buildEventos();
      if (id === 'scraping') buildScraping();
    }

    tabButtons.forEach(btn => btn.addEventListener('click', () => showTab(btn.getAttribute('data-tab'))));

    // Inicialização padrão
    showTab('evolucao');
  </script>
</body>
</html>
