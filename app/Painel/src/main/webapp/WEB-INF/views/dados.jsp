<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>Dados DIEESE - Gastos Mensais</title>
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" />
  <link rel="stylesheet" href="<c:url value='/resources/css/navbar.css'/>" />
  <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
  <style>
    body { background-color: #f5f6f8; }
    section { scroll-margin-top: 6.5rem; }
    .table-nowrap th,
    .table-nowrap td { white-space: nowrap; }
    .import-overlay {
      position: fixed;
      inset: 0;
      background: rgba(15, 23, 42, 0.65);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1080;
    }
    .import-overlay.d-none { display: none !important; }
    .import-overlay .overlay-card { width: min(320px, 90%); }
    .badge-periodo { font-size: 0.75rem; }
    .periodo-faixa {
      display: flex;
      flex-wrap: wrap;
      gap: 0.35rem;
      padding-bottom: 0.25rem;
    }
    .periodo-faixa .badge { margin-bottom: 0.35rem; }
    .pagination { flex-wrap: wrap; gap: 0.35rem; }
    .pagination .page-item { flex: 0 0 auto; }
    .pagination .page-link {
      min-width: 2.25rem;
      text-align: center;
    }
    @media (max-width: 575.98px) {
      .pagination .page-link {
        padding: 0.35rem 0.5rem;
        font-size: 0.75rem;
      }
    }
    @media (max-width: 991.98px) {
      .table-nowrap th,
      .table-nowrap td { white-space: normal; }
    }

    .layout-wrapper {
      min-height: calc(100vh - 88px);
      padding-bottom: 2rem;
    }

    .municipios-box {
      max-height: 140px;
      overflow: auto;
      padding: 0.5rem 0.75rem;
      background-color: #f8fafc;
      border: 1px solid #e5e7eb;
      border-radius: 0.5rem;
    }

    .municipios-box ul {
      margin-bottom: 0;
    }

    .municipios-box li + li {
      margin-top: 0.25rem;
    }
  </style>
</head>
<body id="topo" class="bg-light">
  <header class="bg-white border-bottom sticky-top shadow-sm">
    <nav class="navbar navbar-expand-lg navbar-light bg-white" aria-label="Navegação principal">
      <div class="container-xl py-2 py-lg-3">
        <a class="navbar-brand fw-semibold" href="<c:url value='/dados'/>">Dados DIEESE</a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#dadosNav" aria-controls="dadosNav" aria-expanded="false" aria-label="Alternar navegação">
          <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse justify-content-lg-end" id="dadosNav">
          <ul class="navbar-nav align-items-lg-center gap-lg-2 ms-lg-4">
            <li class="nav-item"><a class="nav-link active" href="#resumo-importacao" data-scroll-target="#resumo-importacao">Resumo</a></li>
            <li class="nav-item"><a class="nav-link" href="#painel-periodos" data-scroll-target="#painel-periodos">Períodos monitorados</a></li>
            <li class="nav-item"><a class="nav-link" href="#sec-registros" data-scroll-target="#sec-registros">Registros importados</a></li>
            <li class="nav-item ms-lg-3 mt-2 mt-lg-0">
              <a class="btn btn-outline-primary w-100" href="<c:url value='/'/>">Painel principal</a>
            </li>
          </ul>
        </div>
      </div>
    </nav>
  </header>

  <div id="importOverlay" class="import-overlay d-none" aria-hidden="true">
    <div class="overlay-card bg-dark text-white rounded-4 p-4 shadow-lg text-center">
      <div class="spinner-border text-light mb-3" role="status" aria-hidden="true"></div>
      <div class="fw-semibold">Processando importação...</div>
      <div class="progress bg-dark-subtle mt-3" style="height: 0.75rem;">
        <div id="importProgressBar" class="progress-bar progress-bar-striped progress-bar-animated bg-primary" role="progressbar" aria-valuenow="0" aria-valuemin="0" aria-valuemax="100" style="width: 0%;">0%</div>
      </div>
    </div>
  </div>

  <main class="layout-wrapper">
      <div class="container-xl py-4 px-3 px-lg-4">
        <div class="mb-4">
          <h1 class="h5 mb-1">Gastos mensais - Pesquisa DIEESE</h1>
          <p class="text-muted small mb-0">Importe períodos, visualize registros e mantenha o histórico consolidado.</p>
        </div>

        <c:if test="${not empty erroImportacao}">
          <div class="alert alert-warning shadow-sm" role="alert">
            <c:out value="${erroImportacao}"/>
          </div>
        </c:if>

        <section id="resumo-importacao" class="mb-4">
          <c:if test="${not empty resultadoImportacao}">
            <div class="card shadow-sm">
              <div class="card-body">
                <h2 class="h6 mb-3">Resumo da importação</h2>
                <div class="row g-3">
                  <div class="col-md-4 col-12">
                    <div class="border rounded-3 p-3 bg-light">
                      <div class="text-muted small text-uppercase fw-semibold">Processados</div>
                      <div class="fs-4 fw-semibold">${resultadoImportacao.totalProcessados}</div>
                    </div>
                  </div>
                  <div class="col-md-4 col-12">
                    <div class="border rounded-3 p-3 bg-light">
                      <div class="text-muted small text-uppercase fw-semibold">Novos registros</div>
                      <div class="fs-4 fw-semibold">${resultadoImportacao.totalInseridos}</div>
                    </div>
                  </div>
                  <div class="col-md-4 col-12">
                    <div class="border rounded-3 p-3 bg-light">
                      <div class="text-muted small text-uppercase fw-semibold">Atualizados</div>
                      <div class="fs-4 fw-semibold">${resultadoImportacao.totalAtualizados}</div>
                    </div>
                  </div>
                </div>

                <c:if test="${not empty resultadoImportacao.periodosSucesso}">
                  <hr class="my-3" />
                  <div class="fw-semibold small mb-2">Períodos importados</div>
                  <div class="d-flex flex-wrap gap-1">
                    <c:forEach items="${resultadoImportacao.periodosSucesso}" var="periodo">
                      <span class="badge bg-success-subtle text-success badge-periodo">${periodo}</span>
                    </c:forEach>
                  </div>
                </c:if>

                <c:if test="${not empty resultadoImportacao.errosPorPeriodo}">
                  <hr class="my-3" />
                  <div class="fw-semibold small mb-2">Falhas registradas</div>
                  <ul class="small mb-0 ps-3">
                    <c:forEach items="${resultadoImportacao.errosPorPeriodo}" var="err">
                      <li><strong>${err.key}</strong>: <c:out value="${err.value}"/></li>
                    </c:forEach>
                  </ul>
                </c:if>
              </div>
            </div>
          </c:if>
        </section>

        <section id="sec-importacao" class="mb-4">
          <div class="card shadow-sm">
            <div class="card-body">
              <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
                <div>
                  <h2 class="h6 mb-1">Importar períodos</h2>
                  <p class="text-muted small mb-0">Defina a faixa desejada para atualizar os registros monitorados.</p>
                </div>
              </div>
              <form class="row g-3 align-items-end" method="post" action="<c:url value='/dados/importar'/>" id="formImportacao">
                <div class="col-12 col-md-4">
                  <label for="dataInicial" class="form-label">Data inicial (mmaaaa)</label>
                  <input type="text" id="dataInicial" name="dataInicial" class="form-control" maxlength="7" pattern="[0-9/]{6,7}" placeholder="mmAAAA" required>
                </div>
                <div class="col-12 col-md-4">
                  <label for="dataFinal" class="form-label">Data final (mmaaaa)</label>
                  <input type="text" id="dataFinal" name="dataFinal" class="form-control" maxlength="7" pattern="[0-9/]{6,7}" placeholder="mmAAAA" required>
                </div>
                <div class="col-12 col-md-4 d-flex align-items-end">
                  <div class="form-check mb-0">
                    <input class="form-check-input" type="checkbox" value="true" id="chkSequencial" name="sequencial">
                    <label class="form-check-label" for="chkSequencial">
                      Executar em modo sequencial (sem threads)
                    </label>
                  </div>
                </div>
                <div class="col-12 d-grid d-md-flex justify-content-md-end gap-2">
                  <button type="submit" class="btn btn-primary">Executar importação</button>
                </div>
              </form>
            </div>
          </div>
        </section>

        <section id="painel-periodos" class="mb-4">
          <c:if test="${not empty linhaPeriodos}">
            <div class="card shadow-sm">
              <div class="card-body">
                <div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2">
                  <h2 class="h6 mb-0">Períodos monitorados (01/2000 - 12/2025)</h2>
                  <div class="small text-muted d-flex align-items-center gap-3">
                    <span><span class="badge bg-primary text-white me-1">&nbsp;</span> Importado</span>
                    <span><span class="badge bg-secondary-subtle text-secondary me-1">&nbsp;</span> Não importado</span>
                  </div>
                </div>
                <div class="periodo-faixa">
                  <c:forEach items="${linhaPeriodos}" var="periodo">
                    <c:choose>
                      <c:when test="${periodo.importado}">
                        <span class="badge bg-primary text-white badge-periodo">${periodo.periodo}</span>
                      </c:when>
                      <c:otherwise>
                        <span class="badge bg-secondary-subtle text-secondary badge-periodo">${periodo.periodo}</span>
                      </c:otherwise>
                    </c:choose>
                  </c:forEach>
                </div>
              </div>
            </div>
          </c:if>
        </section>

        <section id="eventos-externos" class="mb-4">
          <div class="card shadow-sm">
            <div class="card-body">
              <div class="d-flex flex-wrap justify-content-between align-items-center mb-3 gap-2">
                <div>
                  <h2 class="h6 mb-1">Eventos externos monitorados</h2>
                  <p class="text-muted small mb-0">Cadastre eventos que impactam o custo da cesta básica e acompanhe seus efeitos.</p>
                </div>
                <span class="badge text-bg-light">${fn:length(eventosExternos)} eventos</span>
              </div>

              <c:if test="${not empty eventoSucesso}">
                <div class="alert alert-success alert-dismissible fade show" role="alert">
                  <c:out value="${eventoSucesso}"/>
                  <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Fechar"></button>
                </div>
              </c:if>
              <c:if test="${not empty eventoErro}">
                <div class="alert alert-danger alert-dismissible fade show" role="alert">
                  <c:out value="${eventoErro}"/>
                  <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Fechar"></button>
                </div>
              </c:if>

              <div class="row g-4 align-items-start">
                <div class="col-12 col-lg-5">
                  <div class="border rounded-3 p-3 h-100 bg-light-subtle">
                    <h3 class="h6 mb-3">Novo evento</h3>
                    <form action="<c:url value='/dados/eventos'/>" method="post" class="row g-3" id="formNovoEvento">
                      <div class="col-12">
                        <label for="eventoTitulo" class="form-label">Título</label>
                        <input type="text" id="eventoTitulo" name="titulo" class="form-control" maxlength="150"
                               value="${fn:escapeXml(eventoForm.titulo)}" required />
                      </div>
                      <div class="col-12">
                        <label for="eventoMunicipios" class="form-label">Municípios impactados</label>
                        <select id="eventoMunicipios" name="municipios" class="form-select" multiple size="6" required>
                          <c:if test="${empty eventoForm.municipios}">
                            <option value="" disabled>Selecione um ou mais municípios...</option>
                          </c:if>
                          <c:forEach items="${municipiosEventos}" var="mun">
                            <c:set var="municipioSelecionado" value="false" />
                            <c:if test="${not empty eventoForm.municipios}">
                              <c:forEach items="${eventoForm.municipios}" var="munSel">
                                <c:if test="${munSel == mun}">
                                  <c:set var="municipioSelecionado" value="true" />
                                </c:if>
                              </c:forEach>
                            </c:if>
                            <option value="${fn:escapeXml(mun)}"<c:if test="${municipioSelecionado}"> selected</c:if>>${mun}</option>
                          </c:forEach>
                          <c:forEach items="${eventoForm.municipios}" var="munSel">
                            <c:if test="${not empty munSel}">
                              <c:set var="municipioDisponivel" value="false" />
                              <c:forEach items="${municipiosEventos}" var="munDisponivel">
                                <c:if test="${munDisponivel == munSel}">
                                  <c:set var="municipioDisponivel" value="true" />
                                </c:if>
                              </c:forEach>
                              <c:if test="${not municipioDisponivel}">
                                <option value="${fn:escapeXml(munSel)}" selected>${munSel}</option>
                              </c:if>
                            </c:if>
                          </c:forEach>
                        </select>
                        <div class="form-text">Use Ctrl (ou Command no macOS) para selecionar múltiplos municípios.</div>
                      </div>
                      <div class="col-sm-6">
                        <label for="eventoImpacto" class="form-label">Impacto no custo</label>
                        <select id="eventoImpacto" name="impacto" class="form-select" required>
                          <option value="">Selecione...</option>
                          <option value="POSITIVO" ${eventoForm.impacto == 'POSITIVO' ? 'selected' : ''}>Impacto positivo</option>
                          <option value="NEGATIVO" ${(eventoForm.impacto == null or eventoForm.impacto == 'NEGATIVO') ? 'selected' : ''}>Impacto negativo</option>
                        </select>
                      </div>
                      <div class="col-sm-6">
                        <label for="eventoDataInicio" class="form-label">Data inicial</label>
                        <input type="date" id="eventoDataInicio" name="dataInicio" class="form-control"
                               value="${eventoForm.dataInicio}" required />
                      </div>
                      <div class="col-sm-6">
                        <label for="eventoDataFim" class="form-label">Data final</label>
                        <input type="date" id="eventoDataFim" name="dataFim" class="form-control"
                               value="${eventoForm.dataFim}" required />
                      </div>
                      <div class="col-12">
                        <label for="eventoDescricao" class="form-label">Descrição</label>
                        <textarea id="eventoDescricao" name="descricao" class="form-control" rows="3" maxlength="1000"
                                  placeholder="Impacto observado, fontes, observações...">${fn:escapeXml(eventoForm.descricao)}</textarea>
                      </div>
                      <div class="col-12 d-grid">
                        <button type="submit" class="btn btn-primary">Registrar evento</button>
                      </div>
                    </form>
                  </div>
                </div>
                <div class="col-12 col-lg-7">
                  <div class="table-responsive">
                    <table class="table table-sm align-middle mb-0">
                      <thead class="table-light">
                        <tr>
                          <th>Título</th>
                          <th>Municípios</th>
                          <th>Impacto</th>
                          <th>Período</th>
                          <th class="text-end">Ações</th>
                        </tr>
                      </thead>
                      <tbody>
                        <c:choose>
                          <c:when test="${empty eventosExternos}">
                            <tr>
                              <td colspan="5" class="text-center text-muted py-4">Nenhum evento cadastrado até o momento.</td>
                            </tr>
                          </c:when>
                          <c:otherwise>
                            <c:forEach items="${eventosExternos}" var="evento">
                              <c:set var="municipiosAttr" value="${not empty evento.municipiosConcatenados ? evento.municipiosConcatenados : ''}" />
                              <tr>
                                <td>
                                  <div class="fw-semibold">${evento.titulo}</div>
                                  <c:if test="${not empty evento.descricao}">
                                    <div class="small text-muted">${evento.descricao}</div>
                                  </c:if>
                                </td>
                                <td>
                                  <c:choose>
                                    <c:when test="${not empty evento.municipios}">
                                      <div class="municipios-box">
                                        <ul class="list-unstyled mb-0 small">
                                          <c:forEach items="${evento.municipios}" var="mun">
                                            <li class="d-flex align-items-start gap-1">
                                              <span class="text-primary-emphasis">•</span>
                                              <span class="text-break">${mun}</span>
                                            </li>
                                          </c:forEach>
                                        </ul>
                                      </div>
                                    </c:when>
                                    <c:otherwise>
                                      <span class="text-muted">—</span>
                                    </c:otherwise>
                                  </c:choose>
                                </td>
                                <td>
                                  <c:choose>
                                    <c:when test="${evento.impacto == 'POSITIVO'}">
                                      <span class="badge bg-success-subtle text-success">Impacto positivo</span>
                                    </c:when>
                                    <c:when test="${evento.impacto == 'NEGATIVO'}">
                                      <span class="badge bg-danger-subtle text-danger">Impacto negativo</span>
                                    </c:when>
                                    <c:otherwise>
                                      <span class="badge bg-secondary-subtle text-secondary">Não informado</span>
                                    </c:otherwise>
                                  </c:choose>
                                </td>
                                <td>
                                  <span class="badge text-bg-light">${evento.periodoInicio}</span>
                                  <c:if test="${not empty evento.periodoFim && evento.periodoFim != evento.periodoInicio}">
                                    <span class="material-symbols-outlined align-middle text-muted" style="font-size: 1rem;">trending_flat</span>
                                    <span class="badge text-bg-light">${evento.periodoFim}</span>
                                  </c:if>
                                </td>
                                <td class="text-end">
                                  <div class="btn-group btn-group-sm" role="group">
                                    <button type="button" class="btn btn-outline-secondary" data-bs-toggle="modal"
                                            data-bs-target="#modalEditarEvento"
                                            data-evento-id="${evento.id}"
                                            data-evento-titulo="${fn:escapeXml(evento.titulo)}"
                                            data-evento-descricao="${fn:escapeXml(evento.descricao)}"
                                            data-evento-inicio="${evento.dataInicio}"
                                            data-evento-fim="${evento.dataFim}"
                                            data-evento-municipios="${fn:escapeXml(municipiosAttr)}"
                                            data-evento-impacto="${evento.impacto}"
                                            data-evento-url="<c:url value='/dados/eventos/${evento.id}/atualizar'/>">
                                      Editar
                                    </button>
                                    <form action="<c:url value='/dados/eventos/${evento.id}/excluir'/>" method="post"
                                          onsubmit="return confirm('Confirma a remoção deste evento?');">
                                      <button type="submit" class="btn btn-outline-danger">Excluir</button>
                                    </form>
                                  </div>
                                </td>
                              </tr>
                            </c:forEach>
                          </c:otherwise>
                        </c:choose>
                      </tbody>
                    </table>
                  </div>
                  <p class="d-lg-none text-muted small text-center mt-2 mb-0">
                    Deslize horizontalmente para ver todos os detalhes da tabela.
                  </p>
                </div>
              </div>
            </div>
          </div>
        </section>

        

        <section id="sec-registros" class="mb-4">
          <div class="card shadow-sm">
            <div class="card-body">
              <div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2">
                <h2 class="h6 mb-0">Registros importados</h2>
                <c:if test="${not empty dados and dados.totalElements > 0}">
                  <span class="badge text-bg-light">Total: ${dados.totalElements}</span>
                </c:if>
              </div>

              <form class="row g-3 align-items-end mb-4" method="get" action="<c:url value='/dados'/>">
                <div class="col-12 col-lg-4">
                  <label for="filtroMunicipio" class="form-label">Município</label>
                  <input type="text" id="filtroMunicipio" name="municipio" class="form-control" value="${fn:escapeXml(municipioFiltro)}" placeholder="Ex.: São Paulo" />
                </div>
                <div class="col-6 col-lg-2">
                  <label for="filtroPeriodo" class="form-label">Período (mm/aaaa)</label>
                  <input type="text" id="filtroPeriodo" name="periodo" class="form-control" value="${fn:escapeXml(periodoFiltro)}" placeholder="mm/aaaa" />
                </div>
                <div class="col-6 col-lg-2">
                  <label for="pageSize" class="form-label">Itens por página</label>
                  <select id="pageSize" name="size" class="form-select">
                    <c:forEach items="${tamanhosPermitidos}" var="opt">
                      <option value="${opt}" ${opt == size ? 'selected' : ''}>${opt}</option>
                    </c:forEach>
                  </select>
                </div>
                <div class="col-12 col-lg-4">
                  <label class="form-label visually-hidden" for="btnBuscar">Ações</label>
                  <div class="d-grid d-sm-flex gap-2 align-items-sm-end">
                    <button type="submit" class="btn btn-dark" id="btnBuscar">Buscar</button>
                    <a class="btn btn-outline-secondary" href="<c:url value='/dados'/>">Limpar</a>
                  </div>
                </div>
              </form>

              <c:choose>
                <c:when test="${empty dados or dados.totalElements == 0}">
                  <p class="text-muted mb-0">Nenhum registro encontrado para os filtros selecionados.</p>
                </c:when>
                <c:otherwise>
                  <div class="table-responsive">
                    <table class="table table-sm table-striped align-middle table-nowrap">
                      <thead class="table-light">
                        <tr>
                          <th>Município</th>
                          <th>Período</th>
                          <th class="text-end">Total</th>
                          <th class="text-end">Carne</th>
                          <th class="text-end">Leite</th>
                          <th class="text-end">Feijão</th>
                          <th class="text-end">Arroz</th>
                          <th class="text-end">Farinha</th>
                          <th class="text-end">Batata</th>
                          <th class="text-end">Tomate</th>
                          <th class="text-end">Pão</th>
                          <th class="text-end">Café</th>
                          <th class="text-end">Banana</th>
                          <th class="text-end">Açúcar</th>
                          <th class="text-end">Óleo</th>
                          <th class="text-end">Manteiga</th>
                        </tr>
                      </thead>
                      <tbody>
                        <c:forEach items="${dados.content}" var="item">
                          <tr>
                            <td><c:out value="${item.municipio}"/></td>
                            <td><c:out value="${item.mesAnoFormatado}"/></td>
                            <td class="text-end"><jsp:include page="/WEB-INF/views/fragments/numero.jsp"><jsp:param name="valor" value="${item.totalCesta}"/></jsp:include></td>
                            <td class="text-end"><jsp:include page="/WEB-INF/views/fragments/numero.jsp"><jsp:param name="valor" value="${item.carne}"/></jsp:include></td>
                            <td class="text-end"><jsp:include page="/WEB-INF/views/fragments/numero.jsp"><jsp:param name="valor" value="${item.leite}"/></jsp:include></td>
                            <td class="text-end"><jsp:include page="/WEB-INF/views/fragments/numero.jsp"><jsp:param name="valor" value="${item.feijao}"/></jsp:include></td>
                            <td class="text-end"><jsp:include page="/WEB-INF/views/fragments/numero.jsp"><jsp:param name="valor" value="${item.arroz}"/></jsp:include></td>
                            <td class="text-end"><jsp:include page="/WEB-INF/views/fragments/numero.jsp"><jsp:param name="valor" value="${item.farinha}"/></jsp:include></td>
                            <td class="text-end"><jsp:include page="/WEB-INF/views/fragments/numero.jsp"><jsp:param name="valor" value="${item.batata}"/></jsp:include></td>
                            <td class="text-end"><jsp:include page="/WEB-INF/views/fragments/numero.jsp"><jsp:param name="valor" value="${item.tomate}"/></jsp:include></td>
                            <td class="text-end"><jsp:include page="/WEB-INF/views/fragments/numero.jsp"><jsp:param name="valor" value="${item.pao}"/></jsp:include></td>
                            <td class="text-end"><jsp:include page="/WEB-INF/views/fragments/numero.jsp"><jsp:param name="valor" value="${item.cafe}"/></jsp:include></td>
                            <td class="text-end"><jsp:include page="/WEB-INF/views/fragments/numero.jsp"><jsp:param name="valor" value="${item.banana}"/></jsp:include></td>
                            <td class="text-end"><jsp:include page="/WEB-INF/views/fragments/numero.jsp"><jsp:param name="valor" value="${item.acucar}"/></jsp:include></td>
                            <td class="text-end"><jsp:include page="/WEB-INF/views/fragments/numero.jsp"><jsp:param name="valor" value="${item.oleo}"/></jsp:include></td>
                            <td class="text-end"><jsp:include page="/WEB-INF/views/fragments/numero.jsp"><jsp:param name="valor" value="${item.manteiga}"/></jsp:include></td>
                          </tr>
                        </c:forEach>
                      </tbody>
                    </table>
                  </div>
                  <p class="d-lg-none text-muted small text-center mt-2 mb-0">
                    Deslize horizontalmente para acompanhar todos os itens da cesta.
                  </p>

                  <c:set var="paginaAtual" value="${dados.number}" />
                  <c:set var="totalPaginas" value="${dados.totalPages}" />
                  <c:set var="paginaAnterior" value="${paginaAtual > 0 ? paginaAtual - 1 : 0}" />
                  <c:set var="paginaSeguinte" value="${paginaAtual + 1 < totalPaginas ? paginaAtual + 1 : totalPaginas - 1}" />
                  <nav class="mt-3" aria-label="Paginação">
                    <c:url var="primeiraUrl" value="/dados">
                      <c:if test="${not empty municipioFiltro}"><c:param name="municipio" value="${municipioFiltro}"/></c:if>
                      <c:if test="${not empty periodoFiltro}"><c:param name="periodo" value="${periodoFiltro}"/></c:if>
                      <c:param name="size" value="${size}"/>
                      <c:param name="page" value="0"/>
                    </c:url>
                    <c:url var="anteriorUrl" value="/dados">
                      <c:if test="${not empty municipioFiltro}"><c:param name="municipio" value="${municipioFiltro}"/></c:if>
                      <c:if test="${not empty periodoFiltro}"><c:param name="periodo" value="${periodoFiltro}"/></c:if>
                      <c:param name="size" value="${size}"/>
                      <c:param name="page" value="${paginaAnterior}"/>
                    </c:url>
                    <c:url var="proximaUrl" value="/dados">
                      <c:if test="${not empty municipioFiltro}"><c:param name="municipio" value="${municipioFiltro}"/></c:if>
                      <c:if test="${not empty periodoFiltro}"><c:param name="periodo" value="${periodoFiltro}"/></c:if>
                      <c:param name="size" value="${size}"/>
                      <c:param name="page" value="${paginaSeguinte}"/>
                    </c:url>
                    <c:url var="ultimaUrl" value="/dados">
                      <c:if test="${not empty municipioFiltro}"><c:param name="municipio" value="${municipioFiltro}"/></c:if>
                      <c:if test="${not empty periodoFiltro}"><c:param name="periodo" value="${periodoFiltro}"/></c:if>
                      <c:param name="size" value="${size}"/>
                      <c:param name="page" value="${totalPaginas - 1}"/>
                    </c:url>
                    <div class="d-flex flex-wrap align-items-center gap-2 small">
                      <a class="link-underline link-underline-opacity-0 ${dados.first ? 'text-muted pe-none' : 'link-primary'}" href="${dados.first ? '#' : primeiraUrl}" aria-label="Primeira página" aria-disabled="${dados.first}">&laquo;&laquo;</a>
                      <a class="link-underline link-underline-opacity-0 ${dados.first ? 'text-muted pe-none' : 'link-primary'}" href="${dados.first ? '#' : anteriorUrl}" aria-disabled="${dados.first}">Anterior</a>
                      <span>Página</span>
                      <form method="get" class="d-inline" id="formSelecaoPagina">
                        <input type="hidden" name="size" value="${size}"/>
                        <c:if test="${not empty municipioFiltro}">
                          <input type="hidden" name="municipio" value="${municipioFiltro}"/>
                        </c:if>
                        <c:if test="${not empty periodoFiltro}">
                          <input type="hidden" name="periodo" value="${periodoFiltro}"/>
                        </c:if>
                        <select class="form-select form-select-sm d-inline-block w-auto" name="page" aria-label="Selecionar página" onchange="this.form.submit()">
                          <c:forEach var="i" begin="0" end="${totalPaginas - 1}">
                            <option value="${i}" ${i == paginaAtual ? 'selected' : ''}>${i + 1}</option>
                          </c:forEach>
                        </select>
                      </form>
                      <span>de ${totalPaginas}</span>
                      <a class="link-underline link-underline-opacity-0 ${dados.last ? 'text-muted pe-none' : 'link-primary'}" href="${dados.last ? '#' : proximaUrl}" aria-disabled="${dados.last}">Próxima</a>
                      <a class="link-underline link-underline-opacity-0 ${dados.last ? 'text-muted pe-none' : 'link-primary'}" href="${dados.last ? '#' : ultimaUrl}" aria-label="Última página" aria-disabled="${dados.last}">&raquo;&raquo;</a>
                    </div>
                  </nav>
                </c:otherwise>
              </c:choose>
            </div>
          </div>
        </section>
      </div>
    </main>

  <div class="modal fade" id="modalEditarEvento" tabindex="-1" aria-labelledby="modalEditarEventoLabel" aria-hidden="true">
    <div class="modal-dialog modal-lg modal-dialog-centered">
      <div class="modal-content">
        <div class="modal-header">
          <h2 class="modal-title fs-6" id="modalEditarEventoLabel">Editar evento externo</h2>
          <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Fechar"></button>
        </div>
        <form id="formEditarEvento" method="post">
          <div class="modal-body">
            <div class="row g-3">
              <div class="col-12">
                <label for="editarTitulo" class="form-label">Título</label>
                <input type="text" id="editarTitulo" name="titulo" class="form-control" maxlength="150" required />
              </div>
              <div class="col-12">
                <label for="editarMunicipios" class="form-label">Municípios impactados</label>
                <select id="editarMunicipios" name="municipios" class="form-select" multiple size="6" required>
                  <c:forEach items="${municipiosEventos}" var="mun">
                    <option value="${fn:escapeXml(mun)}">${mun}</option>
                  </c:forEach>
                </select>
                <div class="form-text">Selecione todos os municípios afetados antes de salvar.</div>
              </div>
              <div class="col-sm-6">
                <label for="editarImpacto" class="form-label">Impacto no custo</label>
                <select id="editarImpacto" name="impacto" class="form-select" required>
                  <option value="">Selecione...</option>
                  <option value="POSITIVO">Impacto positivo</option>
                  <option value="NEGATIVO">Impacto negativo</option>
                </select>
              </div>
              <div class="col-sm-6">
                <label for="editarDataInicio" class="form-label">Data inicial</label>
                <input type="date" id="editarDataInicio" name="dataInicio" class="form-control" required />
              </div>
              <div class="col-sm-6">
                <label for="editarDataFim" class="form-label">Data final</label>
                <input type="date" id="editarDataFim" name="dataFim" class="form-control" required />
              </div>
              <div class="col-12">
                <label for="editarDescricao" class="form-label">Descrição</label>
                <textarea id="editarDescricao" name="descricao" class="form-control" rows="3" maxlength="1000"></textarea>
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Cancelar</button>
            <button type="submit" class="btn btn-primary">Salvar alterações</button>
          </div>
        </form>
      </div>
    </div>
  </div>

  <script src="<c:url value='/resources/js/index.js'/>" defer></script>
  <script>
    (function () {
      const importForm = document.getElementById('formImportacao');
      const overlay = document.getElementById('importOverlay');
      const progressBar = document.getElementById('importProgressBar');
      let progressTimer = null;
      let currentValue = 0;

      if (!importForm || !overlay || !progressBar) {
        return;
      }

      function updateProgress(value) {
        const rounded = Math.min(100, Math.max(0, Math.round(value)));
        progressBar.style.width = rounded + '%';
        progressBar.textContent = rounded + '%';
        progressBar.setAttribute('aria-valuenow', String(rounded));
        currentValue = rounded;
      }

      function hideOverlay() {
        overlay.classList.add('d-none');
        overlay.setAttribute('aria-hidden', 'true');
        if (progressTimer) {
          window.clearInterval(progressTimer);
          progressTimer = null;
        }
        currentValue = 0;
        updateProgress(0);
      }

      importForm.addEventListener('submit', function () {
        if (!importForm.checkValidity()) {
          return;
        }
        overlay.classList.remove('d-none');
        overlay.setAttribute('aria-hidden', 'false');
        updateProgress(5);
        progressTimer = window.setInterval(function () {
          const nextValue = currentValue + Math.max(1, Math.round((100 - currentValue) * 0.08));
          updateProgress(Math.min(nextValue, 95));
        }, 400);
      });

      window.addEventListener('beforeunload', function () {
        if (!overlay.classList.contains('d-none')) {
          updateProgress(100);
        }
      });

      window.addEventListener('pageshow', function () {
        hideOverlay();
      });
    })();
  </script>
</body>
</html>
