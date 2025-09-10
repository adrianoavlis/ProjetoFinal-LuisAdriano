<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"
	isELIgnored="true"%>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
<meta charset="UTF-8" />
<title>Cesta Básica – Painel Analítico (JSP + Bootstrap)</title>

<!-- Bootstrap 5 (CSS + JS) -->
<link rel="stylesheet"
	href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" />
<script
	src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>

<!-- Chart.js + plugin annotation -->
<script
	src="https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js"></script>
<script
	src="https://cdn.jsdelivr.net/npm/chartjs-plugin-annotation@3.0.1/dist/chartjs-plugin-annotation.min.js"></script>

<style>
.card-kpi p.value {
	font-size: 1.6rem;
	font-weight: 600;
	margin: 0;
}

.text-pos {
	color: #059669 !important;
} /* emerald-600 */
.text-neg {
	color: #e11d48 !important;
} /* rose-600 */
.chart-wrap {
	height: 420px;
}

.badge-chip {
	font-weight: 500;
}

.table-sm td, .table-sm th {
	padding: .5rem .75rem;
}
</style>
</head>
<body class="bg-light">
	<!-- Header -->
	<header class="bg-white border-bottom sticky-top">
		<div
			class="container py-3 d-flex align-items-center justify-content-between">
			<div>
				<h1 class="h4 mb-1">Painel Analítico - Cesta Básica</h1>
			</div>
			<div class="d-none d-md-flex gap-2">
				<button id="btnSnapshot" class="btn btn-dark btn-sm">Baixar
					PNG</button>
				<a class="btn btn-outline-primary"
					href="${pageContext.request.contextPath}/cesta/ui">Importar
					dados (DIEESE)</a>
			</div>
		</div>
	</header>

	<main class="container my-4">

		<!-- Abas -->
		<ul class="nav nav-pills mb-3" id="tabs" role="tablist">
			<li class="nav-item" role="presentation"><button
					class="nav-link active" data-bs-toggle="pill"
					data-bs-target="#tab-evolucao" type="button">Evolução</button></li>
			<li class="nav-item" role="presentation"><button
					class="nav-link" data-bs-toggle="pill"
					data-bs-target="#tab-regional" type="button">Comparativo
					regional</button></li>
			<li class="nav-item" role="presentation"><button
					class="nav-link" data-bs-toggle="pill"
					data-bs-target="#tab-inflacao" type="button">Inflação x
					Cesta</button></li>
			<li class="nav-item" role="presentation"><button
					class="nav-link" data-bs-toggle="pill" data-bs-target="#tab-peso"
					type="button">Peso dos itens</button></li>
			<li class="nav-item" role="presentation"><button
					class="nav-link" data-bs-toggle="pill"
					data-bs-target="#tab-eventos" type="button">Eventos
					externos</button></li>
			<li class="nav-item" role="presentation"><button
					class="nav-link" data-bs-toggle="pill"
					data-bs-target="#tab-scraping" type="button">Scraping
					(itens)</button></li>
		</ul>

		<div class="tab-content">

			<!-- EVOLUÇÃO -->
			<section class="tab-pane fade show active" id="tab-evolucao">
				<div class="row g-3">
					<div class="col-md-4">
						<div class="card card-kpi shadow-sm">
							<div class="card-body">
								<div class="text-muted small">Custo atual da cesta</div>
								<p id="kpi-atual" class="value">R$ -</p>
							</div>
						</div>
					</div>
					<div class="col-md-4">
						<div class="card card-kpi shadow-sm">
							<div class="card-body">
								<div class="text-muted small">Variação mensal</div>
								<p id="kpi-mensal" class="value">-</p>
							</div>
						</div>
					</div>
					<div class="col-md-4">
						<div class="card card-kpi shadow-sm">
							<div class="card-body">
								<div class="text-muted small">Variação anual</div>
								<p id="kpi-anual" class="value">-</p>
							</div>
						</div>
					</div>
				</div>

				<div class="card shadow-sm mt-3">
					<div class="card-body">
						<div
							class="d-flex justify-content-between align-items-center mb-2">
							<h2 class="h6 mb-0">Série histórica – custo total da cesta</h2>
							<span class="small text-muted">Fonte alvo: DIEESE (mock)</span>
						</div>
						<div class="chart-wrap">
							<canvas id="chEvolucao"></canvas>
						</div>
					</div>
				</div>
			</section>

			<!-- COMPARATIVO REGIONAL -->
			<section class="tab-pane fade" id="tab-regional">
				<div class="card shadow-sm mb-3">
					<div class="card-body">
						<div class="row g-3 align-items-end">
							<div class="col-lg-6">
								<label class="form-label small">Selecione municípios
									(múltiplos)</label> <select id="selMunicipios" class="form-select"
									multiple size="6"></select>
								<div id="chipsMunicipios" class="mt-2 d-flex flex-wrap gap-2"></div>
								<div class="mt-2 d-flex gap-2">
									<button id="btnSelTodos"
										class="btn btn-outline-secondary btn-sm">Selecionar
										todos</button>
									<button id="btnLimparSel"
										class="btn btn-outline-secondary btn-sm">Limpar</button>
								</div>
							</div>
							<div class="col-lg-3">
								<label class="form-label small">Métrica</label> <select
									id="selMetricaRegional" class="form-select">
									<option value="cesta">Custo da cesta (R$)</option>
									<option value="var">Variação mensal (%)</option>
								</select>
							</div>
							<div class="col-lg-3 d-grid">
								<button id="btnAplicarRegional" class="btn btn-dark">Comparar</button>
							</div>
						</div>
					</div>
				</div>

				<div class="row g-3">
					<div class="col-xl-6">
						<div class="card shadow-sm">
							<div class="card-body">
								<h3 class="h6 mb-2">Evolução por município</h3>
								<div class="chart-wrap">
									<canvas id="chRegionalLinha"></canvas>
								</div>
							</div>
						</div>
					</div>
					<div class="col-xl-6">
						<div class="card shadow-sm">
							<div class="card-body">
								<h3 class="h6 mb-2">Custo atual (último mês)</h3>
								<div class="chart-wrap">
									<canvas id="chRegionalBar"></canvas>
								</div>
							</div>
						</div>
					</div>
				</div>

				<div class="card shadow-sm mt-3">
					<div class="card-body">
						<h3 class="h6 mb-2">Resumo comparativo</h3>
						<div class="table-responsive">
							<table class="table table-sm align-middle">
								<thead class="table-light">
									<tr>
										<th>Município</th>
										<th class="text-end">Custo atual</th>
										<th class="text-end">Menor no período</th>
										<th class="text-end">Maior no período</th>
										<th class="text-end">Variação mensal</th>
										<th class="text-end">Variação anual</th>
									</tr>
								</thead>
								<tbody id="tbResumoRegional"></tbody>
							</table>
						</div>
					</div>
				</div>
			</section>

			<!-- INFLAÇÃO X CESTA -->
			<section class="tab-pane fade" id="tab-inflacao">
				<div class="card shadow-sm">
					<div class="card-body">
						<div
							class="d-flex justify-content-between align-items-center mb-2">
							<h2 class="h6 mb-0">Cesta vs IPCA/INPC</h2>
							<div class="d-flex align-items-center gap-2">
								<span class="small text-muted">Índice</span> <select
									id="selIndice" class="form-select form-select-sm"
									style="width: auto;">
									<option value="ipca" selected>IPCA</option>
									<option value="inpc">INPC</option>
								</select>
							</div>
						</div>
						<div class="chart-wrap">
							<canvas id="chInflacao"></canvas>
						</div>
					</div>
				</div>
			</section>

			<!-- PESO DOS ITENS -->
			<section class="tab-pane fade" id="tab-peso">
				<div class="row g-3">
					<div class="col-xl-6">
						<div class="card shadow-sm">
							<div class="card-body">
								<div
									class="d-flex justify-content-between align-items-center mb-2">
									<h2 class="h6 mb-0">Peso dos itens na cesta</h2>
									<span class="small text-muted">Participação % (mock)</span>
								</div>
								<div class="chart-wrap">
									<canvas id="chPesoPizza"></canvas>
								</div>
							</div>
						</div>
					</div>
					<div class="col-xl-6">
						<div class="card shadow-sm">
							<div class="card-body">
								<h2 class="h6 mb-2">Top itens do mês</h2>
								<div class="chart-wrap">
									<canvas id="chPesoBar"></canvas>
								</div>
							</div>
						</div>
					</div>
				</div>
			</section>

			<!-- EVENTOS EXTERNOS -->
			<section class="tab-pane fade" id="tab-eventos">
				<div class="card shadow-sm">
					<div class="card-body">
						<div
							class="d-flex justify-content-between align-items-center mb-2">
							<h2 class="h6 mb-0">Linha do tempo com eventos externos</h2>
							<div class="d-flex gap-2">
								<button id="btnChoqueClima"
									class="btn btn-outline-secondary btn-sm">Simular
									choque climático</button>
								<button id="btnChoqueCambio"
									class="btn btn-outline-secondary btn-sm">Simular alta
									do dólar</button>
								<button id="btnKnime" class="btn btn-dark btn-sm">Reprocessar
									KNIME (mock)</button>
							</div>
						</div>
						<div class="chart-wrap">
							<canvas id="chEventos"></canvas>
						</div>
						<ul id="listaEventos" class="mt-3 small"></ul>
					</div>
				</div>
			</section>

			<!-- SCRAPING -->
			<section class="tab-pane fade" id="tab-scraping">
				<div class="card shadow-sm">
					<div class="card-body">
						<div
							class="d-flex justify-content-between align-items-center mb-2">
							<h2 class="h6 mb-0">Catálogo de itens (coleta externa)</h2>
							<div class="d-flex gap-2">
								<button id="btnImportCsv"
									class="btn btn-outline-secondary btn-sm">Importar CSV</button>
								<button id="btnAtualizarScraping" class="btn btn-dark btn-sm">Atualizar
									(scraping)</button>
							</div>
						</div>
						<div class="table-responsive border rounded">
							<table class="table table-sm mb-0">
								<thead class="table-light">
									<tr>
										<th>Nome</th>
										<th>Categoria</th>
										<th>Mercado</th>
										<th class="text-end">Preço</th>
										<th>Fonte</th>
										<th>Coletado em</th>
									</tr>
								</thead>
								<tbody id="tbScraping"></tbody>
							</table>
						</div>
						<div class="small text-muted mt-2">Obs.: respeitar termos de
							uso, robots.txt e créditos de fonte nos relatórios.</div>
					</div>
				</div>
			</section>

		</div>
	</main>

	<script>
    // =======================
    // DADOS MOCK (iguais ao seu TS)
    // =======================
    const evolucaoHistorica = [
      { mes: "2024-09", cesta: 298.3 },{ mes: "2024-10", cesta: 301.1 },
      { mes: "2024-11", cesta: 304.9 },{ mes: "2024-12", cesta: 306.2 },
      { mes: "2025-01", cesta: 309.7 },{ mes: "2025-02", cesta: 312.1 },
      { mes: "2025-03", cesta: 314.6 },{ mes: "2025-04", cesta: 316.0 },
      { mes: "2025-05", cesta: 318.4 },{ mes: "2025-06", cesta: 320.0 },
      { mes: "2025-07", cesta: 322.4 },{ mes: "2025-08", cesta: 323.9 },
      { mes: "2025-09", cesta: 321.8 },
    ];

    const capitais = ["São Paulo","Rio de Janeiro","Belo Horizonte","Porto Alegre","Salvador","Fortaleza","Manaus","Curitiba","Recife"];
    const comparacaoRegional = capitais.map((c,i)=>({ capital: c, custo: 280 + (i*6) + (i%2 ? 12 : 0) }));

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

    let pesoItens = [
      { name: "Carnes", value: 24 },{ name: "Grãos", value: 20 },
      { name: "Laticínios", value: 18 },{ name: "Hortifruti", value: 16 },
      { name: "Açúcar e Café", value: 12 },{ name: "Higiene/Limpeza", value: 10 },
    ];

    let eventosExternos = [
      { data: "2025-02", rotulo: "Chuvas no Sul", comentario: "Alta hortifruti" },
      { data: "2025-05", rotulo: "Alta do dólar", comentario: "Importados/insumos" },
      { data: "2025-08", rotulo: "Geada no Centro-Sul", comentario: "Pressão em grãos" },
    ];

    let scrapingItens = [
      { nome: "Arroz 5kg", categoria: "Grãos", mercado: "Guanabara", preco: 22.9, url: "#", coletadoEm: "2025-09-01" },
      { nome: "Feijão 1kg", categoria: "Grãos", mercado: "Mundial", preco: 7.49, url: "#", coletadoEm: "2025-09-02" },
      { nome: "Leite 1L", categoria: "Laticínios", mercado: "Prezunic", preco: 4.79, url: "#", coletadoEm: "2025-09-03" },
      { nome: "Óleo 900ml", categoria: "Mercearia", mercado: "Supermarket", preco: 6.49, url: "#", coletadoEm: "2025-09-04" },
    ];

    const COLORS = ["#0ea5e9","#22c55e","#f59e0b","#ef4444","#8b5cf6","#14b8a6","#84cc16","#06b6d4"];

    // Utils
    const brl = (v) => v?.toLocaleString("pt-BR",{style:"currency",currency:"BRL"});
    const pct = (v) => `${(v??0).toFixed(1)}%`;
    function kpisEvolucao(serie){
      if(!serie.length) return {atual:0,varMensal:0,varAnual:0};
      const last = serie[serie.length-1].cesta;
      const prev = serie[serie.length-2]?.cesta ?? last;
      const anoAtras = serie[Math.max(serie.length-13, 0)]?.cesta ?? last;
      return { atual:last, varMensal:((last-prev)/prev)*100, varAnual:((last-anoAtras)/anoAtras)*100 };
    }

    // ========= CHARTS =========
    let chEvolucao, chRegionalLinha, chRegionalBar, chInflacao, chPesoPizza, chPesoBar, chEventos;

    function buildEvolucao(){
      const k = kpisEvolucao(evolucaoHistorica);
      document.getElementById('kpi-atual').textContent = brl(k.atual);
      const m = document.getElementById('kpi-mensal');
      const a = document.getElementById('kpi-anual');
      m.textContent = pct(Math.abs(k.varMensal));
      a.textContent = pct(Math.abs(k.varAnual));
      m.classList.toggle('text-pos', k.varMensal>=0); m.classList.toggle('text-neg', k.varMensal<0);
      a.classList.toggle('text-pos', k.varAnual>=0);  a.classList.toggle('text-neg',  k.varAnual<0);

      chEvolucao?.destroy();
      chEvolucao = new Chart(document.getElementById('chEvolucao'), {
        type: 'line',
        data: {
          labels: evolucaoHistorica.map(x=>x.mes),
          datasets: [{ label:'Cesta (R$)', data: evolucaoHistorica.map(x=>x.cesta), fill:true, borderWidth:2, tension:.35 }]
        },
        options: { responsive:true, maintainAspectRatio:false }
      });
    }

    // ---- Regional (multi) ----
    const MUNICIPIOS = capitais.map((c, i)=>({ id:`m${i}`, nome:c }));
    const mesesFull = evolucaoHistorica.map(x=>x.mes);
    const seriesMunicipio = Object.fromEntries(MUNICIPIOS.map((m, idx)=>{
      // cria uma série por município deslocando levemente os valores da nacional
      const desloc = (idx%3 - 1) * 1.2; // -1.2, 0, +1.2
      const serie = evolucaoHistorica.map(r=>({ mes:r.mes, cesta:+(r.cesta + desloc* (Math.random()*4+2)).toFixed(1) }));
      return [m.id, serie];
    }));

    function selectedMunicipios(){
      const sel = document.getElementById('selMunicipios');
      return Array.from(sel.selectedOptions).map(o=>o.value);
    }
    function renderMunicipios(){
      const sel = document.getElementById('selMunicipios');
      sel.innerHTML = MUNICIPIOS.map(m=>`<option value="${m.id}">${m.nome}</option>`).join('');
      // default: Rio, São Paulo, Belo Horizonte (ids m1,m0,m2 conforme array)
      [ 'm1','m0','m2' ].forEach(v=>{
        const opt = Array.from(sel.options).find(o=>o.value===v); if(opt) opt.selected = true;
      });
      renderChips();
    }
    function renderChips(){
      const chips = document.getElementById('chipsMunicipios');
      const nomes = selectedMunicipios().map(id=>MUNICIPIOS.find(m=>m.id===id)?.nome).filter(Boolean);
      chips.innerHTML = nomes.map(n=>`<span class="badge bg-light text-dark border badge-chip">${n}</span>`).join('');
    }
    document.getElementById('selMunicipios').addEventListener('change', renderChips);
    document.getElementById('btnSelTodos').addEventListener('click', ()=>{ 
      const sel = document.getElementById('selMunicipios'); Array.from(sel.options).forEach(o=>o.selected=true); renderChips();
    });
    document.getElementById('btnLimparSel').addEventListener('click', ()=>{
      const sel = document.getElementById('selMunicipios'); Array.from(sel.options).forEach(o=>o.selected=false); renderChips();
    });

    function buildRegional(){
      const ids = selectedMunicipios();
      const metrica = document.getElementById('selMetricaRegional').value; // 'cesta' | 'var'
      const labels = mesesFull;

      const datasetsLinha = ids.map((id, idx)=>{
        const serie = seriesMunicipio[id] ?? [];
        const dataValues = metrica==='cesta'
          ? serie.map(x=>x.cesta)
          : serie.map((x,i,arr)=> i===0? 0 : +(((x.cesta-arr[i-1].cesta)/arr[i-1].cesta)*100).toFixed(2));
        return {
          label: MUNICIPIOS.find(m=>m.id===id)?.nome || id,
          data: dataValues,
          borderColor: COLORS[idx%COLORS.length],
          backgroundColor: COLORS[idx%COLORS.length],
          borderWidth:2, tension:.35
        };
      });

      chRegionalLinha?.destroy();
      chRegionalLinha = new Chart(document.getElementById('chRegionalLinha'), {
        type:'line', data:{ labels, datasets: datasetsLinha },
        options:{ responsive:true, maintainAspectRatio:false }
      });

      // barras do último mês
      const lastMes = labels[labels.length-1];
      const barLabels = ids.map(id=>MUNICIPIOS.find(m=>m.id===id)?.nome || id);
      const barData = ids.map(id => {
        const serie = seriesMunicipio[id] ?? [];
        const ponto = serie.find(s=>s.mes===lastMes);
        return ponto?.cesta ?? 0;
      });

      chRegionalBar?.destroy();
      chRegionalBar = new Chart(document.getElementById('chRegionalBar'), {
        type:'bar', data:{ labels:barLabels, datasets:[{ label:'Custo atual (R$)', data:barData, backgroundColor:'#22c55e' }] },
        options:{ responsive:true, maintainAspectRatio:false }
      });

      // tabela resumo
      const tbody = document.getElementById('tbResumoRegional');
      tbody.innerHTML = ids.map(id=>{
        const nome = MUNICIPIOS.find(m=>m.id===id)?.nome || id;
        const serie = seriesMunicipio[id] ?? [];
        const custos = serie.map(x=>x.cesta);
        const atual = custos.at(-1) ?? 0;
        const menor = Math.min(...custos);
        const maior = Math.max(...custos);
        const varMensal = custos.length>1 ? ((custos.at(-1)-custos.at(-2))/custos.at(-2))*100 : 0;
        const varAnual  = custos.length>12? ((custos.at(-1)-custos.at(-13))/custos.at(-13))*100 : ((custos.at(-1)-custos[0])/custos[0])*100;
        return `
        <tr>
          <td>${nome}</td>
          <td class="text-end">${brl(atual)}</td>
          <td class="text-end">${brl(menor)}</td>
          <td class="text-end">${brl(maior)}</td>
          <td class="text-end ${varMensal>=0?'text-pos':'text-neg'}">${pct(Math.abs(varMensal))}</td>
          <td class="text-end ${varAnual>=0?'text-pos':'text-neg'}">${pct(Math.abs(varAnual))}</td>
        </tr>`;
      }).join('');
    }
    document.getElementById('btnAplicarRegional').addEventListener('click', buildRegional);

    // ---- Inflação x Cesta ----
    function buildInflacao(){
      const indice = document.getElementById('selIndice').value; // ipca|inpc
      chInflacao?.destroy();
      chInflacao = new Chart(document.getElementById('chInflacao'), {
        type:'line',
        data:{
          labels: inflacaoXCesta.map(x=>x.mes),
          datasets: [
            { label:'Cesta (R$)', data: inflacaoXCesta.map(x=>x.cesta), borderWidth:2, tension:.35, yAxisID:'y1' },
            { label: indice.toUpperCase() + ' (%)', data: inflacaoXCesta.map(x=>x[indice]), borderWidth:2, tension:.35, yAxisID:'y2' }
          ]
        },
        options:{
          responsive:true, maintainAspectRatio:false,
          scales:{ y1:{ position:'left' }, y2:{ position:'right' } }
        }
      });
    }
    document.getElementById('selIndice').addEventListener('change', buildInflacao);

    // ---- Peso dos Itens ----
    function buildPeso(){
      chPesoPizza?.destroy();
      chPesoPizza = new Chart(document.getElementById('chPesoPizza'), {
        type:'pie',
        data:{ labels: pesoItens.map(x=>x.name), datasets:[{ data: pesoItens.map(x=>x.value), backgroundColor: COLORS }] },
        options:{ responsive:true, maintainAspectRatio:false }
      });

      chPesoBar?.destroy();
      chPesoBar = new Chart(document.getElementById('chPesoBar'), {
        type:'bar',
        data:{ labels: pesoItens.map(x=>x.name), datasets:[{ label:'Participação (%)', data: pesoItens.map(x=>x.value), backgroundColor:'#f59e0b' }] },
        options:{ indexAxis:'y', responsive:true, maintainAspectRatio:false }
      });
    }

    // ---- Eventos Externos ----
    function buildEventos(){
      const annotations = {};
      eventosExternos.forEach((ev, i)=>{
        annotations['line'+i] = {
          type: 'line',
          xMin: evolucaoHistorica.findIndex(r=>r.mes===ev.data),
          xMax: evolucaoHistorica.findIndex(r=>r.mes===ev.data),
          borderColor: '#ef4444', borderWidth: 2, borderDash: [6,6],
          label: { enabled:true, position:'start', content: ev.rotulo }
        };
      });

      chEventos?.destroy();
      chEventos = new Chart(document.getElementById('chEventos'), {
        type:'line',
        data:{ labels: evolucaoHistorica.map(r=>r.mes), datasets:[{ label:'Cesta (R$)', data: evolucaoHistorica.map(r=>r.cesta), borderWidth:2, tension:.35 }] },
        options:{
          responsive:true, maintainAspectRatio:false,
          plugins: { annotation: { annotations } }
        }
      });

      const ul = document.getElementById('listaEventos');
      ul.innerHTML = eventosExternos.map(ev=>`<li><strong>${ev.data} – ${ev.rotulo}:</strong> ${ev.comentario}</li>`).join('');
    }

    // ---- Scraping ----
    function buildScraping(){
      const tbody = document.getElementById('tbScraping');
      tbody.innerHTML = scrapingItens.map(x=>`
        <tr>
          <td>${x.nome}</td>
          <td>${x.categoria}</td>
          <td>${x.mercado}</td>
          <td class="text-end">${brl(x.preco)}</td>
          <td><a href="${x.url}" class="link-primary">link</a></td>
          <td>${x.coletadoEm}</td>
        </tr>
      `).join('');
    }
    document.getElementById('btnAtualizarScraping').addEventListener('click', ()=>{
      // simula leve variação e atualização de data
      const hoje = new Date();
      const dt = `${hoje.getFullYear()}-${String(hoje.getMonth()+1).padStart(2,'0')}-${String(hoje.getDate()).padStart(2,'0')}`;
      scrapingItens = scrapingItens.map(x=>({ ...x, preco: +(x.preco*(1 + (Math.random()-.5)*0.02)).toFixed(2), coletadoEm: dt }));
      buildScraping(); buildPeso(); // pesos poderiam reagir ao scraping (mock)
    });

    // Exportações
    document.getElementById('btnExportCsv').addEventListener('click', ()=>{
      const rows = [['mes','cesta']].concat(evolucaoHistorica.map(x=>[x.mes, x.cesta]));
      const csv = rows.map(r=>r.join(';')).join('\n');
      const blob = new Blob([csv], { type:'text/csv;charset=utf-8;' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a'); a.href=url; a.download='serie_cesta.csv'; a.click(); URL.revokeObjectURL(url);
    });
    document.getElementById('btnSnapshot').addEventListener('click', ()=>{
      const canvas = document.getElementById('chEvolucao');
      const a = document.createElement('a'); a.href=canvas.toDataURL('image/png'); a.download='dashboard.png'; a.click();
    });

    // Botões de simulação de eventos (impacto visual)
    document.getElementById('btnChoqueClima').addEventListener('click', ()=>{
      const idx = evolucaoHistorica.findIndex(x=>x.mes==='2025-08');
      if(idx>=0){
        for(let i=idx; i<Math.min(idx+2, evolucaoHistorica.length); i++){
          evolucaoHistorica[i].cesta = +(evolucaoHistorica[i].cesta * 1.03).toFixed(1); // +3% 2 meses
        }
        eventosExternos.push({ data: '2025-08', rotulo: 'Choque climático', comentario: 'Impacto simulado +3% por 2m' });
        buildEvolucao(); buildEventos();
      }
    });
    document.getElementById('btnChoqueCambio').addEventListener('click', ()=>{
      const idx = evolucaoHistorica.findIndex(x=>x.mes==='2025-05');
      if(idx>=0){
        for(let i=idx; i<Math.min(idx+3, evolucaoHistorica.length); i++){
          evolucaoHistorica[i].cesta = +(evolucaoHistorica[i].cesta * 1.02).toFixed(1); // +2% 3 meses
        }
        eventosExternos.push({ data: '2025-05', rotulo: 'Alta do dólar', comentario: 'Impacto simulado +2% por 3m' });
        buildEvolucao(); buildEventos();
      }
    });
    document.getElementById('btnKnime').addEventListener('click', ()=>{
      // mock: recalcula pesos a partir das médias por categoria (scraping)
      const medias = {};
      scrapingItens.forEach(x=>{ (medias[x.categoria]??=[]).push(x.preco); });
      const pares = Object.entries(medias).map(([k,arr])=>[k, arr.reduce((a,b)=>a+b,0)/arr.length]);
      const total = pares.reduce((a,[,v])=>a+v,0);
      pesoItens = pares.map(([k,v])=>({ name:k, value:+(v/total*100).toFixed(1) }));
      const soma = pesoItens.reduce((a,b)=>a+b.value,0);
      if(soma!==100 && pesoItens.length>0){ // normaliza
        const diff = +(100 - soma).toFixed(1);
        pesoItens[0].value = +(pesoItens[0].value + diff).toFixed(1);
      }
      buildPeso(); buildScraping();
    });

    // Inicialização
    renderMunicipios();
    buildEvolucao();
    buildRegional();
    buildInflacao();
    buildPeso();
    buildEventos();
    buildScraping();
  </script>
</body>
</html>
