<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="false" %>
<%@ taglib prefix="c"   uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"  %>
<%@ taglib prefix="fn"  uri="jakarta.tags.functions" %>

<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>Painel Analítico Cesta Básica</title>

  <!-- Bootstrap 5 (CSS + JS) -->
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" />
  <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>

  <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@24,400,0,0" />

  <!-- AnyChart -->
  <script src="https://cdn.anychart.com/releases/v8/js/anychart-bundle.min.js"></script>

  <link rel="stylesheet" href="<c:url value='/resources/css/navbar.css'/>" />
  <link rel="stylesheet" href="<c:url value='/resources/css/evolucao.css'/>" />
  <link rel="stylesheet" href="<c:url value='/resources/css/index.css'/>" />
</head>
<body id="topo" class="bg-light">
  <!-- Header -->
  <header class="bg-white border-bottom sticky-top shadow-sm">
    <nav class="navbar navbar-expand-lg navbar-light bg-white" aria-label="Navegação principal">
      <div class="container py-2 py-lg-3">
        <a class="navbar-brand fw-semibold" href="#topo">Painel Analítico Cesta Básica</a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#mainNav" aria-controls="mainNav" aria-expanded="false" aria-label="Alternar navegação">
          <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse justify-content-lg-end" id="mainNav">
          <ul class="navbar-nav align-items-lg-center gap-lg-2 ms-lg-4">
            <li class="nav-item"><a class="nav-link active" href="#tab-evolucao" data-tab-target="tab-evolucao">Evolução</a></li>
            <li class="nav-item"><a class="nav-link" href="#tab-regional" data-tab-target="tab-regional">Comparativo regional</a></li>
            <li class="nav-item"><a class="nav-link" href="#tab-peso" data-tab-target="tab-peso">Peso dos itens</a></li>
            <li class="nav-item"><a class="nav-link" href="#tab-eventos" data-tab-target="tab-eventos">Eventos externos</a></li>
            <li class="nav-item mt-2 mt-lg-0">
              <a class="btn btn-outline-primary w-100" href="<c:url value='/dados'/>">Dados</a>
            </li>
          </ul>
        </div>
      </div>
    </nav>
  </header>

  <main class="container my-4">

    <section class="mb-4" aria-label="Filtros globais do painel">
      <div class="card shadow-sm">
        <div class="card-body">
          <div class="d-flex align-items-center gap-3 mb-3">
            <div class="icon-circle icon-circle--accent">
              <span class="material-symbols-outlined">tune</span>
            </div>
            <div>
              <h2 class="h6 mb-0">Personalize a análise</h2>
              <small class="text-muted">Escolha municípios, período e modo de variação para sincronizar todo o painel.</small>
            </div>
          </div>

          <div class="row g-4">
            <div class="col-12 col-lg-5">
              <div class="filter-group h-100">
                <div class="filter-group__header">
                  <span class="filter-group__label">Municípios</span>
                  <small class="filter-group__helper">Escolha um ou mais municípios para comparar</small>
                </div>
                <div class="filter-group__body">
                  <div class="filter-scroll" role="presentation">
                    <div id="filtroDashboardMunicipiosPills" class="filter-pill-grid" role="listbox" aria-multiselectable="true" aria-label="Seleção de municípios"></div>
                  </div>
                  <select id="filtroDashboardMunicipios" class="filter-select-original" multiple size="6" aria-describedby="filtroDashboardMunicipiosFeedback"></select>
                </div>
                <div id="filtroDashboardMunicipiosFeedback" class="filter-group__feedback text-muted d-none">Nenhum município disponível para seleção.</div>
              </div>
            </div>
            <div class="col-12 col-lg-4">
              <div class="filter-group h-100">
                <div class="filter-group__header">
                  <span class="filter-group__label">Período</span>
                  <small class="filter-group__helper">Defina o intervalo da análise</small>
                </div>
                <div class="filter-group__body filter-group__body--period">
                  <div class="filter-column" role="group" aria-label="Período inicial">
                    <span class="filter-column__label">Início</span>
                    <div class="filter-scroll">
                      <div id="filtroDashboardPeriodoInicioPills" class="filter-pill-grid" role="listbox" aria-label="Seleção do período inicial"></div>
                    </div>
                    <select id="filtroDashboardPeriodoInicio" class="filter-select-original"></select>
                  </div>
                  <div class="filter-column" role="group" aria-label="Período final">
                    <span class="filter-column__label">Fim</span>
                    <div class="filter-scroll">
                      <div id="filtroDashboardPeriodoFimPills" class="filter-pill-grid" role="listbox" aria-label="Seleção do período final"></div>
                    </div>
                    <select id="filtroDashboardPeriodoFim" class="filter-select-original"></select>
                  </div>
                </div>
              </div>
            </div>
            <div class="col-12 col-lg-3">
              <div class="filter-group h-100">
                <div class="filter-group__header">
                  <span class="filter-group__label">Modo de variação</span>
                  <small class="filter-group__helper">Escolha como deseja acompanhar a mudança</small>
                </div>
                <div class="filter-group__body">
                  <div class="btn-group filter-variation-group w-100" role="group" aria-label="Modo de variação">
                    <input type="radio" class="btn-check" name="filtroDashboardVariacao" id="filtroDashboardVariacaoMensal" value="mensal" autocomplete="off" checked>
                    <label class="btn btn-outline-primary" for="filtroDashboardVariacaoMensal">Mensal</label>
                    <input type="radio" class="btn-check" name="filtroDashboardVariacao" id="filtroDashboardVariacaoAnual" value="anual" autocomplete="off">
                    <label class="btn btn-outline-primary" for="filtroDashboardVariacaoAnual">Anual</label>
                  </div>
                  <div class="d-grid mt-3">
                    <button id="filtroDashboardAplicar" type="button" class="btn btn-primary">Buscar</button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Seções principais -->

      <div class="functional-divider" aria-labelledby="divider-evolucao">
        <div class="functional-divider__line" aria-hidden="true"></div>
        <div class="functional-divider__content">
          <h2 id="divider-evolucao" class="functional-divider__title">Acompanhar a mudança dos preços</h2>
          <p class="functional-divider__subtitle">Nesta parte você vê, mês a mês, quanto custa a cesta básica em cada cidade e se o valor subiu ou desceu.</p>
        </div>
      </div>

      <!-- EVOLUÇÃO -->
      <section id="tab-evolucao" class="mb-5">
        <div class="mb-4 text-center text-lg-start">
          <h2 class="h4 mb-1">Evolução de preços • Monitor inteligente</h2>
          <p class="text-muted small mb-0">
            Mapa interativo, indicadores dinâmicos e histórico detalhado diretamente no painel principal.
          </p>
        </div>

        <div class="row g-4 align-items-start">
          <section class="col-12">
            <div id="indicadores" class="row g-3">
              <div class="col-md-4">
                <div class="kpi-card kpi-card--price shadow-sm">
                  <div class="kpi-card__label">Preço mais baixo</div>
                  <div id="kpiPrecoAtual" class="kpi-card__value">R$ --</div>
                  <div id="kpiPrecoDescricao" class="kpi-card__meta">—</div>
                </div>
              </div>
              <div class="col-md-4">
                <div class="kpi-card kpi-card--variation shadow-sm">
                  <div class="kpi-card__label mb-2">Variação</div>
                  <span id="kpiVariacaoTipo" class="badge rounded-pill text-bg-light text-uppercase small mb-2">Mensal</span>
                  <div id="kpiVariacao" class="kpi-card__value">0,0%</div>
                  <div id="kpiVariacaoDescricao" class="kpi-card__meta">—</div>
                </div>
              </div>
              <div class="col-md-4">
                <div class="kpi-card kpi-card--trend shadow-sm">
                  <div class="kpi-card__label">Tendência recente</div>
                  <div id="kpiTendencia" class="kpi-card__value">—</div>
                  <div id="kpiTendenciaDescricao" class="kpi-card__meta">Regressão linear do período selecionado</div>
                </div>
              </div>
            </div>

            <div id="sec-mapa" class="card shadow-sm mt-3">
              <div class="card-body p-4">
                <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4">
                  <div>
                    <h3 class="h6 mb-0">Mapa interativo de evolução de preços</h3>
                    <small class="text-muted">Passe o cursor ou clique sobre um município para ver detalhes. Utilize o botão ao lado para recentralizar a visão.</small>
                  </div>
                  <div class="d-flex flex-column flex-lg-row align-items-lg-center gap-2 gap-lg-3 text-lg-end">
                    <span id="variacaoLegendaTexto" class="badge rounded-pill text-bg-light text-uppercase small align-self-lg-start">Modo de variação: —</span>
                    <div class="d-flex align-items-center gap-3 legend-box flex-wrap justify-content-lg-end">
                      <div class="legend-item"><span class="legend-swatch legend-swatch--price"></span><span>Menor preço no período</span></div>
                      <div class="legend-item" data-variacao="mensal"><span class="legend-swatch legend-swatch--variation"></span><span>Variação mensal</span></div>
                      <div class="legend-item d-none" data-variacao="anual"><span class="legend-swatch legend-swatch--variation-anual"></span><span>Variação anual</span></div>
                    </div>
                    <button id="mapaRecentrar" class="btn btn-outline-primary btn-sm align-self-lg-stretch" type="button">Centralizar mapa</button>
                  </div>
                </div>

                <div class="row g-4">
                  <div class="col-xl-7">
                    <div id="mapaMunicipios" class="map-panel"></div>
                  </div>
                  <div class="col-xl-5">
                    <div class="map-side card border-0">
                      <div class="card-body">
                        <div class="d-flex align-items-start justify-content-between mb-3">
                          <div>
                            <h4 id="municipioAtivoTitulo" class="h6 mb-1">Selecione um município</h4>
                            <p id="municipioAtivoSubtitulo" class="text-muted small mb-0">Utilize o mapa ou os filtros laterais.</p>
                          </div>
                          <span id="municipioAtivoIcone" class="badge rounded-pill text-bg-secondary">—</span>
                        </div>
                        <dl class="row g-0 mb-3 info-grid">
                          <dt class="col-6 text-muted small">Preço atual</dt>
                          <dd id="municipioAtivoPreco" class="col-6 text-end fw-semibold">—</dd>
                          <dt class="col-6 text-muted small">Menor preço</dt>
                          <dd id="municipioAtivoMenorPreco" class="col-6 text-end fw-semibold">—</dd>
                          <dt class="col-6 text-muted small">Maior preço</dt>
                          <dd id="municipioAtivoMaiorPreco" class="col-6 text-end fw-semibold">—</dd>
                          <dt class="col-6 text-muted small" data-variacao="mensal">Variação mensal</dt>
                          <dd id="municipioAtivoVariacaoMensal" class="col-6 text-end fw-semibold" data-variacao="mensal">—</dd>
                          <dt class="col-6 text-muted small" data-variacao="anual">Variação anual</dt>
                          <dd id="municipioAtivoVariacaoAnual" class="col-6 text-end fw-semibold" data-variacao="anual">—</dd>
                          <dt class="col-6 text-muted small">Item mais caro</dt>
                          <dd id="municipioAtivoItemMaior" class="col-6 text-end fw-semibold">—</dd>
                          <dt class="col-6 text-muted small">Item mais barato</dt>
                          <dd id="municipioAtivoItemMenor" class="col-6 text-end fw-semibold">—</dd>
                          <dt class="col-6 text-muted small">Tendência</dt>
                          <dd class="col-6 text-end fw-semibold">
                            <span id="municipioAtivoTendencia">—</span>
                            <small id="municipioAtivoTendenciaDetalhe" class="d-block text-muted mt-1">—</small>
                          </dd>
                        </dl>
                        <p id="resumoLegendaVariacao" class="text-muted small mb-2">Resumo ajustado ao modo de variação selecionado.</p>
                        <div class="table-responsive small">
                          <table class="table table-sm align-middle mb-0">
                            <thead>
                              <tr>
                                <th>Município</th>
                                <th class="text-end">Preço</th>
                                <th class="text-end" data-col-variacao="mensal">Var. mensal</th>
                                <th class="text-end d-none" data-col-variacao="anual">Var. anual</th>
                                <th class="text-end" data-col-tendencia>Tendência</th>
                              </tr>
                            </thead>
                            <tbody id="resumoMunicipiosSelecionados"></tbody>
                          </table>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div id="sec-graficos" class="card shadow-sm mt-3">
              <div class="card-body p-4">
                <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-3">
                  <div>
                    <h3 class="h6 mb-0">Histórico de preços</h3>
                    <small class="text-muted">Gráfico de área moderno com comparação entre municípios selecionados. Linhas pontilhadas indicam a média do período filtrado.</small>
                  </div>
                  <span id="periodoSelecionado" class="badge rounded-pill text-bg-light">—</span>
                </div>
                <div class="chart-wrapper">
                  <div id="chartEvolucao"></div>
                </div>
              </div>
            </div>
          </section>
        </div>
      </section>

      <div class="functional-divider" aria-labelledby="divider-regional">
        <div class="functional-divider__line" aria-hidden="true"></div>
        <div class="functional-divider__content">
          <h2 id="divider-regional" class="functional-divider__title">Comparar cidades de forma fácil</h2>
          <p class="functional-divider__subtitle">Aqui você escolhe cidades para comparar. O painel mostra quem está com o preço mais barato e quem está mais caro agora.</p>
        </div>
      </div>

      <!-- COMPARATIVO REGIONAL -->
      <section id="tab-regional" class="mb-5">
        <div class="card shadow-sm mb-3">
          <div class="card-body">
            <div class="row g-3 align-items-end">
              <div class="col-lg-8">
                <p class="small text-muted mb-0">Utilize o filtro global para definir os municípios e o recorte temporal. A métrica abaixo ajusta os gráficos desta seção.</p>
              </div>
              <div class="col-lg-4">
                <label class="form-label small">Métrica</label>
                <select id="selMetricaRegional" class="form-select">
                  <option value="cesta">Custo da cesta (R$)</option>
                  <option value="var">Variação mensal (%)</option>
                </select>
              </div>
            </div>
          </div>
        </div>

        <div class="card shadow-sm mb-3">
          <div class="card-body">
            <div class="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-3">
              <div>
                <h3 class="h6 mb-0">Mapa interativo por município</h3>
                <small class="text-muted">Visualização com Leaflet e camadas OpenStreetMap.</small>
              </div>
              <button id="mapRegionalReset" class="btn btn-outline-primary btn-sm" type="button">Centralizar mapa</button>
            </div>
            <div id="mapMunicipios" class="map-wrap border rounded"></div>
            <p id="mapMunicipioInfo" class="small text-muted mt-2 mb-0">Clique em um município para ver o custo mais recente da cesta básica.</p>
          </div>
        </div>

        <div class="row g-3">
          <div class="col-xl-6">
            <div class="card shadow-sm">
              <div class="card-body">
                <h3 class="h6 mb-2">Evolução por município</h3>
                <div class="chart-wrap"><div id="chRegionalLinha"></div></div>
              </div>
            </div>
          </div>
          <div class="col-xl-6">
            <div class="card shadow-sm">
              <div class="card-body">
                <h3 class="h6 mb-2">Custo atual (último mês)</h3>
                <div class="chart-wrap"><div id="chRegionalBar"></div></div>
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
                    <th class="text-end">Tendência</th>
                  </tr>
                </thead>
                <tbody id="tbResumoRegional"></tbody>
              </table>
            </div>
          </div>
        </div>
      </section>

      <div class="functional-divider" aria-labelledby="divider-peso">
        <div class="functional-divider__line" aria-hidden="true"></div>
        <div class="functional-divider__content">
          <h2 id="divider-peso" class="functional-divider__title">Descobrir os itens que pesam no bolso</h2>
          <p class="functional-divider__subtitle">Aqui você descobre quais produtos da cesta pesam mais no seu bolso neste mês.</p>
        </div>
      </div>

      <!-- PESO DOS ITENS -->
      <section id="tab-peso" class="mb-5">
        <div class="row g-3">
          <div class="col-xl-6">
            <div class="card shadow-sm">
              <div class="card-body">
                <div class="d-flex justify-content-between align-items-center mb-2">
                  <h2 class="h6 mb-0">Peso dos itens na cesta</h2>
                  <span id="pesoPeriodoDescricao" class="small text-muted"></span>
                </div>
                <div class="chart-wrap"><div id="chPesoPizza"></div></div>
              </div>
            </div>
          </div>
          <div class="col-xl-6">
            <div class="card shadow-sm">
              <div class="card-body">
                <h2 class="h6 mb-2">Top itens do mês</h2>
                <div class="chart-wrap"><div id="chPesoBar"></div></div>
              </div>
            </div>
          </div>
        </div>
        <div class="card shadow-sm mt-3">
          <div class="card-body">
            <div class="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-3">
              <h3 class="h6 mb-0">Destaques por município</h3>
              <span id="pesoMunicipiosResumo" class="small text-muted"></span>
            </div>
            <div id="listaPesoMunicipios" class="row g-3"></div>
          </div>
        </div>
      </section>

      <div class="functional-divider" aria-labelledby="divider-eventos">
        <div class="functional-divider__line" aria-hidden="true"></div>
        <div class="functional-divider__content">
          <h2 id="divider-eventos" class="functional-divider__title">Ver como fatos externos mudam os preços</h2>
          <p class="functional-divider__subtitle">Aqui você simula acontecimentos, como clima ou dólar alto, para entender de jeito simples como isso pode mudar os preços.</p>
        </div>
      </div>

      <!-- EVENTOS EXTERNOS -->
      <section id="tab-eventos" class="mb-5">
        <div class="card shadow-sm">
          <div class="card-body">
            <div class="d-flex justify-content-between align-items-center mb-2">
              <h2 class="h6 mb-0">Linha do tempo com eventos externos</h2>
            </div>
            <div class="chart-wrap"><div id="chEventos"></div></div>
            <ul id="listaEventos" class="mt-3 small"></ul>
          </div>
        </div>
      </section>
  </main>

  <script>
    window.evolucaoPrefetch = <c:out value='${evolucaoPrefetchJson}' escapeXml='false'/>;
  </script>

  <script>
    window.evolucaoPeriodos = <c:out value='${periodosDisponiveisJson}' escapeXml='false'/>;
  </script>

  <script>
    (function(){
      var existing = window.googleMapsConfig || {};
      var config = Object.assign({ language: 'pt-BR', region: 'BR' }, existing);
      var jspApiKey = '<c:out value="${googleMapsApiKey}" default=""/>';
      if (typeof jspApiKey === 'string') {
        jspApiKey = jspApiKey.trim();
      }
      if (jspApiKey) {
        config.apiKey = jspApiKey;
      }
      window.googleMapsConfig = config;
    })();
  </script>

  <script>
    window.evolucaoConfig = {
      municipiosUrl: '<c:url value="/api/evolucao/municipios"/>',
      serieMunicipiosUrl: '<c:url value="/api/evolucao/series"/>',
      evolucaoMunicipiosUrl: '<c:url value="/api/evolucao/series"/>',
      indicadoresUrl: '<c:url value="/api/evolucao/indicadores"/>',
      periodosUrl: '<c:url value="/api/evolucao/periodos"/>'
    };
  </script>

  <script>
    window.dashboardConfig = {
      serieHistoricaUrl: '<c:url value="/api/cesta/serie-historica"/>',
      serieMunicipiosUrl: '<c:url value="/api/cesta/serie-municipios"/>',
      municipiosUrl: '<c:url value="/api/cesta/municipios"/>',
      eventosExternosUrl: '<c:url value="/api/eventos-externos"/>',
      pesoMunicipiosUrl: '<c:url value="/api/cesta/peso-municipios"/>'
    };
  </script>

  <script src="<c:url value='/resources/js/index.js'/>" defer></script>
</body>
</html>
