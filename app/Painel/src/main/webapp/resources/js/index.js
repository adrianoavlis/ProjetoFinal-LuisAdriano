// =======================
// NAVEGAÇÃO / NAVBAR
// =======================
(function(){
  if (typeof bootstrap === 'undefined') {
    return;
  }

  document.querySelectorAll('.navbar').forEach(function(navbar){
    var collapseEl = navbar.querySelector('.navbar-collapse');
    if (!collapseEl) {
      return;
    }

    var navLinks = navbar.querySelectorAll('.navbar-nav .nav-link');
    if (!navLinks.length) {
      return;
    }

    var collapseInstance = null;
    function hideCollapse(){
      if (!collapseEl.classList.contains('show')) {
        return;
      }
      if (!collapseInstance) {
        collapseInstance = bootstrap.Collapse.getOrCreateInstance(collapseEl, { toggle: false });
      }
      collapseInstance.hide();
    }

    navLinks.forEach(function(link){
      link.addEventListener('click', function(evt){
        if (link.hasAttribute('data-bs-toggle')) {
          hideCollapse();
          return;
        }

        var handled = false;
        var targetTab = link.getAttribute('data-tab-target');
        if (targetTab) {
          evt.preventDefault();
          handled = true;
          var tabTrigger = document.querySelector('#tabs button[data-bs-target="#' + targetTab + '"]');
          if (tabTrigger) {
            var tab = bootstrap.Tab.getOrCreateInstance(tabTrigger);
            tab.show();
          }
          var tabSection = document.getElementById(targetTab);
          if (tabSection) {
            window.requestAnimationFrame(function(){
              tabSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
            });
          }
        }

        if (!handled) {
          var scrollTarget = link.getAttribute('data-scroll-target');
          if (!scrollTarget) {
            var href = link.getAttribute('href');
            if (href && href.startsWith('#')) {
              scrollTarget = href;
            }
          }
          if (scrollTarget && scrollTarget.startsWith('#')) {
            var section = document.querySelector(scrollTarget);
            if (section) {
              evt.preventDefault();
              handled = true;
              window.requestAnimationFrame(function(){
                section.scrollIntoView({ behavior: 'smooth', block: 'start' });
              });
            }
          }
        }

        if (handled) {
          navLinks.forEach(function(other){
            other.classList.toggle('active', other === link);
          });
          hideCollapse();
        }
      });
    });
  });
})();


// =======================
// FILTRO GLOBAL DO DASHBOARD
// =======================
(function () {
  const evolucaoConfig = window.evolucaoConfig || {};
  const MUNICIPIOS_ENDPOINT = evolucaoConfig.municipiosUrl || "";
  const PERIODOS_ENDPOINT = evolucaoConfig.periodosUrl || "";

  const PREFETCH = window.evolucaoPrefetch || {};
  const PREFETCH_SERIES = Array.isArray(PREFETCH.series) ? PREFETCH.series : [];
  const PREFETCH_MUNICIPIOS = Array.isArray(PREFETCH.municipios) ? PREFETCH.municipios : [];
  const PREFETCH_PERIODOS = window.evolucaoPeriodos || null;

  const DEFAULT_MUNICIPIOS = [
    "Rio de Janeiro / RJ",
    "São Paulo / SP",
    "Brasília / DF"
  ];
  const DEFAULT_PERIODO_INICIO = "2025-01";

  const elements = {};
  const listeners = new Set();
  const state = {
    municipios: [],
    periodo: {
      inicio: null,
      fim: null,
      anoReferencia: null
    },
    variacaoModo: "mensal"
  };

  const appliedState = {
    municipios: [],
    periodo: {
      inicio: null,
      fim: null,
      anoReferencia: null
    },
    variacaoModo: "mensal"
  };

  let municipiosDisponiveis = [];
  let periodosDisponiveis = [];
  let initialized = false;
  let lastTrigger = "init";

  const monthFormatterLong = new Intl.DateTimeFormat("pt-BR", {
    month: "long",
    year: "numeric",
    timeZone: "UTC"
  });
  const monthFormatterShort = new Intl.DateTimeFormat("pt-BR", {
    month: "short",
    year: "numeric",
    timeZone: "UTC"
  });

  function removerAcentos(texto) {
    return texto ? String(texto).normalize("NFD").replace(/[\u0300-\u036f]/g, "") : texto;
  }

  function normalizarMunicipioId(nome) {
    if (!nome) return null;
    const trimmed = String(nome).trim();
    if (!trimmed) return null;
    return removerAcentos(trimmed).toUpperCase();
  }

  function formatDisplayName(nome) {
    if (!nome) return "";
    const lower = String(nome).toLocaleLowerCase("pt-BR");
    return lower.replace(/(^|[\s-])(\p{L})/gu, (match, prefix, letter) => prefix + letter.toLocaleUpperCase("pt-BR"));
  }

  function extrairMunicipioInfo(valor) {
    if (valor && typeof valor === "object") {
      const base = valor.nome || valor.municipio || valor.label || valor.id || "";
      const info = extrairMunicipioInfo(base);
      if (info && !info.uf && valor.uf) {
        info.uf = String(valor.uf).toUpperCase();
        info.id = info.uf ? `${normalizarMunicipioId(info.nome)}-${info.uf}` : info.id;
      }
      return info;
    }

    if (!valor && valor !== 0) {
      return null;
    }

    let texto = String(valor).trim();
    if (!texto) {
      return null;
    }

    let nome = texto;
    let uf = "";

    const barraSplit = texto.split("/");
    if (barraSplit.length === 2 && barraSplit[1].trim().length === 2) {
      nome = barraSplit[0];
      uf = barraSplit[1].trim().toUpperCase();
    } else {
      const parenteseMatch = texto.match(/(.+?)\(([^)\s]{2})\)$/);
      if (parenteseMatch) {
        nome = parenteseMatch[1];
        uf = parenteseMatch[2].toUpperCase();
      } else {
        const hifenMatch = texto.match(/(.+?)[\s\-]+([A-Za-z]{2})$/);
        if (hifenMatch && hifenMatch[2]) {
          nome = hifenMatch[1];
          uf = hifenMatch[2].toUpperCase();
        }
      }
    }

    nome = nome.replace(/[\s\-]+$/, "").trim();
    const display = formatDisplayName(nome);
    const baseId = normalizarMunicipioId(nome);
    const id = uf ? `${baseId}-${uf}` : baseId;
    if (!id) {
      return null;
    }
    return { id, nome: display || nome, uf };
  }

  function normalizarMesAno(raw) {
    if (raw == null) return null;
    const texto = String(raw).trim();
    if (!texto) return null;
    const isoMatch = texto.match(/^(\d{4})[-\/]?(\d{1,2})$/);
    if (isoMatch) {
      const ano = Number(isoMatch[1]);
      const mes = Number(isoMatch[2]);
      if (!Number.isNaN(ano) && !Number.isNaN(mes) && mes >= 1 && mes <= 12) {
        return `${ano}-${String(mes).padStart(2, "0")}`;
      }
    }
    const partes = texto.split("-");
    if (partes.length === 2) {
      const ano = Number(partes[1]);
      if (Number.isNaN(ano)) return null;
      const chave = removerAcentos(partes[0] || "").toLowerCase();
      const meses = {
        jan: 1, janeiro: 1,
        fev: 2, fevereiro: 2,
        mar: 3, março: 3, marco: 3,
        abr: 4, abril: 4,
        mai: 5, maio: 5,
        jun: 6, junho: 6,
        jul: 7, julho: 7,
        ago: 8, agosto: 8,
        set: 9, setembro: 9,
        out: 10, outubro: 10,
        nov: 11, novembro: 11,
        dez: 12, dezembro: 12
      };
      const mes = meses[chave] || meses[chave.slice(0, 3)];
      if (mes) {
        return `${ano}-${String(mes).padStart(2, "0")}`;
      }
    }
    return null;
  }

  function parseMes(valor) {
    const iso = normalizarMesAno(valor);
    if (!iso) return null;
    const [anoStr, mesStr] = iso.split("-");
    const ano = Number(anoStr);
    const mes = Number(mesStr);
    if (!Number.isFinite(ano) || !Number.isFinite(mes)) {
      return null;
    }
    const data = new Date(Date.UTC(ano, mes - 1, 1));
    return {
      iso,
      ano,
      mes,
      key: ano * 100 + mes,
      label: monthFormatterShort.format(data),
      labelLongo: monthFormatterLong.format(data)
    };
  }

  function obterSnapshot() {
    return {
      municipios: [...appliedState.municipios],
      periodo: { ...appliedState.periodo },
      variacaoModo: appliedState.variacaoModo
    };
  }

  function dispatch(eventName, trigger = lastTrigger) {
    const snapshot = obterSnapshot();
    const detail = { ...snapshot, trigger };
    document.dispatchEvent(new CustomEvent(eventName, { detail }));
    if (eventName === "dashboardFiltro:change") {
      listeners.forEach((listener) => {
        try {
          listener(detail);
        } catch (err) {
          console.error("Erro ao notificar listener do filtro global", err);
        }
      });
    }
  }

  function notifyChange(trigger = lastTrigger) {
    if (!initialized) return;
    dispatch("dashboardFiltro:change", trigger);
  }

  function aplicarFiltros(options = {}) {
    const { silent = false, trigger = "user" } = options;
    lastTrigger = trigger || "user";
    appliedState.municipios = [...state.municipios];
    appliedState.periodo = { ...state.periodo };
    appliedState.variacaoModo = state.variacaoModo;
    if (!silent) {
      notifyChange(lastTrigger);
    }
  }

  function notifyReady(trigger = lastTrigger) {
    initialized = true;
    dispatch("dashboardFiltro:ready", trigger);
  }

  function setPillActive(pill, active) {
    if (!pill) return;
    pill.classList.toggle("is-active", Boolean(active));
    pill.setAttribute("aria-pressed", active ? "true" : "false");
  }

  function syncMunicipiosPills() {
    if (!elements.municipiosPills) return;
    const selecionados = new Set(state.municipios);
    elements.municipiosPills.querySelectorAll(".filter-pill").forEach((pill) => {
      setPillActive(pill, selecionados.has(pill.dataset.value));
    });
  }

  function syncMunicipiosSelect() {
    if (!elements.municipios) return;
    Array.from(elements.municipios.options).forEach((option) => {
      option.selected = state.municipios.includes(option.value);
    });
    if (elements.feedbackMunicipios) {
      const hasOptions = elements.municipios.options.length > 0;
      elements.feedbackMunicipios.classList.toggle("d-none", hasOptions);
    }
    syncMunicipiosPills();
  }

  function syncPeriodoPills() {
    if (elements.periodoInicioPills) {
      elements.periodoInicioPills.querySelectorAll(".filter-pill").forEach((pill) => {
        const ativo = state.periodo.inicio === pill.dataset.value;
        setPillActive(pill, ativo);
      });
    }
    if (elements.periodoFimPills) {
      elements.periodoFimPills.querySelectorAll(".filter-pill").forEach((pill) => {
        const ativo = state.periodo.fim === pill.dataset.value;
        setPillActive(pill, ativo);
      });
    }
  }

  function syncPeriodoSelects() {
    if (!elements.periodoInicio || !elements.periodoFim) return;
    elements.periodoInicio.value = state.periodo.inicio || "";
    elements.periodoFim.value = state.periodo.fim || "";
    syncPeriodoPills();
  }

  function ensurePeriodoValido() {
    if (!periodosDisponiveis.length) return;
    if (!state.periodo.inicio || !periodosDisponiveis.includes(state.periodo.inicio)) {
      state.periodo.inicio = periodosDisponiveis[0];
    }
    if (!state.periodo.fim || !periodosDisponiveis.includes(state.periodo.fim)) {
      state.periodo.fim = periodosDisponiveis[periodosDisponiveis.length - 1];
    }
    const inicioInfo = parseMes(state.periodo.inicio);
    const fimInfo = parseMes(state.periodo.fim);
    if (inicioInfo && fimInfo && inicioInfo.key > fimInfo.key) {
      state.periodo.fim = state.periodo.inicio;
    }
    const referencia = fimInfo?.ano ?? inicioInfo?.ano ?? null;
    state.periodo.anoReferencia = referencia;
    syncPeriodoSelects();
  }

  function atualizarMunicipiosSelecionados(novosIds) {
    const validos = novosIds.filter((id) => municipiosDisponiveis.some((m) => m.id === id));
    state.municipios = validos.length ? validos : (municipiosDisponiveis[0] ? [municipiosDisponiveis[0].id] : []);
    syncMunicipiosSelect();
  }

  function renderMunicipiosPills() {
    if (!elements.municipiosPills) return;
    elements.municipiosPills.innerHTML = "";
    municipiosDisponiveis.forEach((municipio) => {
      const button = document.createElement("button");
      button.type = "button";
      button.className = "filter-pill";
      button.dataset.value = municipio.id;
      button.dataset.role = "municipio";
      button.textContent = municipio.uf ? `${municipio.nome} (${municipio.uf})` : municipio.nome;
      button.setAttribute("aria-pressed", "false");
      button.setAttribute("aria-label", municipio.uf ? `${municipio.nome}, ${municipio.uf}` : municipio.nome);
      elements.municipiosPills.appendChild(button);
    });
  }

  function preencherMunicipios() {
    if (!elements.municipios) return;
    elements.municipios.innerHTML = "";
    municipiosDisponiveis.forEach((municipio) => {
      const option = document.createElement("option");
      option.value = municipio.id;
      const ufTexto = municipio.uf ? ` / ${municipio.uf}` : "";
      option.textContent = `${municipio.nome}${ufTexto}`;
      elements.municipios.appendChild(option);
    });
    renderMunicipiosPills();
    if (!municipiosDisponiveis.length && elements.feedbackMunicipios) {
      elements.feedbackMunicipios.classList.remove("d-none");
    }
    atualizarMunicipiosSelecionados(state.municipios.length ? state.municipios : []);
  }

  function renderPeriodoPills(tipo) {
    const container = tipo === "inicio" ? elements.periodoInicioPills : elements.periodoFimPills;
    if (!container) return;
    container.innerHTML = "";
    periodosDisponiveis.forEach((mes) => {
      const info = parseMes(mes);
      if (!info) return;
      const button = document.createElement("button");
      button.type = "button";
      button.className = "filter-pill";
      button.dataset.value = info.iso;
      button.dataset.role = tipo === "inicio" ? "periodo-inicio" : "periodo-fim";
      button.textContent = info.labelLongo;
      button.setAttribute("aria-pressed", "false");
      container.appendChild(button);
    });
  }

  function preencherPeriodos() {
    if (!elements.periodoInicio || !elements.periodoFim) return;
    elements.periodoInicio.innerHTML = "";
    elements.periodoFim.innerHTML = "";
    periodosDisponiveis.forEach((mes) => {
      const info = parseMes(mes);
      if (!info) return;
      const optionInicio = document.createElement("option");
      optionInicio.value = info.iso;
      optionInicio.textContent = info.labelLongo;
      elements.periodoInicio.appendChild(optionInicio);
      const optionFim = optionInicio.cloneNode(true);
      elements.periodoFim.appendChild(optionFim);
    });
    ensurePeriodoValido();
    renderPeriodoPills("inicio");
    renderPeriodoPills("fim");
    syncPeriodoPills();
  }

  function extrairListaMunicipios(payload) {
    if (!payload) return [];
    if (Array.isArray(payload)) return payload;
    if (payload && typeof payload === "object") {
      if (Array.isArray(payload.data)) return payload.data;
      if (Array.isArray(payload.content)) return payload.content;
      if (Array.isArray(payload.items)) return payload.items;
      if (Array.isArray(payload.results)) return payload.results;
    }
    return [];
  }

  async function carregarMunicipios() {
    const mapa = new Map();

    const registrar = (entrada) => {
      const info = extrairMunicipioInfo(entrada);
      if (!info || !info.id) return;
      if (!mapa.has(info.id)) {
        mapa.set(info.id, {
          id: info.id,
          nome: info.nome || formatDisplayName(info.id),
          uf: info.uf || ""
        });
      }
    };

    if (MUNICIPIOS_ENDPOINT) {
      try {
        const resp = await fetch(MUNICIPIOS_ENDPOINT, { headers: { Accept: "application/json" } });
        if (resp.ok) {
          const lista = extrairListaMunicipios(await resp.json());
          lista.forEach(registrar);
        }
      } catch (err) {
        console.warn("Falha ao carregar municípios do endpoint", err);
      }
    }

    if (!mapa.size) {
      PREFETCH_MUNICIPIOS.forEach(registrar);
    }

    if (!mapa.size) {
      PREFETCH_SERIES.forEach((entrada) => {
        if (!entrada) return;
        registrar(entrada.municipio || entrada.nome);
      });
    }

    municipiosDisponiveis = Array.from(mapa.values()).sort((a, b) =>
      a.nome.localeCompare(b.nome, "pt-BR", { sensitivity: "base" })
    );
  }

  function coletarPeriodosPrefetch(dados) {
    const mesesColetados = new Set();
    const adicionar = (valor) => {
      const iso = normalizarMesAno(valor);
      if (iso) mesesColetados.add(iso);
    };

    if (!dados) return mesesColetados;

    if (Array.isArray(dados.meses)) {
      dados.meses.forEach(adicionar);
    }
    if (dados.mesesPorAno && typeof dados.mesesPorAno === "object") {
      Object.values(dados.mesesPorAno).forEach((lista) => {
        if (Array.isArray(lista)) {
          lista.forEach(adicionar);
        }
      });
    }
    return mesesColetados;
  }

  function extrairListaPeriodos(payload) {
    const meses = new Set();
    const adicionar = (valor) => {
      const iso = normalizarMesAno(valor);
      if (iso) meses.add(iso);
    };

    if (!payload && payload !== 0) {
      return [];
    }

    if (Array.isArray(payload)) {
      payload.forEach(adicionar);
    } else if (typeof payload === "object") {
      if (Array.isArray(payload.meses)) {
        payload.meses.forEach(adicionar);
      }
      if (Array.isArray(payload.items)) {
        payload.items.forEach(adicionar);
      }
      if (Array.isArray(payload.content)) {
        payload.content.forEach(adicionar);
      }
      if (Array.isArray(payload.results)) {
        payload.results.forEach(adicionar);
      }
    }

    return Array.from(meses);
  }

  async function carregarPeriodos() {
    const coletados = new Set();

    if (PERIODOS_ENDPOINT) {
      try {
        const resp = await fetch(PERIODOS_ENDPOINT, { headers: { Accept: "application/json" } });
        if (resp.ok) {
          extrairListaPeriodos(await resp.json()).forEach((mes) => coletados.add(mes));
        }
      } catch (err) {
        console.warn("Falha ao carregar períodos do endpoint", err);
      }
    }

    if (!coletados.size) {
      coletarPeriodosPrefetch(PREFETCH_PERIODOS).forEach((mes) => coletados.add(mes));
    }

    PREFETCH_SERIES.forEach((entrada) => {
      if (!entrada || !Array.isArray(entrada.serie)) return;
      entrada.serie.forEach((ponto) => {
        if (!ponto) return;
        const iso = normalizarMesAno(ponto.mes);
        if (iso) coletados.add(iso);
      });
    });

    periodosDisponiveis = Array.from(coletados)
      .map((mes) => parseMes(mes))
      .filter(Boolean)
      .sort((a, b) => a.key - b.key)
      .map((info) => info.iso);
  }

  function handleMunicipiosChange() {
    if (!elements.municipios) return;
    const selecionados = Array.from(elements.municipios.selectedOptions).map((option) => option.value);
    atualizarMunicipiosSelecionados(selecionados);
  }

  function handleMunicipiosPillClick(event) {
    const alvo = event.target.closest(".filter-pill[data-role='municipio']");
    if (!alvo) return;
    event.preventDefault();
    const valor = alvo.dataset.value;
    if (!valor) return;
    const atuais = Array.from(state.municipios);
    const indice = atuais.indexOf(valor);
    if (indice >= 0) {
      atuais.splice(indice, 1);
    } else {
      atuais.push(valor);
    }
    atualizarMunicipiosSelecionados(atuais);
  }

  function handlePillKeydown(handler) {
    return (event) => {
      if (event.key !== " " && event.key !== "Enter" && event.key !== "Spacebar") {
        return;
      }
      event.preventDefault();
      handler(event);
    };
  }

  function handlePeriodoInicioChange() {
    if (!elements.periodoInicio) return;
    state.periodo.inicio = elements.periodoInicio.value || null;
    ensurePeriodoValido();
  }

  function handlePeriodoFimChange() {
    if (!elements.periodoFim) return;
    state.periodo.fim = elements.periodoFim.value || null;
    ensurePeriodoValido();
  }

  function handlePeriodoPillClick(event) {
    const alvo = event.target.closest(".filter-pill");
    if (!alvo) return;
    const { role: papel, value } = alvo.dataset;
    if (!papel || !value) return;
    event.preventDefault();
    if (papel === "periodo-inicio") {
      state.periodo.inicio = value;
    } else if (papel === "periodo-fim") {
      state.periodo.fim = value;
    } else {
      return;
    }
    ensurePeriodoValido();
  }

  function handleVariacaoChange(event) {
    if (!event || !event.target) return;
    const valor = event.target.value;
    if (!valor) return;
    state.variacaoModo = valor === "anual" ? "anual" : "mensal";
  }

  function handleAplicarClick(event) {
    if (event) {
      event.preventDefault();
    }
    aplicarFiltros({ trigger: "button" });
  }

  function attachListeners() {
    if (elements.municipios) {
      elements.municipios.addEventListener("change", handleMunicipiosChange);
    }
    if (elements.municipiosPills) {
      elements.municipiosPills.addEventListener("click", handleMunicipiosPillClick);
      elements.municipiosPills.addEventListener("keydown", handlePillKeydown(handleMunicipiosPillClick));
    }
    if (elements.periodoInicio) {
      elements.periodoInicio.addEventListener("change", handlePeriodoInicioChange);
    }
    if (elements.periodoFim) {
      elements.periodoFim.addEventListener("change", handlePeriodoFimChange);
    }
    if (elements.periodoInicioPills) {
      elements.periodoInicioPills.addEventListener("click", handlePeriodoPillClick);
      elements.periodoInicioPills.addEventListener("keydown", handlePillKeydown(handlePeriodoPillClick));
    }
    if (elements.periodoFimPills) {
      elements.periodoFimPills.addEventListener("click", handlePeriodoPillClick);
      elements.periodoFimPills.addEventListener("keydown", handlePillKeydown(handlePeriodoPillClick));
    }
    if (elements.variacaoMensal) {
      elements.variacaoMensal.addEventListener("change", handleVariacaoChange);
    }
    if (elements.variacaoAnual) {
      elements.variacaoAnual.addEventListener("change", handleVariacaoChange);
    }
    if (elements.aplicar) {
      elements.aplicar.addEventListener("click", handleAplicarClick);
    }
  }

  function initElements() {
    elements.municipios = document.getElementById("filtroDashboardMunicipios");
    elements.feedbackMunicipios = document.getElementById("filtroDashboardMunicipiosFeedback");
    elements.municipiosPills = document.getElementById("filtroDashboardMunicipiosPills");
    elements.periodoInicio = document.getElementById("filtroDashboardPeriodoInicio");
    elements.periodoFim = document.getElementById("filtroDashboardPeriodoFim");
    elements.periodoInicioPills = document.getElementById("filtroDashboardPeriodoInicioPills");
    elements.periodoFimPills = document.getElementById("filtroDashboardPeriodoFimPills");
    elements.variacaoMensal = document.getElementById("filtroDashboardVariacaoMensal");
    elements.variacaoAnual = document.getElementById("filtroDashboardVariacaoAnual");
    elements.aplicar = document.getElementById("filtroDashboardAplicar");
  }

  function definirEstadoInicial() {
    const idsDesejados = DEFAULT_MUNICIPIOS
      .map((descricao) => {
        const info = extrairMunicipioInfo(descricao);
        return info?.id || null;
      })
      .filter(Boolean);

    const disponiveis = new Set(municipiosDisponiveis.map((municipio) => municipio.id));
    const selecionadosPreferenciais = idsDesejados.filter((id) => disponiveis.has(id));
    const fallback = municipiosDisponiveis.slice(0, 3).map((item) => item.id);

    atualizarMunicipiosSelecionados(
      selecionadosPreferenciais.length ? selecionadosPreferenciais : fallback
    );

    const inicioPreferidoIso = normalizarMesAno(DEFAULT_PERIODO_INICIO);
    let inicioSelecionado = null;
    if (inicioPreferidoIso) {
      if (periodosDisponiveis.includes(inicioPreferidoIso)) {
        inicioSelecionado = inicioPreferidoIso;
      } else {
        const preferidoInfo = parseMes(inicioPreferidoIso);
        if (preferidoInfo) {
          const candidato = periodosDisponiveis
            .map((mes) => parseMes(mes))
            .filter(Boolean)
            .find((info) => info.key >= preferidoInfo.key);
          if (candidato) {
            inicioSelecionado = candidato.iso;
          }
        }
      }
    }
    if (!inicioSelecionado && periodosDisponiveis.length) {
      inicioSelecionado = periodosDisponiveis[0];
    }
    state.periodo.inicio = inicioSelecionado;
    state.periodo.fim = periodosDisponiveis.length
      ? periodosDisponiveis[periodosDisponiveis.length - 1]
      : null;

    ensurePeriodoValido();
    if (elements.variacaoMensal) {
      elements.variacaoMensal.checked = state.variacaoModo === "mensal";
    }
    if (elements.variacaoAnual) {
      elements.variacaoAnual.checked = state.variacaoModo === "anual";
    }
  }

  async function init() {
    initElements();
    if (!elements.municipios) {
      return;
    }

    await Promise.all([carregarMunicipios(), carregarPeriodos()]);

    preencherMunicipios();
    preencherPeriodos();
    definirEstadoInicial();
    attachListeners();
    const initTrigger = "init";
    aplicarFiltros({ silent: true, trigger: initTrigger });
    notifyReady(initTrigger);
  }

  function subscribe(callback) {
    if (typeof callback !== "function") {
      return () => {};
    }
    listeners.add(callback);
    return () => listeners.delete(callback);
  }

  const api = {
    subscribe,
    getState: obterSnapshot,
    apply: (options) => {
      if (typeof options === "string") {
        return aplicarFiltros({ trigger: options });
      }
      return aplicarFiltros(options || {});
    }
  };

  window.DashboardFilter = Object.assign(window.DashboardFilter || {}, api);

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();


// =======================
// EVOLUÇÃO E MAPA
// =======================
(function () {

  const evolucaoConfig = window.evolucaoConfig || {};
  const SERIE_MUNICIPIOS_ENDPOINT = evolucaoConfig.serieMunicipiosUrl || "";
  const EVOLUCAO_DATASET_ENDPOINT = evolucaoConfig.evolucaoMunicipiosUrl || SERIE_MUNICIPIOS_ENDPOINT;
  const INDICADORES_ENDPOINT = evolucaoConfig.indicadoresUrl || "";
  const PERIODOS_ENDPOINT = evolucaoConfig.periodosUrl || "";

  const PREFETCH = window.evolucaoPrefetch || {};
  const PREFETCH_SERIES = Array.isArray(PREFETCH.series) ? PREFETCH.series : [];
  const PREFETCH_MUNICIPIOS = Array.isArray(PREFETCH.municipios) ? PREFETCH.municipios : [];
  const PERIODOS_PREFETCH = window.evolucaoPeriodos || null;
  let MUNICIPIOS_DATA = [];
  let MESES_DISPONIVEIS = [];
  let ANOS_DISPONIVEIS = [];
  let MESES_POR_ANO = new Map();
  let INDICADORES = null;
  let indicadoresRequestToken = 0;

  const state = {
    selectedMunicipios: [],
    mesInicio: null,
    mesFim: null,
    anoReferencia: null,
    municipioAtivo: null,
    variacaoModo: "mensal"
  };

  let dadosProntos = false;
  let dadosCarregamentoPromise = null;
  let filtroPendente = null;

  const currencyFormatter = new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
    minimumFractionDigits: 2
  });

  const monthFormatter = new Intl.DateTimeFormat("pt-BR", {
    month: "short",
    year: "2-digit",
    timeZone: "UTC"
  });

  const longMonthFormatter = new Intl.DateTimeFormat("pt-BR", {
    month: "long",
    year: "numeric",
    timeZone: "UTC"
  });

  const MONTH_NAME_MAP = {
    jan: 1, january: 1, janeiro: 1,
    fev: 2, feb: 2, february: 2, fevereiro: 2,
    mar: 3, março: 3, marco: 3, march: 3,
    abr: 4, apr: 4, april: 4, abril: 4,
    mai: 5, may: 5, maio: 5,
    jun: 6, june: 6, junho: 6,
    jul: 7, july: 7, julho: 7,
    ago: 8, aug: 8, august: 8, agosto: 8,
    set: 9, sep: 9, sept: 9, setembro: 9, september: 9,
    out: 10, oct: 10, outubro: 10, october: 10,
    nov: 11, november: 11, novembro: 11,
    dez: 12, december: 12, dezembro: 12
  };

  const COMPONENTE_LABELS = {
    carne: "Carne",
    leite: "Leite",
    feijao: "Feijão",
    arroz: "Arroz",
    farinha: "Farinha",
    batata: "Batata",
    tomate: "Tomate",
    pao: "Pão",
    cafe: "Café",
    banana: "Banana",
    acucar: "Açúcar",
    oleo: "Óleo",
    manteiga: "Manteiga"
  };

  const GOOGLE_MAPS_CONFIG = window.googleMapsConfig || {};
  const GOOGLE_MAPS_API_OPTIONS = {
    key: GOOGLE_MAPS_CONFIG.apiKey || GOOGLE_MAPS_CONFIG.key || (window.dashboardConfig ? window.dashboardConfig.googleMapsApiKey : '') || '',
    language: GOOGLE_MAPS_CONFIG.language || 'pt-BR',
    region: GOOGLE_MAPS_CONFIG.region || 'BR',
    libraries: GOOGLE_MAPS_CONFIG.libraries || 'maps'
  };
  const GOOGLE_MAPS_API_BASE_URL = 'https://maps.googleapis.com/maps/api/js';
  const GOOGLE_MAPS_WAIT_INTERVAL = 120;
  const GOOGLE_MAPS_MAX_ATTEMPTS = 50;

  function createGoogleMapsLoader() {
    if (typeof window.loadGoogleMapsApi === 'function') {
      return window.loadGoogleMapsApi;
    }

    function loadGoogleMapsApi() {
      if (window.google && window.google.maps) {
        return Promise.resolve(window.google.maps);
      }
      if (window.__googleMapsReadyPromise && typeof window.__googleMapsReadyPromise.then === 'function') {
        return window.__googleMapsReadyPromise;
      }

      window.__googleMapsReadyPromise = new Promise((resolve, reject) => {
        if (window.google && window.google.maps) {
          resolve(window.google.maps);
          return;
        }

        const existingScript = Array.prototype.find.call(document.getElementsByTagName('script'), (script) => {
          return script && typeof script.src === 'string' && script.src.indexOf('maps.googleapis.com/maps/api/js') >= 0;
        });

        if (existingScript) {
          let attempts = 0;
          const waitForExisting = () => {
            if (window.google && window.google.maps) {
              resolve(window.google.maps);
              return;
            }
            attempts += 1;
            if (attempts >= GOOGLE_MAPS_MAX_ATTEMPTS) {
              reject(new Error('Google Maps não carregado.'));
              return;
            }
            setTimeout(waitForExisting, GOOGLE_MAPS_WAIT_INTERVAL);
          };
          waitForExisting();
          return;
        }

        const callbackName = '__initGoogleMapsApi';
        const params = new URLSearchParams();
        if (GOOGLE_MAPS_API_OPTIONS.key) {
          params.set('key', GOOGLE_MAPS_API_OPTIONS.key);
        } else {
          console.warn('Google Maps API key não configurada. Defina window.googleMapsConfig.apiKey ou dashboardConfig.googleMapsApiKey.');
        }
        if (GOOGLE_MAPS_API_OPTIONS.language) {
          params.set('language', GOOGLE_MAPS_API_OPTIONS.language);
        }
        if (GOOGLE_MAPS_API_OPTIONS.region) {
          params.set('region', GOOGLE_MAPS_API_OPTIONS.region);
        }
        if (GOOGLE_MAPS_API_OPTIONS.libraries) {
          params.set('libraries', GOOGLE_MAPS_API_OPTIONS.libraries);
        }
        params.set('callback', callbackName);

        const script = document.createElement('script');
        script.src = `${GOOGLE_MAPS_API_BASE_URL}?${params.toString()}`;
        script.async = true;
        script.defer = true;
        script.dataset.googleMapsLoader = 'true';
        script.onerror = () => {
          window.__googleMapsReadyPromise = null;
          reject(new Error('Falha ao carregar Google Maps JavaScript API.'));
        };

        window[callbackName] = () => {
          resolve(window.google.maps);
          window[callbackName] = null;
        };

        document.head.appendChild(script);
      }).catch((err) => {
        window.__googleMapsReadyPromise = null;
        throw err;
      });

      return window.__googleMapsReadyPromise;
    }

    window.loadGoogleMapsApi = loadGoogleMapsApi;
    return loadGoogleMapsApi;
  }

  const loadGoogleMapsApi = createGoogleMapsLoader();

  function removerAcentos(texto) {
    return texto ? texto.normalize("NFD").replace(/[\u0300-\u036f]/g, "") : texto;
  }

  function normalizarTextoBusca(texto) {
    if (texto == null) return "";
    return removerAcentos(String(texto)).toUpperCase();
  }

  function criarSearchIndex(info) {
    if (!info) return "";
    const campos = [info.nome, info.uf, info.id]
      .filter((parte) => parte != null && String(parte).trim() !== "")
      .map((parte) => String(parte));
    return normalizarTextoBusca(campos.join(" "));
  }

  function normalizarMunicipioId(nome) {
    if (!nome) return null;
    const trimmed = String(nome).trim();
    if (!trimmed) return null;
    return removerAcentos(trimmed).toUpperCase();
  }

  function formatDisplayName(nome) {
    if (!nome) return "";
    const lower = String(nome).toLocaleLowerCase("pt-BR");
    return lower.replace(/(^|[\s-])(\p{L})/gu, (match, prefix, letter) => prefix + letter.toLocaleUpperCase("pt-BR"));
  }

  function formatComponentLabel(chave) {
    if (!chave) return "";
    const normalizado = String(chave).toLowerCase();
    if (COMPONENTE_LABELS[normalizado]) {
      return COMPONENTE_LABELS[normalizado];
    }
    return formatDisplayName(chave);
  }

  function extrairMunicipioInfo(raw) {
    if (!raw && raw !== 0) {
      return { id: null, nome: "", uf: "" };
    }
    let texto = String(raw).trim();
    if (!texto) {
      return { id: null, nome: "", uf: "" };
    }
    let nome = texto;
    let uf = "";

    const barraSplit = texto.split("/");
    if (barraSplit.length === 2 && barraSplit[1].trim().length === 2) {
      nome = barraSplit[0];
      uf = barraSplit[1].trim().toUpperCase();
    } else {
      const parenteseMatch = texto.match(/(.+?)\(([^)\s]{2})\)$/);
      if (parenteseMatch) {
        nome = parenteseMatch[1];
        uf = parenteseMatch[2].toUpperCase();
      } else {
        const hifenMatch = texto.match(/(.+?)[\s\-]+([A-Za-z]{2})$/);
        if (hifenMatch && hifenMatch[2]) {
          nome = hifenMatch[1];
          uf = hifenMatch[2].toUpperCase();
        }
      }
    }

    nome = nome.replace(/[\s\-]+$/, "").trim();
    const display = formatDisplayName(nome);
    const baseId = normalizarMunicipioId(nome);
    const id = uf ? `${baseId}-${uf}` : baseId;
    return { id, nome: display || nome.trim(), uf };
  }

  const MUNICIPIOS_COORDENADAS = (() => {
    const base = [
      { nome: "Aracaju", uf: "SE", latitude: -10.9472, longitude: -37.0731 },
      { nome: "Belém", uf: "PA", latitude: -1.4558, longitude: -48.5039 },
      { nome: "Belo Horizonte", uf: "MG", latitude: -19.9167, longitude: -43.9345 },
      { nome: "Boa Vista", uf: "RR", latitude: 2.8235, longitude: -60.6758 },
      { nome: "Brasília", uf: "DF", latitude: -15.7801, longitude: -47.9292 },
      { nome: "Campo Grande", uf: "MS", latitude: -20.4697, longitude: -54.6201 },
      { nome: "Cuiabá", uf: "MT", latitude: -15.6010, longitude: -56.0974 },
      { nome: "Curitiba", uf: "PR", latitude: -25.4284, longitude: -49.2733 },
      { nome: "Florianópolis", uf: "SC", latitude: -27.5949, longitude: -48.5482 },
      { nome: "Fortaleza", uf: "CE", latitude: -3.7319, longitude: -38.5267 },
      { nome: "Goiânia", uf: "GO", latitude: -16.6869, longitude: -49.2648 },
      { nome: "João Pessoa", uf: "PB", latitude: -7.1150, longitude: -34.8631 },
      { nome: "Macapá", uf: "AP", latitude: 0.0349, longitude: -51.0694 },
      { nome: "Maceió", uf: "AL", latitude: -9.6498, longitude: -35.7089 },
      { nome: "Manaus", uf: "AM", latitude: -3.1190, longitude: -60.0217 },
      { nome: "Natal", uf: "RN", latitude: -5.7945, longitude: -35.2110 },
      { nome: "Palmas", uf: "TO", latitude: -10.2491, longitude: -48.3243 },
      { nome: "Porto Alegre", uf: "RS", latitude: -30.0319, longitude: -51.2065 },
      { nome: "Porto Velho", uf: "RO", latitude: -8.7608, longitude: -63.8999 },
      { nome: "Recife", uf: "PE", latitude: -8.0476, longitude: -34.8770 },
      { nome: "Rio Branco", uf: "AC", latitude: -9.9747, longitude: -67.8249 },
      { nome: "Rio de Janeiro", uf: "RJ", latitude: -22.9068, longitude: -43.1729 },
      { nome: "Salvador", uf: "BA", latitude: -12.9777, longitude: -38.5016 },
      { nome: "São Luís", uf: "MA", latitude: -2.5387, longitude: -44.2825 },
      { nome: "São Paulo", uf: "SP", latitude: -23.5505, longitude: -46.6333 },
      { nome: "Teresina", uf: "PI", latitude: -5.0919, longitude: -42.8034 },
      { nome: "Vitória", uf: "ES", latitude: -20.3155, longitude: -40.3128 },
      { nome: "Chapecó", uf: "SC", latitude: -27.1000, longitude: -52.6150 },
      { nome: "Itajaí", uf: "SC", latitude: -26.9101, longitude: -48.6700 },
      { nome: "Blumenau", uf: "SC", latitude: -26.9150, longitude: -49.0661 },
      { nome: "São José", uf: "SC", latitude: -27.6136, longitude: -48.6366 }
    ];
    const mapa = {};
    base.forEach((item) => {
      const baseKey = normalizarMunicipioId(item.nome);
      const asciiKey = removerAcentos(baseKey);
      const keys = new Set([baseKey, asciiKey]);
      if (item.uf) {
        const uf = item.uf.toUpperCase();
        keys.add(`${baseKey}-${uf}`);
        keys.add(`${asciiKey}-${uf}`);
      }
      keys.forEach((key) => {
        if (key && !mapa[key]) {
          mapa[key] = { latitude: item.latitude, longitude: item.longitude };
        }
      });
    });
    return mapa;
  })();

  function resolverCoordenada(info) {
    if (!info) return null;
    const candidatos = [];
    if (info.id) {
      candidatos.push(info.id);
    }
    const base = normalizarMunicipioId(info.nome);
    if (base) {
      candidatos.push(base);
      candidatos.push(removerAcentos(base));
    }
    if (info.uf) {
      const uf = info.uf.toUpperCase();
      if (info.id) candidatos.push(`${info.id}-${uf}`);
      if (base) candidatos.push(`${base}-${uf}`);
      if (base) candidatos.push(`${removerAcentos(base)}-${uf}`);
    }
    for (const key of candidatos) {
      if (key && MUNICIPIOS_COORDENADAS[key]) {
        return MUNICIPIOS_COORDENADAS[key];
      }
    }
    return null;
  }

  function normalizarMesAno(raw) {
    if (raw == null) return null;
    const texto = String(raw).trim();
    if (!texto) return null;

    const iso = texto.match(/^(\d{4})[-\/]?(\d{1,2})$/);
    if (iso) {
      const ano = Number(iso[1]);
      const mes = Number(iso[2]);
      if (Number.isFinite(ano) && Number.isFinite(mes) && mes >= 1 && mes <= 12) {
        return `${ano}-${String(mes).padStart(2, "0")}`;
      }
    }

    const compacto = texto.match(/^(\d{4})(\d{2})$/);
    if (compacto) {
      const ano = Number(compacto[1]);
      const mes = Number(compacto[2]);
      if (Number.isFinite(ano) && Number.isFinite(mes) && mes >= 1 && mes <= 12) {
        return `${ano}-${String(mes).padStart(2, "0")}`;
      }
    }

    const invertido = texto.match(/^(\d{1,2})[-\/]?(\d{4})$/);
    if (invertido) {
      const mes = Number(invertido[1]);
      const ano = Number(invertido[2]);
      if (Number.isFinite(ano) && Number.isFinite(mes) && mes >= 1 && mes <= 12) {
        return `${ano}-${String(mes).padStart(2, "0")}`;
      }
    }

    const partes = texto.split(/[\s-]+/);
    if (partes.length === 2) {
      const ano = Number(partes[1]);
      const chaveMes = removerAcentos(partes[0].toLowerCase());
      const mes = MONTH_NAME_MAP[chaveMes] || MONTH_NAME_MAP[chaveMes.slice(0, 3)];
      if (Number.isFinite(ano) && mes) {
        return `${ano}-${String(mes).padStart(2, "0")}`;
      }
    }

    return null;
  }

  const colorPalette = ["#2563eb", "#22c55e", "#f97316", "#8b5cf6", "#14b8a6", "#ec4899"];

  const elements = {};
  let evolucaoChart = null;
  let googleMapsApiInstance = null;
  let mapaGoogle = null;
  let mapaMarkers = new Map();
  let mapaOverlays = [];
  let mapaBoundsInitialized = false;
  let mapaInfoWindow = null;

  function formatCurrency(value) {
    if (!Number.isFinite(value)) return "—";
    return currencyFormatter.format(value);
  }

  function formatPercent(value, fractionDigits = 1) {
    if (!Number.isFinite(value)) return "—";
    const formatted = value.toFixed(fractionDigits);
    if (value > 0) {
      return `+${formatted}%`;
    }
    return `${formatted}%`;
  }

  function parseMes(mes) {
    const normalizado = normalizarMesAno(mes);
    if (!normalizado) return null;
    const [anoTexto, mesTexto] = normalizado.split("-");
    const ano = Number(anoTexto);
    const mesNumero = Number(mesTexto);
    if (!Number.isFinite(ano) || !Number.isFinite(mesNumero)) return null;
    const key = ano * 100 + mesNumero;
    const dataUtc = new Date(Date.UTC(ano, mesNumero - 1, 1));
    return {
      ano,
      mes: mesNumero,
      key,
      label: monthFormatter.format(dataUtc),
      labelLongo: longMonthFormatter.format(dataUtc),
      iso: normalizado
    };
  }

  function normalizarComponentes(raw) {
    if (!raw || typeof raw !== "object") {
      return null;
    }
    const entries = Object.entries(raw)
      .map(([chave, valor]) => {
        const numero = Number(valor);
        if (!Number.isFinite(numero)) {
          return null;
        }
        return [String(chave), Number(numero.toFixed(2))];
      })
      .filter(Boolean);
    if (!entries.length) {
      return null;
    }
    return Object.fromEntries(entries);
  }

  function prepararHistoricoSerie(serie) {
    if (!Array.isArray(serie)) return [];
    return serie
      .map((ponto) => {
        if (!ponto) return null;
        const iso = normalizarMesAno(ponto.mes ?? ponto.mesAno ?? null);
        const valor = ponto.preco ?? ponto.cesta ?? ponto.valor;
        const preco = Number(valor);
        if (!iso || !Number.isFinite(preco)) {
          return null;
        }
        const componentes = normalizarComponentes(ponto.componentes || ponto.itens || ponto.detalhes || null);
        return {
          mes: iso,
          preco: Number(preco.toFixed(2)),
          componentes: componentes || null
        };
      })
      .filter(Boolean)
      .sort((a, b) => {
        const infoA = parseMes(a.mes);
        const infoB = parseMes(b.mes);
        return (infoA?.key || 0) - (infoB?.key || 0);
      });
  }

  function prepararMunicipioBase(base) {
    if (!base) return null;
    const info = extrairMunicipioInfo(base.uf ? `${base.nome} / ${base.uf}` : base.nome);
    if (!info.id) return null;
    const historico = prepararHistoricoSerie(base.historico || []);
    if (!historico.length) return null;
    const coordsFallback = Number.isFinite(base.latitude) && Number.isFinite(base.longitude)
      ? { latitude: base.latitude, longitude: base.longitude }
      : null;
    const coords = resolverCoordenada(info) || coordsFallback;
    const municipio = {
      id: info.id,
      nome: info.nome || formatDisplayName(base.nome || info.id),
      uf: info.uf || (base.uf || ""),
      latitude: coords ? coords.latitude : null,
      longitude: coords ? coords.longitude : null,
      historico
    };
    municipio.searchIndex = criarSearchIndex(municipio);
    return municipio;
  }

  function construirMunicipioSerie(item) {
    if (!item) return null;
    const historico = prepararHistoricoSerie((item.serie || item.historico || []));
    if (!historico.length) return null;

    const baseRotulo = (() => {
      if (item.municipio) return item.municipio;
      if (item.nome && item.uf) return `${item.nome} / ${item.uf}`;
      if (item.nome) return item.nome;
      if (item.id && item.uf) return `${item.id} / ${item.uf}`;
      return item.id || "";
    })();

    const info = extrairMunicipioInfo(baseRotulo || item.nome || item.id || "");
    let id = normalizarMunicipioId(item.id) || info.id || normalizarMunicipioId(item.nome) || null;
    if (!id) return null;

    const uf = (item.uf || info.uf || "").toUpperCase();
    if (uf && !id.endsWith(`-${uf}`)) {
      id = `${id}-${uf}`;
    }

    const nome = formatDisplayName(item.nome || info.nome || item.municipio || id);
    const coords = resolverCoordenada({ id, nome, uf });

    const municipio = {
      id,
      nome,
      uf,
      latitude: coords ? coords.latitude : null,
      longitude: coords ? coords.longitude : null,
      historico
    };
    municipio.searchIndex = criarSearchIndex(municipio);
    return municipio;
  }

  function extrairEntradasDataset(payload) {
    if (Array.isArray(payload)) return payload;
    if (payload && typeof payload === "object") {
      if (Array.isArray(payload.data)) return payload.data;
      if (Array.isArray(payload.content)) return payload.content;
      if (Array.isArray(payload.items)) return payload.items;
      if (Array.isArray(payload.results)) return payload.results;
    }
    return [];
  }

  function aplicarPrefetchMunicipios(mapa) {
    if (!mapa) return;
    if (PREFETCH_SERIES.length) {
      const entradas = extrairEntradasDataset(PREFETCH_SERIES);
      entradas.forEach((entrada) => {
        const municipio = construirMunicipioSerie(entrada);
        if (municipio) {
          mapa.set(municipio.id, municipio);
        }
      });
    }

    if (PREFETCH_MUNICIPIOS.length) {
      PREFETCH_MUNICIPIOS.forEach((nome) => {
        const info = extrairMunicipioInfo(nome);
        if (!info || !info.id || mapa.has(info.id)) {
          return;
        }
        const coords = resolverCoordenada(info);
        mapa.set(info.id, {
          id: info.id,
          nome: info.nome || formatDisplayName(nome),
          uf: info.uf || "",
          latitude: coords ? coords.latitude : null,
          longitude: coords ? coords.longitude : null,
          historico: []
        });
      });
    }
  }

  function aplicarPeriodosPrefetch(periodos) {
    if (!periodos) return;

    const mesesColetados = new Set();
    const adicionarMes = (valor) => {
      const iso = normalizarMesAno(valor);
      if (iso) {
        mesesColetados.add(iso);
      }
    };

    if (Array.isArray(periodos.meses)) {
      periodos.meses.forEach(adicionarMes);
    }

    if (periodos.mesesPorAno && typeof periodos.mesesPorAno === "object") {
      Object.values(periodos.mesesPorAno).forEach((lista) => {
        if (Array.isArray(lista)) {
          lista.forEach(adicionarMes);
        }
      });
    }

    const infosOrdenados = Array.from(mesesColetados)
      .map((mes) => parseMes(mes))
      .filter(Boolean)
      .sort((a, b) => a.key - b.key);

    if (!infosOrdenados.length) {
      return;
    }

    MESES_DISPONIVEIS = infosOrdenados.map((info) => info.iso);
    ANOS_DISPONIVEIS = Array.from(new Set(infosOrdenados.map((info) => info.ano))).sort((a, b) => a - b);

    const mapa = new Map();
    if (periodos.mesesPorAno && typeof periodos.mesesPorAno === "object") {
      Object.entries(periodos.mesesPorAno).forEach(([anoTexto, lista]) => {
        const anoNumero = Number(anoTexto);
        if (!Number.isFinite(anoNumero)) return;
        if (!Array.isArray(lista)) {
          mapa.set(anoNumero, []);
          return;
        }
        const mesesAno = lista
          .map((item) => {
            const normalizado = normalizarMesAno(item);
            const info = parseMes(normalizado);
            return info ? info.iso : null;
          })
          .filter(Boolean)
          .sort((a, b) => {
            const infoA = parseMes(a);
            const infoB = parseMes(b);
            return (infoA?.key || 0) - (infoB?.key || 0);
          });
        mapa.set(anoNumero, Array.from(new Set(mesesAno)));
      });
    }

    if (!mapa.size) {
      ANOS_DISPONIVEIS.forEach((ano) => {
        const mesesAno = infosOrdenados
          .filter((info) => info.ano === Number(ano))
          .map((info) => info.iso);
        mapa.set(Number(ano), mesesAno);
      });
    }

    MESES_POR_ANO = mapa;
  }


  function possuiPeriodosCarregados() {
    if (MESES_DISPONIVEIS.length) {
      return true;
    }
    if (ANOS_DISPONIVEIS.length) {
      return true;
    }
    return MESES_POR_ANO instanceof Map && MESES_POR_ANO.size > 0;
  }

  async function carregarPeriodosRemotosSeNecessario() {
    if (possuiPeriodosCarregados()) {
      return;
    }
    if (!PERIODOS_ENDPOINT) {
      return;
    }
    try {
      const resp = await fetch(PERIODOS_ENDPOINT, {
        headers: { Accept: "application/json" }
      });
      if (!resp.ok) {
        return;
      }
      const corpo = await resp.json();
      aplicarPeriodosPrefetch(corpo);
    } catch (error) {
      console.error(`Falha ao carregar períodos disponíveis (${PERIODOS_ENDPOINT})`, error);
    }
  }


  function atualizarPeriodosDisponiveis() {
    const meses = new Set();
    (MESES_DISPONIVEIS || []).forEach((mes) => {
      const iso = normalizarMesAno(mes);
      if (iso) meses.add(iso);
    });
    MUNICIPIOS_DATA.forEach((municipio) => {
      (municipio.historico || []).forEach((ponto) => {
        const iso = normalizarMesAno(ponto.mes);
        if (iso) meses.add(iso);
      });
    });
    const infosOrdenados = Array.from(meses)
      .map((mes) => parseMes(mes))
      .filter(Boolean)
      .sort((a, b) => a.key - b.key);
    MESES_DISPONIVEIS = infosOrdenados.map((info) => info.iso);
    const anosSet = new Set(infosOrdenados.map((info) => info.ano));
    ANOS_DISPONIVEIS = Array.from(anosSet).sort((a, b) => a - b);
    const mapa = new Map();
    ANOS_DISPONIVEIS.forEach((ano) => {
      const mesesAno = infosOrdenados
        .filter((info) => info.ano === Number(ano))
        .map((info) => info.iso);
      mapa.set(Number(ano), mesesAno);
    });
    MESES_POR_ANO = mapa;
  }

  async function carregarDados() {
    const municipiosMap = new Map();

    aplicarPrefetchMunicipios(municipiosMap);

    const endpoints = [];
    if (EVOLUCAO_DATASET_ENDPOINT) {
      endpoints.push(EVOLUCAO_DATASET_ENDPOINT);
    }
    if (SERIE_MUNICIPIOS_ENDPOINT && SERIE_MUNICIPIOS_ENDPOINT !== EVOLUCAO_DATASET_ENDPOINT) {
      endpoints.push(SERIE_MUNICIPIOS_ENDPOINT);
    }

    for (const endpoint of endpoints) {
      try {
        const resp = await fetch(endpoint, {
          headers: { Accept: "application/json" }
        });
        if (resp.ok) {
          const corpo = await resp.json();
          const entradas = extrairEntradasDataset(corpo);
          entradas.forEach((entrada) => {
            const municipio = construirMunicipioSerie(entrada);
            if (municipio) {
              municipiosMap.set(municipio.id, municipio);
            }
          });
        }
      } catch (error) {
        console.error(`Falha ao carregar dados de evolução (${endpoint})`, error);
      }
      if (municipiosMap.size) {
        break;
      }
    }

    MUNICIPIOS_DATA = Array.from(municipiosMap.values()).sort((a, b) =>
      a.nome.localeCompare(b.nome, "pt-BR", { sensitivity: "base" })
    );
    atualizarPeriodosDisponiveis();
  }

  async function carregarIndicadores() {
    if (!dadosProntos) {
      return;
    }
    if (!INDICADORES_ENDPOINT) {
      INDICADORES = null;
      renderKpis();
      return;
    }

    const params = new URLSearchParams();
    if (Array.isArray(state.selectedMunicipios) && state.selectedMunicipios.length) {
      state.selectedMunicipios.forEach((id) => params.append("municipio", id));
    }
    if (state.mesInicio) {
      params.append("mesInicio", state.mesInicio);
    }
    if (state.mesFim) {
      params.append("mesFim", state.mesFim);
    }
    if (state.anoReferencia) {
      params.append("anoRef", state.anoReferencia);
    }

    const queryString = params.toString();
    const url = queryString ? `${INDICADORES_ENDPOINT}?${queryString}` : INDICADORES_ENDPOINT;
    const token = ++indicadoresRequestToken;

    try {
      const resp = await fetch(url, { headers: { Accept: "application/json" } });
      if (!resp.ok) {
        throw new Error(`Status ${resp.status}`);
      }
      const corpo = await resp.json();
      if (token !== indicadoresRequestToken) {
        return;
      }
      INDICADORES = corpo;
    } catch (error) {
      if (token !== indicadoresRequestToken) {
        return;
      }
      console.error("Falha ao carregar indicadores de evolução", error);
      INDICADORES = null;
    }

    renderKpis();
  }

  async function garantirDadosProntos() {
    if (dadosProntos) {
      return;
    }
    if (dadosCarregamentoPromise) {
      await dadosCarregamentoPromise;
      return;
    }

    dadosCarregamentoPromise = (async () => {
      await carregarPeriodosRemotosSeNecessario();
      await carregarDados();
      if (!MUNICIPIOS_DATA.length) {
        console.warn("Nenhum dado de evolução disponível.");
        return;
      }
      inicializarGrafico();
      inicializarMapa();
      dadosProntos = true;
    })();

    try {
      await dadosCarregamentoPromise;
    } finally {
      dadosCarregamentoPromise = null;
    }
  }

  function obterMunicipioPorId(id) {
    return MUNICIPIOS_DATA.find((item) => item.id === id) || null;
  }

  function obterMesesPorAno(ano) {
    if (!ano) return [];
    const anoNumero = Number(ano);
    if (!Number.isFinite(anoNumero)) return [];
    const meses = MESES_POR_ANO.get(anoNumero);
    if (Array.isArray(meses) && meses.length) {
      return [...meses];
    }
    return MESES_DISPONIVEIS.filter((mes) => {
      const info = parseMes(mes);
      return info ? info.ano === anoNumero : false;
    });
  }

  function filtrarHistoricoPorPeriodo(municipio, mesInicio, mesFim) {
    if (!municipio || !Array.isArray(municipio.historico)) return [];
    const inicio = parseMes(mesInicio);
    const fim = parseMes(mesFim);
    return municipio.historico
      .map((item) => {
        const info = parseMes(item.mes);
        if (!info) return null;
        return {
          ...item,
          ano: info.ano,
          mesNumero: info.mes,
          key: info.key,
          label: info.label,
          labelLongo: info.labelLongo
        };
      })
      .filter(Boolean)
      .filter((item) => {
        const depoisInicio = !inicio || item.key >= inicio.key;
        const antesFim = !fim || item.key <= fim.key;
        return depoisInicio && antesFim;
      })
      .sort((a, b) => a.key - b.key);
  }

  function encontrarExtremosPreco(hist) {
    return (hist || []).reduce(
      (acc, item) => {
        if (!item || !Number.isFinite(item.preco)) {
          return acc;
        }
        if (!acc.menor || item.preco < acc.menor.preco) {
          acc.menor = item;
        }
        if (!acc.maior || item.preco > acc.maior.preco) {
          acc.maior = item;
        }
        return acc;
      },
      { menor: null, maior: null }
    );
  }

  function obterResumoComponentes(hist) {
    if (!Array.isArray(hist) || !hist.length) {
      return { maior: null, menor: null, referencia: null };
    }
    const referencia = [...hist]
      .slice()
      .reverse()
      .find((item) => item?.componentes && Object.keys(item.componentes).length);
    if (!referencia) {
      return { maior: null, menor: null, referencia: null };
    }
    const entries = Object.entries(referencia.componentes)
      .map(([chave, valor]) => {
        const numero = Number(valor);
        if (!Number.isFinite(numero)) {
          return null;
        }
        return [chave, Number(numero.toFixed(2))];
      })
      .filter(Boolean)
      .sort((a, b) => b[1] - a[1]);

    if (!entries.length) {
      return { maior: null, menor: null, referencia: referencia };
    }

    const [maiorChave, maiorValor] = entries[0];
    const [menorChave, menorValor] = entries[entries.length - 1];
    return {
      referencia,
      maior: { chave: maiorChave, valor: maiorValor },
      menor: { chave: menorChave, valor: menorValor }
    };
  }

  function obterAnoReferenciaCalculo() {
    if (state.anoReferencia) {
      const ano = Number(state.anoReferencia);
      if (Number.isFinite(ano)) {
        return ano;
      }
    }
    const inicioInfo = parseMes(state.mesInicio);
    if (inicioInfo) {
      return inicioInfo.ano;
    }
    return null;
  }

  function calcularVariacaoMensal(hist) {
    const serie = (hist || []).filter((item) => Number.isFinite(item?.preco));
    if (serie.length < 2) return null;
    const ultimo = serie[serie.length - 1];
    const penultimo = serie[serie.length - 2];
    if (!Number.isFinite(ultimo.preco) || !Number.isFinite(penultimo.preco) || penultimo.preco === 0) {
      return null;
    }
    return ((ultimo.preco - penultimo.preco) / penultimo.preco) * 100;
  }

  function calcularVariacaoAnual(hist, anoReferencia) {
    const serieBase = (hist || []).filter((item) => Number.isFinite(item?.preco));
    if (serieBase.length < 2) return null;
    let serieAnual = serieBase;
    const ano = Number(anoReferencia);
    if (Number.isFinite(ano)) {
      const filtrada = serieBase.filter((item) => item.ano === ano);
      if (filtrada.length >= 2) {
        serieAnual = filtrada;
      }
    }
    const primeiro = serieAnual[0];
    const ultimo = serieAnual[serieAnual.length - 1];
    if (!Number.isFinite(ultimo.preco) || !Number.isFinite(primeiro.preco) || primeiro.preco === 0) {
      return null;
    }
    return ((ultimo.preco - primeiro.preco) / primeiro.preco) * 100;
  }

  function inferirTendencia(hist) {
    const dados = (hist || []).filter((item) => Number.isFinite(item?.preco));
    if (dados.length < 2) {
      return {
        status: "indefinido",
        label: "Sem dados",
        badge: "text-bg-secondary",
        slope: 0,
        percentSlope: null,
        descricao: "Histórico insuficiente para análise."
      };
    }

    const n = dados.length;
    const xs = dados.map((_, index) => index);
    const sumX = xs.reduce((acc, value) => acc + value, 0);
    const sumY = dados.reduce((acc, item) => acc + item.preco, 0);
    const sumXY = dados.reduce((acc, item, index) => acc + item.preco * xs[index], 0);
    const sumX2 = xs.reduce((acc, value) => acc + value * value, 0);
    const denominator = n * sumX2 - sumX * sumX;

    if (denominator === 0) {
      return {
        status: "estavel",
        label: "Estável",
        badge: "text-bg-warning",
        slope: 0,
        percentSlope: 0,
        descricao: "Variação mínima ao longo do período."
      };
    }

    const slope = (n * sumXY - sumX * sumY) / denominator;
    const media = sumY / n;
    const percentSlope = media ? (slope / media) * 100 : 0;

    const LIMIAR_ALTA = 0.35; // % por mês
    const LIMIAR_QUEDA = -0.35;

    if (percentSlope > LIMIAR_ALTA) {
      return {
        status: "alta",
        label: "Subindo",
        badge: "text-bg-success",
        slope,
        percentSlope,
        descricao: `Crescimento médio de ${formatPercent(percentSlope, 2)} por mês.`
      };
    }

    if (percentSlope < LIMIAR_QUEDA) {
      return {
        status: "queda",
        label: "Queda",
        badge: "text-bg-danger",
        slope,
        percentSlope,
        descricao: `Queda média de ${formatPercent(percentSlope, 2)} por mês.`
      };
    }

    return {
      status: "estavel",
      label: "Estável",
      badge: "text-bg-warning",
      slope,
      percentSlope,
      descricao: `Oscilação controlada (inclinação de ${formatPercent(percentSlope, 2)} ao mês).`
    };
  }

  function obterRotuloVariacao(modo) {
    switch (modo) {
      case "anual":
        return "Anual";
      default:
        return "Mensal";
    }
  }

  function obterDescricaoVariacao(modo) {
    switch (modo) {
      case "anual":
        return "variação anual";
      default:
        return "variação mensal";
    }
  }

  function obterElementos() {
    elements.kpiPrecoAtual = document.getElementById("kpiPrecoAtual");
    elements.kpiPrecoDescricao = document.getElementById("kpiPrecoDescricao");
    elements.kpiVariacao = document.getElementById("kpiVariacao");
    elements.kpiVariacaoDescricao = document.getElementById("kpiVariacaoDescricao");
    elements.kpiVariacaoTipo = document.getElementById("kpiVariacaoTipo");
    elements.kpiTendencia = document.getElementById("kpiTendencia");
    elements.kpiTendenciaDescricao = document.getElementById("kpiTendenciaDescricao");
    elements.periodoSelecionado = document.getElementById("periodoSelecionado");
    elements.resumoTabela = document.getElementById("resumoMunicipiosSelecionados");
    elements.municipioTitulo = document.getElementById("municipioAtivoTitulo");
    elements.municipioSubtitulo = document.getElementById("municipioAtivoSubtitulo");
    elements.municipioBadge = document.getElementById("municipioAtivoIcone");
    elements.municipioPreco = document.getElementById("municipioAtivoPreco");
    elements.municipioMenorPreco = document.getElementById("municipioAtivoMenorPreco");
    elements.municipioMaiorPreco = document.getElementById("municipioAtivoMaiorPreco");
    elements.municipioVariacaoMensal = document.getElementById("municipioAtivoVariacaoMensal");
    elements.municipioVariacaoAnual = document.getElementById("municipioAtivoVariacaoAnual");
    elements.municipioItemMaior = document.getElementById("municipioAtivoItemMaior");
    elements.municipioItemMenor = document.getElementById("municipioAtivoItemMenor");
    elements.municipioTendencia = document.getElementById("municipioAtivoTendencia");
    elements.municipioTendenciaDetalhe = document.getElementById("municipioAtivoTendenciaDetalhe");
    elements.variacaoMensalNodes = document.querySelectorAll('[data-variacao="mensal"]');
    elements.variacaoAnualNodes = document.querySelectorAll('[data-variacao="anual"]');
    elements.variacaoLegenda = document.getElementById("variacaoLegendaTexto");
    elements.resumoLegendaVariacao = document.getElementById("resumoLegendaVariacao");
    elements.resumoHeaderVariacao = document.querySelectorAll('[data-col-variacao]');
  }

  function garantirPeriodoValido() {
    const mesesDisponiveis = state.anoReferencia ? obterMesesPorAno(state.anoReferencia) : MESES_DISPONIVEIS;
    if (!mesesDisponiveis.length) {
      return;
    }

    if (state.anoReferencia) {
      if (!state.mesInicio) {
        state.mesInicio = mesesDisponiveis[0];
      }
      if (!state.mesFim) {
        state.mesFim = mesesDisponiveis[mesesDisponiveis.length - 1];
      }
    }

    if (state.mesInicio && !mesesDisponiveis.includes(state.mesInicio)) {
      state.mesInicio = mesesDisponiveis[0];
    }

    if (state.mesFim && !mesesDisponiveis.includes(state.mesFim)) {
      state.mesFim = mesesDisponiveis[mesesDisponiveis.length - 1];
    }

    if (state.mesInicio && state.mesFim) {
      const inicioInfo = parseMes(state.mesInicio);
      const fimInfo = parseMes(state.mesFim);
      if (inicioInfo && fimInfo && inicioInfo.key > fimInfo.key) {
        state.mesFim = state.mesInicio;
      }
    }
  }

  function filtrarSelecaoMunicipios(ids) {
    const existentes = ids.filter((id) => obterMunicipioPorId(id));
    return existentes;
  }

  function atualizarEstadoComFiltro(filtro) {
    if (!filtro) return;
    const idsFiltro = Array.isArray(filtro.municipios) ? filtro.municipios : [];
    let selecionados = filtrarSelecaoMunicipios(idsFiltro);
    if (!selecionados.length && MUNICIPIOS_DATA.length) {
      selecionados = [MUNICIPIOS_DATA[0].id];
    }
    state.selectedMunicipios = selecionados;
    if (!selecionados.includes(state.municipioAtivo)) {
      state.municipioAtivo = selecionados[0] || null;
    }

    const periodo = filtro.periodo || {};
    const inicio = normalizarMesAno(periodo.inicio);
    const fim = normalizarMesAno(periodo.fim);
    state.mesInicio = inicio || null;
    state.mesFim = fim || null;

    const fimInfo = parseMes(state.mesFim);
    const inicioInfo = parseMes(state.mesInicio);
    if (periodo.anoReferencia != null) {
      state.anoReferencia = periodo.anoReferencia;
    } else if (fimInfo?.ano != null) {
      state.anoReferencia = fimInfo.ano;
    } else if (inicioInfo?.ano != null) {
      state.anoReferencia = inicioInfo.ano;
    } else {
      state.anoReferencia = null;
    }

    state.variacaoModo = filtro.variacaoModo === "anual" ? "anual" : "mensal";
    garantirPeriodoValido();
  }

  let postBuscaTimeoutId = null;

  function obterElementoPorXPath(xpath) {
    try {
      return document.evaluate(xpath, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
    } catch (erro) {
      console.error("Falha ao buscar elemento pelo XPath", xpath, erro);
      return null;
    }
  }

  function agendarCliquesPosBusca() {
    if (postBuscaTimeoutId) {
      clearTimeout(postBuscaTimeoutId);
    }

    const xpaths = [
      "/html/body/main/section[3]/div[2]/div/div[2]/div[2]/table/tr/td[2]/button",
      "/html/body/main/section[2]/div[2]/section/div[2]/div/div[2]/div[1]/div/div[2]/table/tr/td[2]/button"
    ];

    postBuscaTimeoutId = window.setTimeout(() => {
      postBuscaTimeoutId = null;
      xpaths.forEach((xpath) => {
        const botao = obterElementoPorXPath(xpath);
        if (botao && typeof botao.click === "function") {
          botao.click();
        }
      });
    }, 1000);
  }

  async function aplicarFiltroGlobal(filtro, options = {}) {
    const { dispararPosBusca = false } = options;
    if (!filtro) return;
    atualizarEstadoComFiltro(filtro);
    await carregarIndicadores();
    atualizarInterface();
    if (dispararPosBusca) {
      agendarCliquesPosBusca();
    }
  }

  function registrarFiltroGlobal() {
    const handler = async (event) => {
      const detalhe = event && typeof event === "object" && "detail" in event ? event.detail : event;
      if (!detalhe) return;

      const eventType = event && typeof event === "object" && "type" in event ? event.type : null;
      const trigger = detalhe.trigger || (eventType === "dashboardFiltro:ready" ? "init" : "unknown");

      filtroPendente = {
        ...detalhe,
        municipios: Array.isArray(detalhe.municipios) ? [...detalhe.municipios] : []
      };

      if (eventType !== "dashboardFiltro:change" || trigger !== "button") {
        return;
      }

      const aplicarSePronto = async () => {
        if (!dadosProntos) {
          return;
        }
        await aplicarFiltroGlobal(filtroPendente, { dispararPosBusca: trigger === "button" });
      };

      if (!dadosProntos) {
        garantirDadosProntos()
          .then(aplicarSePronto)
          .catch((erro) => {
            console.error("Falha ao preparar dados de evolução", erro);
          });
        return;
      }

      await aplicarSePronto();
    };

    document.addEventListener("dashboardFiltro:ready", handler);
    document.addEventListener("dashboardFiltro:change", handler);

    if (window.DashboardFilter) {
      if (typeof window.DashboardFilter.subscribe === "function") {
        window.DashboardFilter.subscribe((novoFiltro) => handler({ type: "dashboardFiltro:change", detail: novoFiltro }));
      }
      if (typeof window.DashboardFilter.getState === "function") {
        const inicial = window.DashboardFilter.getState();
        if (inicial) {
          handler({ type: "dashboardFiltro:ready", detail: inicial });
        }
      }
    }
  }

  function atualizarResumoTabela() {
    if (!elements.resumoTabela) return;
    elements.resumoTabela.innerHTML = "";

    if (!state.selectedMunicipios.length) {
      const row = document.createElement("tr");
      const cell = document.createElement("td");
      cell.colSpan = 5;
      cell.className = "text-muted small";
      cell.textContent = "Selecione ao menos um município para ver o resumo.";
      row.appendChild(cell);
      elements.resumoTabela.appendChild(row);
      return;
    }

    const anoReferenciaCalculo = obterAnoReferenciaCalculo();

    state.selectedMunicipios.forEach((id) => {
      const municipio = obterMunicipioPorId(id);
      if (!municipio) return;
      const hist = filtrarHistoricoPorPeriodo(municipio, state.mesInicio, state.mesFim);
      const ultimo = hist[hist.length - 1];
      const variacaoMensal = calcularVariacaoMensal(hist);
      const variacaoAnual = calcularVariacaoAnual(hist, anoReferenciaCalculo);
      const tendencia = inferirTendencia(hist);
      const row = document.createElement("tr");
      const tdNome = document.createElement("td");
      tdNome.textContent = municipio.nome;
      const tdPreco = document.createElement("td");
      tdPreco.className = "text-end";
      tdPreco.textContent = ultimo ? formatCurrency(ultimo.preco) : "—";
      const tdVarMensal = document.createElement("td");
      tdVarMensal.className = "text-end";
      tdVarMensal.dataset.colVariacao = "mensal";
      tdVarMensal.classList.remove("text-success", "text-danger", "text-muted");
      if (variacaoMensal == null) {
        tdVarMensal.textContent = "—";
        tdVarMensal.classList.add("text-muted");
      } else {
        tdVarMensal.textContent = formatPercent(variacaoMensal);
        tdVarMensal.classList.add(variacaoMensal >= 0 ? "text-danger" : "text-success");
      }
      const tdVarAnual = document.createElement("td");
      tdVarAnual.className = "text-end";
      tdVarAnual.dataset.colVariacao = "anual";
      tdVarAnual.classList.remove("text-success", "text-danger", "text-muted");
      if (variacaoAnual == null) {
        tdVarAnual.textContent = "—";
        tdVarAnual.classList.add("text-muted");
      } else {
        tdVarAnual.textContent = formatPercent(variacaoAnual);
        tdVarAnual.classList.add(variacaoAnual >= 0 ? "text-danger" : "text-success");
      }
      const tdTendencia = document.createElement("td");
      tdTendencia.className = "text-end";
      const badge = document.createElement("span");
      badge.className = `badge rounded-pill ${tendencia.badge}`;
      badge.textContent = tendencia.label;
      tdTendencia.appendChild(badge);

      row.appendChild(tdNome);
      row.appendChild(tdPreco);
      row.appendChild(tdVarMensal);
      row.appendChild(tdVarAnual);
      row.appendChild(tdTendencia);
      elements.resumoTabela.appendChild(row);
    });
  }

  function sincronizarVariacaoUi() {
    const modo = state.variacaoModo === "anual" ? "anual" : "mensal";
    const mostrarMensal = modo === "mensal";
    const mostrarAnual = modo === "anual";

    const toggleNodes = (nodes, show) => {
      Array.from(nodes || []).forEach((node) => {
        node.classList.toggle("d-none", !show);
      });
    };

    toggleNodes(elements.variacaoMensalNodes, mostrarMensal);
    toggleNodes(elements.variacaoAnualNodes, mostrarAnual);

    if (elements.resumoTabela) {
      toggleNodes(elements.resumoTabela.querySelectorAll('[data-col-variacao="mensal"]'), mostrarMensal);
      toggleNodes(elements.resumoTabela.querySelectorAll('[data-col-variacao="anual"]'), mostrarAnual);
    }

    const descricao = obterDescricaoVariacao(modo);
    const descricaoCapitalizada = descricao.charAt(0).toUpperCase() + descricao.slice(1);

    if (elements.variacaoLegenda) {
      elements.variacaoLegenda.textContent = `Modo de variação: ${descricaoCapitalizada}`;
    }

    if (elements.resumoLegendaVariacao) {
      elements.resumoLegendaVariacao.textContent = modo === "anual"
        ? "Resumo com variação anual."
        : "Resumo com variação mensal.";
    }

    if (elements.resumoHeaderVariacao) {
      Array.from(elements.resumoHeaderVariacao).forEach((header) => {
        const tipo = header.getAttribute("data-col-variacao");
        if (!tipo) return;
        const mostrar = tipo === "mensal" ? mostrarMensal : mostrarAnual;
        header.classList.toggle("d-none", !mostrar);
      });
    }

  }

  function renderKpis() {
    if (!elements.kpiPrecoAtual) {
      return;
    }

    const modo = state.variacaoModo === "anual" ? "anual" : "mensal";
    if (elements.kpiVariacaoTipo) {
      elements.kpiVariacaoTipo.textContent = obterRotuloVariacao(modo);
    }

    if (!INDICADORES) {
      elements.kpiPrecoAtual.textContent = "R$ --";
      elements.kpiPrecoDescricao.textContent = "Selecione um município";
      if (elements.kpiVariacao) {
        elements.kpiVariacao.className = "kpi-card__value";
        elements.kpiVariacao.textContent = "—";
      }
      if (elements.kpiVariacaoDescricao) {
        elements.kpiVariacaoDescricao.textContent = "Sem variação calculada";
      }
      if (elements.kpiTendencia) {
        elements.kpiTendencia.textContent = "—";
        elements.kpiTendencia.className = "kpi-card__value";
      }
      if (elements.kpiTendenciaDescricao) {
        elements.kpiTendenciaDescricao.textContent = "Adicione municípios para acompanhar a tendência.";
      }
      return;
    }

    const menor = INDICADORES.menorPreco || null;
    if (menor && Number.isFinite(Number(menor.valor))) {
      elements.kpiPrecoAtual.textContent = formatCurrency(Number(menor.valor));
      const descricao = menor.observacao
        || [menor.municipio, menor.mesDescricao].filter(Boolean).join(" • ")
        || menor.municipio
        || "Menor preço no período";
      elements.kpiPrecoDescricao.textContent = descricao;
    } else {
      elements.kpiPrecoAtual.textContent = "R$ --";
      elements.kpiPrecoDescricao.textContent = "Dados indisponíveis";
    }

    const variacaoDados = modo === "anual" ? INDICADORES.variacaoAnual : INDICADORES.variacaoMensal;
    if (elements.kpiVariacao) {
      elements.kpiVariacao.className = "kpi-card__value";
      if (variacaoDados && Number.isFinite(Number(variacaoDados.percentual))) {
        const percentual = Number(variacaoDados.percentual);
        elements.kpiVariacao.textContent = formatPercent(percentual);
        if (percentual > 0) {
          elements.kpiVariacao.classList.add("text-danger");
        } else if (percentual < 0) {
          elements.kpiVariacao.classList.add("text-success");
        }
      } else {
        elements.kpiVariacao.textContent = "—";
      }
    }
    if (elements.kpiVariacaoDescricao) {
      const descricao = variacaoDados?.descricao || (modo === "anual"
        ? "Sem dados anuais suficientes."
        : "Sem dados mensais suficientes.");
      elements.kpiVariacaoDescricao.textContent = descricao;
    }

    const tendencia = INDICADORES.tendencia || null;
    if (elements.kpiTendencia) {
      elements.kpiTendencia.className = "kpi-card__value";
      if (tendencia && tendencia.texto) {
        elements.kpiTendencia.textContent = tendencia.texto;
        if (tendencia.classe) {
          elements.kpiTendencia.classList.add(tendencia.classe);
        }
      } else {
        elements.kpiTendencia.textContent = "—";
      }
    }
    if (elements.kpiTendenciaDescricao) {
      if (tendencia && tendencia.descricao) {
        elements.kpiTendenciaDescricao.textContent = tendencia.descricao;
      } else {
        elements.kpiTendenciaDescricao.textContent = "Adicione municípios para acompanhar a tendência.";
      }
    }
  }

  function atualizarBadgePeriodo() {
    if (!elements.periodoSelecionado) return;
    const inicio = parseMes(state.mesInicio);
    const fim = parseMes(state.mesFim);
    if (!inicio && !fim) {
      elements.periodoSelecionado.textContent = "Todos os períodos";
      return;
    }

    if (inicio && fim) {
      const labelInicio = inicio.label;
      const labelFim = fim.label;
      const quantidadeMeses = MESES_DISPONIVEIS.filter((mes) => {
        const info = parseMes(mes);
        if (!info) return false;
        return info.key >= inicio.key && info.key <= fim.key;
      }).length;
      elements.periodoSelecionado.textContent = `${labelInicio} – ${labelFim} • ${quantidadeMeses} meses`;
      return;
    }

    if (inicio && !fim) {
      elements.periodoSelecionado.textContent = `Desde ${inicio.label}`;
      return;
    }

    if (!inicio && fim) {
      elements.periodoSelecionado.textContent = `Até ${fim.label}`;
      return;
    }
  }

  function atualizarPainelMunicipioAtivo() {
    const municipio = obterMunicipioPorId(state.municipioAtivo);
    if (!municipio) {
      elements.municipioTitulo.textContent = "Selecione um município";
      elements.municipioSubtitulo.textContent = "Utilize o mapa ou os filtros laterais.";
      elements.municipioBadge.textContent = "—";
      elements.municipioBadge.className = "badge rounded-pill text-bg-secondary";
      elements.municipioPreco.textContent = "—";
      if (elements.municipioVariacaoMensal) elements.municipioVariacaoMensal.textContent = "—";
      elements.municipioVariacaoAnual.textContent = "—";
      if (elements.municipioMenorPreco) elements.municipioMenorPreco.textContent = "—";
      if (elements.municipioMaiorPreco) elements.municipioMaiorPreco.textContent = "—";
      if (elements.municipioItemMaior) elements.municipioItemMaior.textContent = "—";
      if (elements.municipioItemMenor) elements.municipioItemMenor.textContent = "—";
      elements.municipioTendencia.textContent = "—";
      if (elements.municipioTendenciaDetalhe) {
        elements.municipioTendenciaDetalhe.textContent = "—";
      }
      return;
    }

    const hist = filtrarHistoricoPorPeriodo(municipio, state.mesInicio, state.mesFim);
    const ultimo = hist[hist.length - 1];
    const variacaoMensal = calcularVariacaoMensal(hist);
    const variacaoAnual = calcularVariacaoAnual(hist, obterAnoReferenciaCalculo());
    const tendencia = inferirTendencia(hist);
    const extremos = encontrarExtremosPreco(hist);
    const componentesResumo = obterResumoComponentes(hist);

    elements.municipioTitulo.textContent = municipio.nome;
    if (hist.length) {
      const inicioLabel = hist[0].labelLongo;
      const fimLabel = ultimo ? ultimo.labelLongo : inicioLabel;
      elements.municipioSubtitulo.textContent = `${inicioLabel} a ${fimLabel}`;
    } else {
      elements.municipioSubtitulo.textContent = "Sem registros no período filtrado.";
    }

    elements.municipioPreco.textContent = ultimo ? formatCurrency(ultimo.preco) : "—";

    const aplicarVariacao = (element, valor) => {
      if (!element) return;
      element.classList.remove("text-success", "text-danger", "text-muted");
      if (valor == null) {
        element.textContent = "—";
        element.classList.add("text-muted");
      } else {
        element.textContent = formatPercent(valor);
        if (valor > 0) {
          element.classList.add("text-danger");
        } else if (valor < 0) {
          element.classList.add("text-success");
        }
      }
    };

    aplicarVariacao(elements.municipioVariacaoMensal, variacaoMensal);
    aplicarVariacao(elements.municipioVariacaoAnual, variacaoAnual);

    if (elements.municipioMenorPreco) {
      const menor = extremos.menor;
      elements.municipioMenorPreco.textContent = menor ? `${formatCurrency(menor.preco)} • ${menor.label}` : "—";
    }

    if (elements.municipioMaiorPreco) {
      const maior = extremos.maior;
      elements.municipioMaiorPreco.textContent = maior ? `${formatCurrency(maior.preco)} • ${maior.label}` : "—";
    }

    if (elements.municipioItemMaior) {
      const itemMaior = componentesResumo.maior;
      elements.municipioItemMaior.textContent = itemMaior
        ? `${formatComponentLabel(itemMaior.chave)} • ${formatCurrency(itemMaior.valor)}`
        : "—";
    }

    if (elements.municipioItemMenor) {
      const itemMenor = componentesResumo.menor;
      elements.municipioItemMenor.textContent = itemMenor
        ? `${formatComponentLabel(itemMenor.chave)} • ${formatCurrency(itemMenor.valor)}`
        : "—";
    }

    elements.municipioTendencia.textContent = tendencia.label;
    if (elements.municipioTendenciaDetalhe) {
      elements.municipioTendenciaDetalhe.textContent = tendencia.descricao;
    }

    elements.municipioBadge.textContent = tendencia.label;
    elements.municipioBadge.className = `badge rounded-pill ${tendencia.badge}`;
  }

  function atualizarGrafico() {
    if (!evolucaoChart) {
      return;
    }

    const selecionados = filtrarSelecaoMunicipios(state.selectedMunicipios);
    const container = document.getElementById("chartEvolucao");

    if (!selecionados.length) {
      evolucaoChart.removeAllSeries();
      evolucaoChart.draw();
      return;
    }

    const inicioFiltro = parseMes(state.mesInicio);
    const fimFiltro = parseMes(state.mesFim);

    let mesesPeriodo = MESES_DISPONIVEIS.filter((mes) => {
      const info = parseMes(mes);
      if (!info) return false;
      if (inicioFiltro && info.key < inicioFiltro.key) return false;
      if (fimFiltro && info.key > fimFiltro.key) return false;
      return true;
    });

    if (!mesesPeriodo.length) {
      const mesesSet = new Set();
      selecionados.forEach((id) => {
        const municipio = obterMunicipioPorId(id);
        if (!municipio) return;
        const hist = filtrarHistoricoPorPeriodo(municipio, state.mesInicio, state.mesFim);
        hist.forEach((item) => {
          if (item?.mes) {
            mesesSet.add(item.mes);
          }
        });
      });
      mesesPeriodo = Array.from(mesesSet).sort((a, b) => {
        const infoA = parseMes(a);
        const infoB = parseMes(b);
        if (!infoA || !infoB) return 0;
        return infoA.key - infoB.key;
      });
    }

    if (!mesesPeriodo.length) {
      evolucaoChart.removeAllSeries();
      evolucaoChart.draw();
      return;
    }

    const labels = mesesPeriodo.map((mes) => {
      const info = parseMes(mes);
      return info ? info.label : mes;
    });

    evolucaoChart.removeAllSeries();

    selecionados.forEach((id, index) => {
      const municipio = obterMunicipioPorId(id);
      if (!municipio) return;
      const hist = filtrarHistoricoPorPeriodo(municipio, state.mesInicio, state.mesFim);
      const dataMap = new Map(hist.map((item) => [item.mes, item.preco]));
      const color = colorPalette[index % colorPalette.length];

      const serieData = mesesPeriodo.map((mes, idx) => {
        const valor = dataMap.get(mes);
        return {
          x: labels[idx],
          value: Number.isFinite(valor) ? valor : null
        };
      });

      const serie = evolucaoChart.area(serieData);
      serie.name(municipio?.nome || "");
      serie.stroke({ color, thickness: 2 });
      serie.fill(anychart.color.setOpacity(color, 0.3));
      serie.hovered().stroke({ color, thickness: 2 });
      serie.markers(true);
      serie.markers().type("circle").size(4).fill(color).stroke("#ffffff");
      serie.hovered().markers().enabled(true).size(6);
      serie.tooltip().format(function () {
        const valor = this.value;
        return `${this.seriesName}: ${formatCurrency(valor)}`;
      });

      const valoresValidos = hist
        .map((item) => item.preco)
        .filter((valor) => Number.isFinite(valor));
      if (valoresValidos.length) {
        const media = valoresValidos.reduce((acc, valor) => acc + valor, 0) / valoresValidos.length;
        const mediaData = mesesPeriodo.map((_, idx) => ({
          x: labels[idx],
          value: media
        }));
        const mediaSerie = evolucaoChart.line(mediaData);
        mediaSerie.name(`${municipio?.nome || ""} • média`);
        mediaSerie.stroke({ color, dash: "6 6", thickness: 1.5 });
        mediaSerie.hovered().stroke({ color, dash: "6 6", thickness: 1.5 });
        mediaSerie.markers(false);
        mediaSerie.hovered().markers(false);
        mediaSerie.tooltip().format(function () {
          const valor = this.value;
          return `${this.seriesName}: ${formatCurrency(valor)}`;
        });
      }
    });

    evolucaoChart.draw();
  }

  function sincronizarMapaSelecao() {
    if (!mapaMarkers || mapaMarkers.size === 0) {
      if (mapaInfoWindow) {
        mapaInfoWindow.close();
        mapaInfoWindow.__markerId = null;
      }
      return;
    }
    if (!googleMapsApiInstance) {
      return;
    }

    let ativoInfo = null;
    mapaMarkers.forEach((info, id) => {
      const marker = info?.marker;
      const data = info?.data;
      if (!marker || !data) {
        return;
      }
      const selecionado = state.selectedMunicipios.includes(id);
      const ativo = state.municipioAtivo === id;
      const hasDados = Boolean(data.hasDados);
      let baseColor = hasDados ? "#2563eb" : "#94a3b8";
      if (selecionado) {
        baseColor = "#1d4ed8";
      }
      if (ativo) {
        baseColor = "#22c55e";
      }
      const radius = ativo ? 11 : selecionado ? 8 : 6;
      const stroke = ativo ? 3 : selecionado ? 2.5 : 1.5;
      marker.setIcon({
        path: googleMapsApiInstance.SymbolPath.CIRCLE,
        scale: radius,
        fillColor: baseColor,
        fillOpacity: hasDados ? 0.9 : 0.55,
        strokeColor: baseColor,
        strokeWeight: stroke
      });
      marker.setZIndex(ativo ? 3 : (selecionado ? 2 : 1));
      if (ativo) {
        ativoInfo = info;
      }
    });

    if (mapaInfoWindow) {
      if (ativoInfo && ativoInfo.data && ativoInfo.data.tooltipHtml) {
        mapaInfoWindow.setContent(ativoInfo.data.tooltipHtml);
        mapaInfoWindow.setPosition({ lat: ativoInfo.data.latitude, lng: ativoInfo.data.longitude });
        mapaInfoWindow.open(mapaGoogle, ativoInfo.marker);
        mapaInfoWindow.__markerId = ativoInfo.data.id;
      } else {
        mapaInfoWindow.close();
        mapaInfoWindow.__markerId = null;
      }
    }
  }

  function construirDadosMapa() {
    const anoReferenciaCalculo = obterAnoReferenciaCalculo();
    if (!state.selectedMunicipios.length) {
      return [];
    }
    const filtroSelecionados = new Set(state.selectedMunicipios);

    return MUNICIPIOS_DATA.filter((municipio) => filtroSelecionados.has(municipio.id)).map((municipio) => {
      const hist = filtrarHistoricoPorPeriodo(municipio, state.mesInicio, state.mesFim);
      const ultimo = hist[hist.length - 1];
      const variacaoMensal = calcularVariacaoMensal(hist);
      const variacaoAnual = calcularVariacaoAnual(hist, anoReferenciaCalculo);
      const tendencia = inferirTendencia(hist);
      const hasDados = Boolean(ultimo && Number.isFinite(ultimo.preco));
      const mostrarMensal = state.variacaoModo !== "anual";
      const mostrarAnual = state.variacaoModo !== "mensal";
      const extremos = encontrarExtremosPreco(hist);
      const componentesResumo = obterResumoComponentes(hist);


      if (!Number.isFinite(municipio.latitude) || !Number.isFinite(municipio.longitude)) {
        return null;
      }


      let tooltipHtml = "";
      if (hasDados) {
        const infoList = [];
        infoList.push(`<li>Preço atual: <strong>${formatCurrency(ultimo.preco)}</strong></li>`);
        if (mostrarMensal && variacaoMensal != null) {
          infoList.push(`<li>Variação mensal: <strong>${formatPercent(variacaoMensal)}</strong></li>`);
        }
        if (mostrarAnual && variacaoAnual != null) {
          infoList.push(`<li>Variação anual: <strong>${formatPercent(variacaoAnual)}</strong></li>`);
        }
        if (extremos.menor) {
          infoList.push(`<li>Menor preço: <strong>${formatCurrency(extremos.menor.preco)}</strong> (${extremos.menor.label})</li>`);
        }
        if (extremos.maior) {
          infoList.push(`<li>Maior preço: <strong>${formatCurrency(extremos.maior.preco)}</strong> (${extremos.maior.label})</li>`);
        }
        if (componentesResumo.maior) {
          infoList.push(`<li>Item mais caro: <strong>${formatComponentLabel(componentesResumo.maior.chave)}</strong> (${formatCurrency(componentesResumo.maior.valor)})</li>`);
        }
        if (componentesResumo.menor) {
          infoList.push(`<li>Item mais barato: <strong>${formatComponentLabel(componentesResumo.menor.chave)}</strong> (${formatCurrency(componentesResumo.menor.valor)})</li>`);
        }
        const tendenciaResumo = Number.isFinite(tendencia.percentSlope)
          ? `${formatPercent(tendencia.percentSlope, 2)} ao mês`
          : "";
        const tendenciaLinha = tendenciaResumo
          ? `<li>Tendência: <strong>${tendencia.label}</strong> <span class="text-muted">(${tendenciaResumo})</span></li>`
          : `<li>Tendência: <strong>${tendencia.label}</strong></li>`;
        infoList.push(tendenciaLinha);
        const periodoLabel = ultimo?.labelLongo ? `<small>${ultimo.labelLongo}</small>` : "";
        tooltipHtml = [
          '<div class="mapa-tooltip">',
          `<strong>${municipio.nome}</strong>`,
          periodoLabel,
          `<ul>${infoList.join("")}</ul>`,
          '</div>'
        ].join('');
      }

      return {
        id: municipio.id,
        title: municipio.nome,
        latitude: municipio.latitude,
        longitude: municipio.longitude,
        precoAtual: hasDados ? Number(ultimo.preco.toFixed(2)) : null,
        variacaoMensal,
        variacaoAnual,
        tendencia,
        menorPreco: extremos.menor,
        maiorPreco: extremos.maior,
        itemMaior: componentesResumo.maior,
        itemMenor: componentesResumo.menor,
        hasDados,
        tooltipHtml
      };
    }).filter(Boolean);
  }

  function inicializarMapa() {
    const mapaContainer = document.getElementById("mapaMunicipios");
    if (!mapaContainer) {
      return;
    }

    loadGoogleMapsApi()
      .then((maps) => {
        googleMapsApiInstance = maps;
        if (!mapaContainer.isConnected) {
          return;
        }
        if (!mapaGoogle) {
          mapaGoogle = new maps.Map(mapaContainer, {
            center: { lat: -14.235, lng: -51.9253 },
            zoom: 4,
            mapTypeControl: false,
            streetViewControl: false,
            fullscreenControl: false,
            gestureHandling: "greedy"
          });
          mapaMarkers = new Map();
          mapaOverlays = [];
          mapaInfoWindow = new maps.InfoWindow();
          mapaBoundsInitialized = false;

          setTimeout(() => {
            maps.event.trigger(mapaGoogle, "resize");
          }, 200);
        }

        atualizarMapa();
      })
      .catch((err) => {
        console.warn("Não foi possível inicializar o mapa de evolução.", err);
        const legenda = document.getElementById("municipioAtivoSubtitulo");
        if (legenda) {
          legenda.textContent = "Mapa indisponível no momento. Recarregue a página para tentar novamente.";
        }
      });
  }

  function atualizarMapa() {
    if (!mapaGoogle || !googleMapsApiInstance) {
      inicializarMapa();
      return;
    }

    const dados = construirDadosMapa();
    mapaOverlays.forEach((overlay) => {
      if (overlay && typeof overlay.setMap === "function") {
        overlay.setMap(null);
      }
    });
    mapaOverlays = [];
    mapaMarkers.clear();
    if (mapaInfoWindow) {
      mapaInfoWindow.close();
      mapaInfoWindow.__markerId = null;
    }

    if (!dados.length) {
      mapaBoundsInitialized = false;
      return;
    }

    const bounds = new googleMapsApiInstance.LatLngBounds();
    let possuiBounds = false;

    dados.forEach((data) => {
      if (!Number.isFinite(data.latitude) || !Number.isFinite(data.longitude)) {
        return;
      }
      const position = { lat: data.latitude, lng: data.longitude };
      bounds.extend(position);
      possuiBounds = true;

      const baseColor = data.hasDados ? "#2563eb" : "#94a3b8";
      const marker = new googleMapsApiInstance.Marker({
        position,
        map: mapaGoogle,
        icon: {
          path: googleMapsApiInstance.SymbolPath.CIRCLE,
          scale: 6,
          fillColor: baseColor,
          fillOpacity: data.hasDados ? 0.9 : 0.55,
          strokeColor: baseColor,
          strokeWeight: 1.5
        },
        title: data.title || "",
        zIndex: 1
      });

      marker.addListener("click", () => {
        if (data.hasDados) {
          setMunicipioAtivo(data.id);
        }
      });

      marker.addListener("mouseover", () => {
        if (data.hasDados) {
          setMunicipioAtivo(data.id, true);
        }
      });

      mapaMarkers.set(data.id, { marker, data });
      mapaOverlays.push(marker);
    });

    if (possuiBounds && !mapaBoundsInitialized) {
      mapaGoogle.fitBounds(bounds, { top: 40, right: 40, bottom: 40, left: 40 });
      mapaBoundsInitialized = true;
    }

    sincronizarMapaSelecao();
  }

  function atualizarInterface() {

    garantirPeriodoValido();

    state.selectedMunicipios = filtrarSelecaoMunicipios(state.selectedMunicipios);
    if (state.selectedMunicipios.length && !state.municipioAtivo) {
      state.municipioAtivo = state.selectedMunicipios[0];
    }
    if (state.municipioAtivo && !state.selectedMunicipios.includes(state.municipioAtivo)) {
      state.municipioAtivo = state.selectedMunicipios[0] || null;
    }

    atualizarResumoTabela();
    sincronizarVariacaoUi();
    renderKpis();
    atualizarBadgePeriodo();
    atualizarPainelMunicipioAtivo();
    atualizarGrafico();
    atualizarMapa();
  }

  function setMunicipioAtivo(id, fromHover) {
    if (!id) return;
    state.municipioAtivo = id;
    atualizarPainelMunicipioAtivo();
    sincronizarMapaSelecao();
    if (!fromHover && !state.selectedMunicipios.includes(id)) {
      state.selectedMunicipios.push(id);
      atualizarInterface();
      carregarIndicadores();
    }
  }

  function toggleMunicipioSelecao(id) {
    const index = state.selectedMunicipios.indexOf(id);
    if (index >= 0) {
      state.selectedMunicipios.splice(index, 1);
    } else {
      state.selectedMunicipios.push(id);
    }
    if (!state.selectedMunicipios.length) {
      state.municipioAtivo = null;
    } else if (!state.selectedMunicipios.includes(state.municipioAtivo)) {
      state.municipioAtivo = state.selectedMunicipios[0];
    }
    atualizarInterface();
    carregarIndicadores();
  }

  function inicializarGrafico() {
    const container = document.getElementById("chartEvolucao");
    if (!container) return;

    container.innerHTML = "";
    evolucaoChart = anychart.cartesian();
    evolucaoChart.animation(true);
    evolucaoChart.xScale().mode("ordinal");
    evolucaoChart.crosshair().enabled(true).yLabel(false).yStroke(null);
    evolucaoChart.interactivity().hoverMode("by-x");
    evolucaoChart.background().fill("transparent");
    evolucaoChart.padding(20, 20, 20, 40);
    evolucaoChart.xAxis().labels().fontColor("#64748b");
    evolucaoChart.xAxis().ticks(true);
    evolucaoChart.yAxis().labels().format(function () {
      return formatCurrency(this.value);
    }).fontColor("#64748b");
    evolucaoChart.legend().enabled(true);
    evolucaoChart.legend().position("bottom");
    evolucaoChart.legend().itemsLayout("horizontal");
    evolucaoChart.tooltip().format(function () {
      return `${this.seriesName}: ${formatCurrency(this.value)}`;
    });
    evolucaoChart.container(container);
    evolucaoChart.draw();
  }

  async function init() {
    obterElementos();
    registrarFiltroGlobal();
    aplicarPeriodosPrefetch(PERIODOS_PREFETCH);

    const resetMapaBtn = document.getElementById("mapaRecentrar");
    if (resetMapaBtn) {
      resetMapaBtn.addEventListener("click", () => {
        if (!dadosProntos) {
          return;
        }
        mapaBoundsInitialized = false;
        if (googleMapsApiInstance && mapaGoogle) {
          googleMapsApiInstance.event.trigger(mapaGoogle, "resize");
        }
        atualizarMapa();
      });
    }

    if (!window.DashboardFilter || typeof window.DashboardFilter.subscribe !== "function") {
      try {
        await garantirDadosProntos();
        if (dadosProntos) {
          const fallback = {
            municipios: MUNICIPIOS_DATA.slice(0, 3).map((item) => item.id),
            periodo: {
              inicio: MESES_DISPONIVEIS[0] || null,
              fim: MESES_DISPONIVEIS[MESES_DISPONIVEIS.length - 1] || null,
              anoReferencia: state.anoReferencia
            },
            variacaoModo: state.variacaoModo,
            trigger: "fallback"
          };
          filtroPendente = fallback;
          await aplicarFiltroGlobal(fallback);
        }
      } catch (erro) {
        console.error("Falha ao carregar dados iniciais de evolução", erro);
      }
    }
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();


// =======================
// GESTÃO DE EVENTOS (DADOS)
// =======================
(function(){
  const modal = document.getElementById('modalEditarEvento');
  const formEditar = document.getElementById('formEditarEvento');
  const formNovo = document.getElementById('formNovoEvento');

  function configurarValidacaoDatas(form, inicioSelector, fimSelector){
    if (!form) return;
    const inicio = form.querySelector(inicioSelector);
    const fim = form.querySelector(fimSelector);
    if (!inicio || !fim) return;

    function validar(){
      inicio.setCustomValidity('');
      fim.setCustomValidity('');
      if (inicio.value && fim.value && inicio.value > fim.value){
        fim.setCustomValidity('A data final deve ser igual ou posterior à data inicial.');
      }
    }

    inicio.addEventListener('change', validar);
    fim.addEventListener('change', validar);
    form.addEventListener('submit', function(event){
      validar();
      if (!form.checkValidity()){
        event.preventDefault();
        form.reportValidity();
      }
    });
  }

  configurarValidacaoDatas(formNovo, '#eventoDataInicio', '#eventoDataFim');
  configurarValidacaoDatas(formEditar, '#editarDataInicio', '#editarDataFim');

  if (!modal || !formEditar) {
    return;
  }

  modal.addEventListener('show.bs.modal', function(event){
    const button = event.relatedTarget;
    if (!button) return;
    const id = button.getAttribute('data-evento-id');
    const titulo = button.getAttribute('data-evento-titulo') || '';
    const descricao = button.getAttribute('data-evento-descricao') || '';
    const inicio = button.getAttribute('data-evento-inicio') || '';
    const fim = button.getAttribute('data-evento-fim') || '';
    const impacto = button.getAttribute('data-evento-impacto') || '';
    const actionUrl = button.getAttribute('data-evento-url');

    if (actionUrl) {
      formEditar.action = actionUrl;
    } else {
      formEditar.action = '/dados/eventos/' + id + '/atualizar';
    }
    formEditar.querySelector('#editarTitulo').value = titulo;
    formEditar.querySelector('#editarDescricao').value = descricao;
    formEditar.querySelector('#editarDataInicio').value = inicio;
    formEditar.querySelector('#editarDataFim').value = fim;
    const selectImpacto = formEditar.querySelector('#editarImpacto');
    if (selectImpacto) {
      selectImpacto.value = impacto;
    }
  });

  modal.addEventListener('hidden.bs.modal', function(){
    formEditar.reset();
    formEditar.action = '';
  });
})();


// =======================
// DADOS MOCK / UTILITÁRIOS
// =======================
const DASHBOARD_CONFIG = window.dashboardConfig || {};
const SERIE_HISTORICA_URL = DASHBOARD_CONFIG.serieHistoricaUrl || '';
const SERIE_MUNICIPIOS_URL = DASHBOARD_CONFIG.serieMunicipiosUrl || '';
const MUNICIPIOS_URL = DASHBOARD_CONFIG.municipiosUrl || '';
const EVENTOS_EXTERNOS_URL = DASHBOARD_CONFIG.eventosExternosUrl || '';
const PESO_MUNICIPIOS_URL = DASHBOARD_CONFIG.pesoMunicipiosUrl || '';
const EVOLUCAO_PREFETCH = window.evolucaoPrefetch || {};
const PREFETCH_SERIES = Array.isArray(EVOLUCAO_PREFETCH.series) ? EVOLUCAO_PREFETCH.series : [];
const PREFETCH_MUNICIPIOS = Array.isArray(EVOLUCAO_PREFETCH.municipios) ? EVOLUCAO_PREFETCH.municipios : [];
const SERIE_HISTORICA_MOCK = [
  { mes: "2024-09", cesta: 298.3 },{ mes: "2024-10", cesta: 301.1 },
  { mes: "2024-11", cesta: 304.9 },{ mes: "2024-12", cesta: 306.2 },
  { mes: "2025-01", cesta: 309.7 },{ mes: "2025-02", cesta: 312.1 },
  { mes: "2025-03", cesta: 314.6 },{ mes: "2025-04", cesta: 316.0 },
  { mes: "2025-05", cesta: 318.4 },{ mes: "2025-06", cesta: 320.0 },
  { mes: "2025-07", cesta: 322.4 },{ mes: "2025-08", cesta: 323.9 },
  { mes: "2025-09", cesta: 321.8 },
];

let evolucaoHistorica = [];
const MUNICIPIOS_FALLBACK = ["São Paulo","Rio de Janeiro","Belo Horizonte","Porto Alegre","Salvador","Fortaleza","Manaus","Curitiba","Recife"];

let MUNICIPIOS = [];
let mesesFull = [];
let seriesMunicipio = {};
let chRegionalLinha = null;
let chRegionalBar = null;
let chPesoPizza = null;
let chPesoBar = null;
let chEventos = null;
let googleMapsApiInstance = null;
let mapaRegionalMap = null;
let mapaRegionalMarkers = new Map();
let mapaRegionalOverlays = [];
let mapaRegionalInfoWindow = null;
let mapaRegionalBoundsApplied = false;
let ultimaSelecaoMapa = null;
let ultimoFiltroRegional = [];
let filtroDashboardAtual = null;

let eventosExternosBase = [];
let eventosExternosCache = [];
let dadosIniciaisCarregados = false;

const COLORS = ["#0ea5e9","#22c55e","#f59e0b","#ef4444","#8b5cf6","#14b8a6","#84cc16","#06b6d4"];
const COMPONENTE_LABELS = {
  carne: "Carne",
  leite: "Leite",
  feijao: "Feijão",
  arroz: "Arroz",
  farinha: "Farinha",
  batata: "Batata",
  tomate: "Tomate",
  pao: "Pão",
  cafe: "Café",
  banana: "Banana",
  acucar: "Açúcar",
  oleo: "Óleo",
  manteiga: "Manteiga"
};
const MONTH_FORMATTER = new Intl.DateTimeFormat('pt-BR', {
  month: 'short',
  year: 'numeric',
  timeZone: 'UTC'
});
const MAPA_MESES_ANALISE = {
  jan:1, january:1,
  fev:2, feb:2, february:2,
  mar:3, march:3,
  abr:4, apr:4, april:4,
  mai:5, may:5,
  jun:6, june:6,
  jul:7, july:7,
  ago:8, aug:8, august:8,
  set:9, sep:9, sept:9, september:9,
  out:10, oct:10, october:10,
  nov:11, november:11,
  dez:12, dec:12, december:12
};
const MUNICIPIOS_COORD_MAP = (function(){
  var base = [
    { nome: "Aracaju", uf: "SE", latitude: -10.9472, longitude: -37.0731 },
    { nome: "Belém", uf: "PA", latitude: -1.4558, longitude: -48.5039 },
    { nome: "Belo Horizonte", uf: "MG", latitude: -19.9167, longitude: -43.9345 },
    { nome: "Boa Vista", uf: "RR", latitude: 2.8235, longitude: -60.6758 },
    { nome: "Brasília", uf: "DF", latitude: -15.7801, longitude: -47.9292 },
    { nome: "Campo Grande", uf: "MS", latitude: -20.4697, longitude: -54.6201 },
    { nome: "Cuiabá", uf: "MT", latitude: -15.6010, longitude: -56.0974 },
    { nome: "Curitiba", uf: "PR", latitude: -25.4284, longitude: -49.2733 },
    { nome: "Florianópolis", uf: "SC", latitude: -27.5949, longitude: -48.5482 },
    { nome: "Fortaleza", uf: "CE", latitude: -3.7319, longitude: -38.5267 },
    { nome: "Goiânia", uf: "GO", latitude: -16.6869, longitude: -49.2648 },
    { nome: "João Pessoa", uf: "PB", latitude: -7.1150, longitude: -34.8631 },
    { nome: "Macapá", uf: "AP", latitude: 0.0349, longitude: -51.0694 },
    { nome: "Maceió", uf: "AL", latitude: -9.6498, longitude: -35.7089 },
    { nome: "Manaus", uf: "AM", latitude: -3.1190, longitude: -60.0217 },
    { nome: "Natal", uf: "RN", latitude: -5.7945, longitude: -35.2110 },
    { nome: "Palmas", uf: "TO", latitude: -10.2491, longitude: -48.3243 },
    { nome: "Porto Alegre", uf: "RS", latitude: -30.0319, longitude: -51.2065 },
    { nome: "Porto Velho", uf: "RO", latitude: -8.7608, longitude: -63.8999 },
    { nome: "Recife", uf: "PE", latitude: -8.0476, longitude: -34.8770 },
    { nome: "Rio Branco", uf: "AC", latitude: -9.9747, longitude: -67.8249 },
    { nome: "Rio de Janeiro", uf: "RJ", latitude: -22.9068, longitude: -43.1729 },
    { nome: "Salvador", uf: "BA", latitude: -12.9777, longitude: -38.5016 },
    { nome: "São Luís", uf: "MA", latitude: -2.5387, longitude: -44.2825 },
    { nome: "São Paulo", uf: "SP", latitude: -23.5505, longitude: -46.6333 },
    { nome: "Teresina", uf: "PI", latitude: -5.0919, longitude: -42.8034 },
    { nome: "Vitória", uf: "ES", latitude: -20.3155, longitude: -40.3128 },
    { nome: "Chapecó", uf: "SC", latitude: -27.1000, longitude: -52.6150 },
    { nome: "Itajaí", uf: "SC", latitude: -26.9101, longitude: -48.6700 },
    { nome: "Blumenau", uf: "SC", latitude: -26.9150, longitude: -49.0661 },
    { nome: "São José", uf: "SC", latitude: -27.6136, longitude: -48.6366 }
  ];
  var mapa = {};
  base.forEach(function(item){
    var baseKey = normalizeMunicipioId(item.nome);
    var asciiKey = removerAcentos(baseKey);
    var chaves = [baseKey, asciiKey];
    if (item.uf){
      var uf = item.uf.toUpperCase();
      chaves.push(baseKey + '-' + uf);
      chaves.push(asciiKey + '-' + uf);
    }
    chaves.forEach(function(chave){
      if (chave && !mapa[chave]){
        mapa[chave] = { latitude: item.latitude, longitude: item.longitude };
      }
    });
  });
  return mapa;
})();

const GOOGLE_MAPS_CONFIG = window.googleMapsConfig || {};
const GOOGLE_MAPS_API_OPTIONS = {
  key: GOOGLE_MAPS_CONFIG.apiKey || GOOGLE_MAPS_CONFIG.key || DASHBOARD_CONFIG.googleMapsApiKey || '',
  language: GOOGLE_MAPS_CONFIG.language || 'pt-BR',
  region: GOOGLE_MAPS_CONFIG.region || 'BR',
  libraries: GOOGLE_MAPS_CONFIG.libraries || 'maps'
};
const GOOGLE_MAPS_API_BASE_URL = 'https://maps.googleapis.com/maps/api/js';
const GOOGLE_MAPS_WAIT_INTERVAL = 120;
const GOOGLE_MAPS_MAX_ATTEMPTS = 50;

function loadGoogleMapsApi(){
  if (window.google && window.google.maps) {
    return Promise.resolve(window.google.maps);
  }
  if (window.__googleMapsReadyPromise && typeof window.__googleMapsReadyPromise.then === 'function') {
    return window.__googleMapsReadyPromise;
  }

  window.__googleMapsReadyPromise = new Promise(function(resolve, reject){
    if (window.google && window.google.maps) {
      resolve(window.google.maps);
      return;
    }

    var existingScript = Array.prototype.find.call(document.getElementsByTagName('script'), function(script){
      return script && typeof script.src === 'string' && script.src.indexOf('maps.googleapis.com/maps/api/js') >= 0;
    });

    if (existingScript) {
      var attempts = 0;
      (function waitForExisting(){
        if (window.google && window.google.maps) {
          resolve(window.google.maps);
          return;
        }
        attempts += 1;
        if (attempts >= GOOGLE_MAPS_MAX_ATTEMPTS) {
          reject(new Error('Google Maps não carregado.'));
          return;
        }
        setTimeout(waitForExisting, GOOGLE_MAPS_WAIT_INTERVAL);
      })();
      return;
    }

    var callbackName = '__initGoogleMapsApi';
    var params = new URLSearchParams();
    if (GOOGLE_MAPS_API_OPTIONS.key) {
      params.set('key', GOOGLE_MAPS_API_OPTIONS.key);
    } else {
      console.warn('Google Maps API key não configurada. Defina window.googleMapsConfig.apiKey ou dashboardConfig.googleMapsApiKey.');
    }
    if (GOOGLE_MAPS_API_OPTIONS.language) {
      params.set('language', GOOGLE_MAPS_API_OPTIONS.language);
    }
    if (GOOGLE_MAPS_API_OPTIONS.region) {
      params.set('region', GOOGLE_MAPS_API_OPTIONS.region);
    }
    if (GOOGLE_MAPS_API_OPTIONS.libraries) {
      params.set('libraries', GOOGLE_MAPS_API_OPTIONS.libraries);
    }
    params.set('callback', callbackName);

    var script = document.createElement('script');
    script.src = GOOGLE_MAPS_API_BASE_URL + '?' + params.toString();
    script.async = true;
    script.defer = true;
    script.dataset.googleMapsLoader = 'true';
    script.onerror = function(){
      window.__googleMapsReadyPromise = null;
      reject(new Error('Falha ao carregar Google Maps JavaScript API.'));
    };

    window[callbackName] = function(){
      resolve(window.google.maps);
      window[callbackName] = null;
    };

    document.head.appendChild(script);
  }).catch(function(err){
    window.__googleMapsReadyPromise = null;
    throw err;
  });

  return window.__googleMapsReadyPromise;
}
if (typeof window.loadGoogleMapsApi !== 'function') {
  window.loadGoogleMapsApi = loadGoogleMapsApi;
}
function brl(v){ return v!=null ? v.toLocaleString("pt-BR",{style:"currency",currency:"BRL"}) : ""; }
function pct(v){
  if (v == null || Number.isNaN(v)) return "0.0%";
  return v.toFixed(1) + "%";
}

function removerAcentos(texto){
  return texto ? String(texto).normalize('NFD').replace(/[\u0300-\u036f]/g, '') : texto;
}

function formatDisplayName(nome){
  if (!nome) return '';
  var lower = String(nome).toLocaleLowerCase('pt-BR');
  return lower.replace(/(^|[\s-])(\p{L})/gu, function(match, prefix, letter){
    return prefix + letter.toLocaleUpperCase('pt-BR');
  });
}

function formatComponentLabel(chave){
  if (!chave && chave !== 0) return '';
  var normalizado = String(chave).toLowerCase();
  if (COMPONENTE_LABELS[normalizado]) {
    return COMPONENTE_LABELS[normalizado];
  }
  return formatDisplayName(chave);
}

function formatPercent(valor, fractionDigits, includePlus){
  var casas = Number.isFinite(fractionDigits) ? fractionDigits : 1;
  var mostrarSinal = includePlus !== false;
  var numero = toFiniteNumber(valor);
  if (numero == null) {
    return null;
  }
  var texto = numero.toFixed(casas);
  if (mostrarSinal && numero > 0) {
    return '+' + texto + '%';
  }
  return texto + '%';
}

function toFiniteNumber(valor){
  if (valor == null || valor === '') return null;
  var numero = Number(valor);
  return Number.isFinite(numero) ? numero : null;
}

function calcularTendenciaSerie(hist){
  var dados = (hist || []).filter(function(item){
    return item && Number.isFinite(item.cesta);
  });

  if (dados.length < 2){
    return {
      status: 'indefinido',
      texto: 'Sem dados',
      classe: 'trend-indicador--indefinido',
      slope: 0,
      percentSlope: null
    };
  }

  var n = dados.length;
  var sumX = 0;
  var sumY = 0;
  var sumXY = 0;
  var sumX2 = 0;

  for (var i = 0; i < n; i++){
    var x = i;
    var y = dados[i].cesta;
    sumX += x;
    sumY += y;
    sumXY += x * y;
    sumX2 += x * x;
  }

  var denominator = n * sumX2 - sumX * sumX;
  if (denominator === 0){
    return {
      status: 'estavel',
      texto: 'Estável',
      classe: 'trend-indicador--estavel',
      slope: 0,
      percentSlope: 0
    };
  }

  var slope = (n * sumXY - sumX * sumY) / denominator;
  var media = sumY / n;
  var percentSlope = media ? (slope / media) * 100 : 0;

  var LIMIAR_ALTA = 0.35;
  var LIMIAR_QUEDA = -0.35;

  if (percentSlope > LIMIAR_ALTA){
    return {
      status: 'alta',
      texto: 'Alta',
      classe: 'trend-indicador--alta',
      slope: slope,
      percentSlope: percentSlope
    };
  }

  if (percentSlope < LIMIAR_QUEDA){
    return {
      status: 'queda',
      texto: 'Queda',
      classe: 'trend-indicador--queda',
      slope: slope,
      percentSlope: percentSlope
    };
  }

  return {
    status: 'estavel',
    texto: 'Estável',
    classe: 'trend-indicador--estavel',
    slope: slope,
    percentSlope: percentSlope
  };
}

function gerarCores(qtd){
  var total = Number.isFinite(qtd) ? qtd : 0;
  if (total <= 0) return [];
  var cores = [];
  for (var i = 0; i < total; i++) {
    cores.push(COLORS[i % COLORS.length]);
  }
  return cores;
}

function normalizeMunicipioId(nome){
  if (!nome && nome !== 0) return null;
  var texto = String(nome).trim();
  if (!texto) return null;
  return removerAcentos(texto).toUpperCase();
}

function extrairAnoMesInfo(mes){
  if (!mes && mes !== 0) return null;
  var texto = String(mes).trim();
  if (!texto) return null;
  var normalizado = texto.replace('/', '-');
  var iso = normalizado.match(/^(\d{4})-(\d{1,2})$/);
  if (iso){
    var anoIso = parseInt(iso[1], 10);
    var mesIso = parseInt(iso[2], 10);
    if (!Number.isNaN(anoIso) && !Number.isNaN(mesIso)){
      return { ano: anoIso, mes: mesIso, label: anoIso + '-' + String(mesIso).padStart(2,'0') };
    }
  }
  var br = normalizado.match(/^(\d{1,2})-(\d{4})$/);
  if (br){
    var mesBr = parseInt(br[1], 10);
    var anoBr = parseInt(br[2], 10);
    if (!Number.isNaN(anoBr) && !Number.isNaN(mesBr)){
      return { ano: anoBr, mes: mesBr, label: anoBr + '-' + String(mesBr).padStart(2,'0') };
    }
  }
  var partes = normalizado.split('-');
  if (partes.length === 2){
    var anoTxt = parseInt(partes[1], 10);
    if (Number.isNaN(anoTxt)) return null;
    var mesTxt = removerAcentos(partes[0] || '').toLowerCase();
    var chave = mesTxt.substring(0,3);
    var numeroMes = MAPA_MESES_ANALISE[chave] || MAPA_MESES_ANALISE[mesTxt];
    if (!numeroMes) return null;
    return { ano: anoTxt, mes: numeroMes, label: anoTxt + '-' + String(numeroMes).padStart(2,'0') };
  }
  return null;
}

function obterLimitesPeriodo(periodo){
  var inicioInfo = periodo && periodo.inicio ? extrairAnoMesInfo(periodo.inicio) : null;
  var fimInfo = periodo && periodo.fim ? extrairAnoMesInfo(periodo.fim) : null;
  var inicioKey = inicioInfo ? inicioInfo.ano * 100 + inicioInfo.mes : null;
  var fimKey = fimInfo ? fimInfo.ano * 100 + fimInfo.mes : null;
  if (inicioKey != null && fimKey != null && inicioKey > fimKey){
    fimKey = inicioKey;
  }
  return { inicioInfo: inicioInfo, fimInfo: fimInfo, inicioKey: inicioKey, fimKey: fimKey };
}

function normalizarPeriodoChave(valor){
  if (!valor && valor !== 0) return null;
  var texto = String(valor).trim();
  if (!texto) return null;
  if (texto.length >= 7 && texto[4] === '-') {
    texto = texto.substring(0, 7);
  }
  var info = extrairAnoMesInfo(texto);
  return info ? info.label : null;
}

function normalizarMesParaChave(valor){
  var chave = normalizarPeriodoChave(valor);
  if (chave) return chave;
  if (!valor && valor !== 0) return null;
  var texto = String(valor).trim();
  if (!texto) return null;
  if (texto.indexOf('-') === -1 && texto.indexOf('/') >= 0) {
    texto = texto.replace('/', '-');
  }
  return texto;
}

function normalizarImpacto(valor){
  if (!valor && valor !== 0) return '';
  var texto = String(valor).trim().toUpperCase();
  return texto === 'POSITIVO' || texto === 'NEGATIVO' ? texto : '';
}

function obterCoresImpacto(valor){
  var impacto = normalizarImpacto(valor);
  if (impacto === 'POSITIVO'){
    return { cor:'#22c55e', texto:'#166534', faixa:'rgba(34,197,94,0.12)', badge:'bg-success-subtle text-success' };
  }
  if (impacto === 'NEGATIVO'){
    return { cor:'#ef4444', texto:'#991b1b', faixa:'rgba(239,68,68,0.12)', badge:'bg-danger-subtle text-danger' };
  }
  return { cor:'#64748b', texto:'#0f172a', faixa:'rgba(148,163,184,0.12)', badge:'bg-secondary-subtle text-secondary' };
}

function rotuloImpacto(valor){
  var impacto = normalizarImpacto(valor);
  if (impacto === 'POSITIVO') return 'Impacto positivo';
  if (impacto === 'NEGATIVO') return 'Impacto negativo';
  return 'Impacto não informado';
}

function construirSeriePrefetch(mediaSeries){
  if (!Array.isArray(mediaSeries) || !mediaSeries.length) return [];
  var acumulado = new Map();
  mediaSeries.forEach(function(municipio){
    if (!municipio) return;
    var serie = Array.isArray(municipio.serie) ? municipio.serie : [];
    serie.forEach(function(ponto){
      if (!ponto) return;
      var chave = normalizarMesParaChave(ponto.mes || ponto.periodo || ponto.data);
      if (!chave) return;
      var valorBruto = ponto.cesta;
      if (valorBruto == null && ponto.valor != null) valorBruto = ponto.valor;
      if (valorBruto == null && ponto.total != null) valorBruto = ponto.total;
      var numero = Number(valorBruto);
      if (!Number.isFinite(numero)) return;
      var agregado = acumulado.get(chave);
      if (!agregado) {
        acumulado.set(chave, { soma: numero, count: 1 });
      } else {
        agregado.soma += numero;
        agregado.count += 1;
      }
    });
  });
  return Array.from(acumulado.entries()).map(function(entry){
    var chave = entry[0];
    var dado = entry[1];
    if (!dado || !dado.count) return null;
    return { mes: chave, cesta: +(dado.soma / dado.count).toFixed(1) };
  }).filter(Boolean);
}

function normalizarSerie(lista){
  var mapa = new Map();
  if (Array.isArray(lista)) {
    lista.forEach(function(item){
      if (!item) return;
      var chave = normalizarMesParaChave(item.mes || item.mesAno || item.periodo || item.data);
      if (!chave) return;
      var valorBruto = item.cesta;
      if (valorBruto == null && item.valor != null) valorBruto = item.valor;
      if (valorBruto == null && item.total != null) valorBruto = item.total;
      var numero = Number(valorBruto);
      if (!Number.isFinite(numero)) return;
      mapa.set(chave, +numero.toFixed(1));
    });
  }
  return Array.from(mapa.keys()).sort().map(function(chave){
    return { mes: chave, cesta: mapa.get(chave) };
  });
}

function construirDataIso(ano, mes, dia){
  var a = String(ano).padStart(4, '0');
  var m = String(mes).padStart(2, '0');
  var d = String(dia).padStart(2, '0');
  return a + '-' + m + '-' + d;
}

function ultimoDiaDoMes(ano, mes){
  return new Date(ano, mes, 0).getDate();
}

function compararEventosPorPeriodo(a, b){
  var inicioA = normalizarPeriodoChave(a && (a.periodoInicio || a.dataInicio));
  var inicioB = normalizarPeriodoChave(b && (b.periodoInicio || b.dataInicio));
  if (inicioA == null && inicioB == null) return 0;
  if (inicioA == null) return 1;
  if (inicioB == null) return -1;
  if (inicioA === inicioB) {
    var tituloA = (a && (a.titulo || a.rotulo)) || '';
    var tituloB = (b && (b.titulo || b.rotulo)) || '';
    return tituloA.localeCompare(tituloB, 'pt-BR', { sensitivity:'base' });
  }
  return inicioA.localeCompare(inicioB);
}

function formatarPeriodoEvento(evento){
  if (!evento) return '—';
  var inicioChave = normalizarPeriodoChave(evento.periodoInicio || evento.dataInicio);
  var fimChave = normalizarPeriodoChave(evento.periodoFim || evento.periodoInicio || evento.dataFim) || inicioChave;
  if (!inicioChave && !fimChave) return '—';
  if (!fimChave) fimChave = inicioChave;
  if (fimChave < inicioChave){
    var troca = inicioChave;
    inicioChave = fimChave;
    fimChave = troca;
  }
  var inicioInfo = extrairAnoMesInfo(inicioChave);
  var fimInfo = extrairAnoMesInfo(fimChave);
  var inicioTexto = inicioInfo ? MONTH_FORMATTER.format(new Date(Date.UTC(inicioInfo.ano, inicioInfo.mes - 1, 1))) : inicioChave;
  var fimTexto = fimInfo ? MONTH_FORMATTER.format(new Date(Date.UTC(fimInfo.ano, fimInfo.mes - 1, 1))) : fimChave;
  if (inicioTexto === fimTexto) {
    return inicioTexto;
  }
  return inicioTexto + ' – ' + fimTexto;
}

function calcularValorMedioLocal(periodoInicio, periodoFim){
  var inicio = normalizarPeriodoChave(periodoInicio);
  var fim = normalizarPeriodoChave(periodoFim || periodoInicio) || inicio;
  if (!inicio) return null;
  if (!fim) fim = inicio;
  if (fim < inicio) {
    var troca = inicio;
    inicio = fim;
    fim = troca;
  }
  var valores = evolucaoHistorica.filter(function(ponto){
    var chave = normalizarPeriodoChave(ponto && ponto.mes);
    if (!chave) return false;
    return chave >= inicio && chave <= fim;
  }).map(function(p){ return Number(p.cesta); }).filter(function(v){ return !Number.isNaN(v); });
  if (!valores.length) return null;
  var soma = valores.reduce(function(total, atual){ return total + atual; }, 0);
  return +(soma / valores.length).toFixed(2);
}

function obterMesesDoPeriodo(periodo){
  var base = mesesFull.length ? mesesFull.slice() : [];
  if (!base.length && evolucaoHistorica.length){
    base = evolucaoHistorica.map(function(x){ return x.mes; });
  }
  if (!base.length){
    base = SERIE_HISTORICA_MOCK.map(function(x){ return x.mes; });
  }
  var limites = obterLimitesPeriodo(periodo);
  if (limites.inicioKey == null && limites.fimKey == null){
    return base;
  }
  return base.filter(function(mes){
    var info = extrairAnoMesInfo(mes);
    if (!info) return true;
    var key = info.ano * 100 + info.mes;
    if (limites.inicioKey != null && key < limites.inicioKey) return false;
    if (limites.fimKey != null && key > limites.fimKey) return false;
    return true;
  });
}

function filtrarSeriePorPeriodo(serie, periodo){
  if (!Array.isArray(serie)) return [];
  var limites = obterLimitesPeriodo(periodo);
  if (limites.inicioKey == null && limites.fimKey == null){
    return serie.slice();
  }
  return serie.filter(function(ponto){
    if (!ponto || ponto.mes == null) return false;
    var info = extrairAnoMesInfo(ponto.mes);
    if (!info) return false;
    var key = info.ano * 100 + info.mes;
    if (limites.inicioKey != null && key < limites.inicioKey) return false;
    if (limites.fimKey != null && key > limites.fimKey) return false;
    return true;
  });
}

function obterMunicipiosDoFiltro(filtro, quantidadePadrao){
  var ids = Array.isArray(filtro && filtro.municipios) ? filtro.municipios.slice() : [];
  var validos = ids.filter(function(id){ return !!seriesMunicipio[id]; });
  if (!validos.length && MUNICIPIOS.length){
    var limite = quantidadePadrao != null ? quantidadePadrao : 4;
    validos = MUNICIPIOS.slice(0, limite).map(function(item){ return item.id; });
  }
  return validos;
}

async function garantirDadosIniciais(){
  if (dadosIniciaisCarregados) {
    return;
  }
  await carregarSerieHistorica();
  await carregarMunicipiosESeries();
  atualizarMesesFull();
  dadosIniciaisCarregados = true;
}

async function atualizarSecoesComFiltro(filtro){
  await garantirDadosIniciais();
  var payload = filtro || null;
  if (payload && typeof payload === 'object' && 'trigger' in payload){
    payload = Object.assign({}, payload);
    delete payload.trigger;
  }
  filtroDashboardAtual = payload;
  buildRegional(filtroDashboardAtual);
  buildPeso(filtroDashboardAtual);
  await buildEventos(filtroDashboardAtual);
}

function nomeMunicipio(id){
  var encontrado = MUNICIPIOS.find(function(item){ return item.id === id; });
  return encontrado ? encontrado.nome : id;
}

function coordenadaMunicipio(id){
  if (!id) return null;
  var chave = String(id).toUpperCase();
  if (MUNICIPIOS_COORD_MAP[chave]) {
    return MUNICIPIOS_COORD_MAP[chave];
  }
  var ascii = removerAcentos(chave);
  return MUNICIPIOS_COORD_MAP[ascii] || null;
}

async function carregarSerieHistorica(){
  var dados = [];
  if (SERIE_HISTORICA_URL) {
    try {
      var resposta = await fetch(SERIE_HISTORICA_URL, { headers: { 'Accept': 'application/json' } });
      if (resposta.ok) {
        var corpo = await resposta.json();
        if (Array.isArray(corpo)) {
          dados = corpo;
        }
      }
    } catch (erroSerie) {
      console.error('Falha ao carregar série histórica', erroSerie);
    }
  }

  if (!Array.isArray(dados) || !dados.length) {
    dados = construirSeriePrefetch(PREFETCH_SERIES);
  }

  if (!Array.isArray(dados) || !dados.length) {
    dados = SERIE_HISTORICA_MOCK.slice();
  }

  evolucaoHistorica = normalizarSerie(dados);
}

async function carregarMunicipiosESeries(){
  var nomes = [];
  try {
    var resp = await fetch(MUNICIPIOS_URL, { headers: { 'Accept': 'application/json' } });
    if (resp.ok) {
      var lista = await resp.json();
      if (Array.isArray(lista)) {
        nomes = lista;
      }
    }
  } catch (err) {
    console.error('Falha ao carregar municípios', err);
  }

  if (!Array.isArray(nomes) || !nomes.length) {
    nomes = Array.isArray(PREFETCH_MUNICIPIOS) && PREFETCH_MUNICIPIOS.length ? PREFETCH_MUNICIPIOS.slice() : nomes;
  }

  if (!Array.isArray(nomes) || !nomes.length) {
    var nomesPrefetch = [];
    PREFETCH_SERIES.forEach(function(item){
      if (!item) return;
      if (item.nome) {
        nomesPrefetch.push(item.nome);
      } else if (item.municipio) {
        nomesPrefetch.push(item.municipio);
      }
    });
    if (nomesPrefetch.length) {
      nomes = nomesPrefetch;
    }
  }

  if (!Array.isArray(nomes) || !nomes.length) {
    nomes = MUNICIPIOS_FALLBACK.slice();
  }

  var vistos = new Set();
  MUNICIPIOS = [];
  nomes.forEach(function(nome, idx){
    var id = normalizeMunicipioId(nome);
    if (!id) {
      id = 'M' + idx;
    }
    if (vistos.has(id)) return;
    vistos.add(id);
    var display = (typeof nome === 'string' && nome.trim().length) ? nome.trim() : id;
    MUNICIPIOS.push({ id:id, nome:display });
  });

  var dadosSeries = [];
  try {
    var respSerie = await fetch(SERIE_MUNICIPIOS_URL, { headers: { 'Accept': 'application/json' } });
    if (respSerie.ok) {
      var corpo = await respSerie.json();
      if (Array.isArray(corpo)) {
        dadosSeries = corpo;
      }
    }
  } catch (err) {
    console.error('Falha ao carregar séries por município', err);
  }

  if ((!Array.isArray(dadosSeries) || !dadosSeries.length) && PREFETCH_SERIES.length) {
    dadosSeries = PREFETCH_SERIES.slice();
  }

  seriesMunicipio = {};
  if (Array.isArray(dadosSeries) && dadosSeries.length) {
    dadosSeries.forEach(function(item, idx){
      if (!item) return;
      var id = normalizeMunicipioId(item.id || item.municipio || item.nome);
      if (!id) {
        id = 'M' + idx;
      }
      var displayBase = item.nome || item.municipio;
      var display = (displayBase && displayBase.trim().length) ? displayBase.trim() : id;
      if (!vistos.has(id)) {
        vistos.add(id);
        MUNICIPIOS.push({ id:id, nome:display });
      } else {
        var existente = MUNICIPIOS.find(function(m){ return m.id === id; });
        if (existente && (!existente.nome || existente.nome === existente.id)) {
          existente.nome = display;
        }
      }
      var serieOrdenada = Array.isArray(item.serie) ? item.serie.map(function(p){
        if (!p) return null;
        var chave = normalizarMesParaChave(p.mes || p.periodo || p.data);
        if (!chave) return null;
        var numero = Number(p.cesta != null ? p.cesta : (p.valor != null ? p.valor : p.total));
        if (!Number.isFinite(numero)) return null;
        return { mes: chave, cesta: +numero.toFixed(1) };
      }).filter(Boolean).sort(function(a,b){ return a.mes.localeCompare(b.mes); }) : [];
      seriesMunicipio[id] = serieOrdenada;
    });
  } else {
    criarSeriesMock();
  }

  if (MUNICIPIOS.length) {
    var baseFallback = evolucaoHistorica.length ? evolucaoHistorica : SERIE_HISTORICA_MOCK;
    MUNICIPIOS.forEach(function(m, idx){
      if (!seriesMunicipio[m.id]) {
        var desloc = (idx%3 - 1) * 1.2;
        seriesMunicipio[m.id] = baseFallback.map(function(r){
          return { mes:r.mes, cesta: +(r.cesta + desloc*(Math.random()*4+2)).toFixed(1) };
        });
      }
    });
  }

  MUNICIPIOS.sort(function(a,b){ return a.nome.localeCompare(b.nome, 'pt-BR', { sensitivity:'base' }); });
  atualizarMesesFull();
}

function criarSeriesMock(){
  seriesMunicipio = {};
  var base = evolucaoHistorica.length ? evolucaoHistorica : SERIE_HISTORICA_MOCK;
  MUNICIPIOS.forEach(function(m, idx){
    var desloc = (idx%3 - 1) * 1.2;
    seriesMunicipio[m.id] = base.map(function(r){
      return { mes:r.mes, cesta: +(r.cesta + desloc*(Math.random()*4+2)).toFixed(1) };
    });
  });
  mesesFull = base.map(function(x){ return x.mes; });
}

function atualizarMesesFull(){
  var conjunto = new Set();
  Object.keys(seriesMunicipio).forEach(function(chave){
    var serie = seriesMunicipio[chave];
    if (!Array.isArray(serie)) return;
    serie.forEach(function(p){
      if (p && p.mes != null){
        conjunto.add(String(p.mes));
      }
    });
  });
  if (!conjunto.size && evolucaoHistorica.length){
    evolucaoHistorica.forEach(function(item){ conjunto.add(item.mes); });
  }
  if (!conjunto.size){
    SERIE_HISTORICA_MOCK.forEach(function(item){ conjunto.add(item.mes); });
  }
  mesesFull = Array.from(conjunto).sort();
}

function buildRegional(filtro){
  filtroDashboardAtual = filtro || filtroDashboardAtual || null;
  var ids = obterMunicipiosDoFiltro(filtroDashboardAtual, 4);
  ultimoFiltroRegional = ids.slice();

  var periodo = filtroDashboardAtual ? filtroDashboardAtual.periodo : null;
  var labelsIso = obterMesesDoPeriodo(periodo);
  if (!labelsIso.length){
    labelsIso = SERIE_HISTORICA_MOCK.map(function(x){ return x.mes; });
  }
  var chartLabels = labelsIso.map(function(m){
    var info = extrairAnoMesInfo(m);
    if (!info) return m;
    var dataUtc = new Date(Date.UTC(info.ano, info.mes - 1, 1));
    return MONTH_FORMATTER.format(dataUtc);
  });

  var metricaSelect = document.getElementById('selMetricaRegional');
  var metrica = metricaSelect ? metricaSelect.value : 'cesta';

  var datasetsLinha = ids.map(function(id, idx){
    var serieCompleta = seriesMunicipio[id] || [];
    var serieFiltrada = filtrarSeriePorPeriodo(serieCompleta, periodo);
    var mapa = {};
    serieFiltrada.forEach(function(p){ mapa[p.mes] = p.cesta; });
    var dataValues = labelsIso.map(function(m, index){
      if (metrica === 'var'){
        if (index === 0) return 0;
        var atual = mapa[m];
        var anterior = mapa[labelsIso[index-1]];
        if (atual == null || anterior == null || anterior === 0) return 0;
        return +(((atual - anterior) / anterior) * 100).toFixed(2);
      }
      return mapa[m] != null ? mapa[m] : null;
    });
    return {
      label: nomeMunicipio(id),
      data: dataValues,
      borderColor: COLORS[idx%COLORS.length],
      backgroundColor: COLORS[idx%COLORS.length],
      borderWidth:2, tension:.35,
      spanGaps:true
    };
  });

  var linhaContainer = document.getElementById('chRegionalLinha');
  if (linhaContainer) {
    if (chRegionalLinha && typeof chRegionalLinha.dispose === 'function') {
      chRegionalLinha.dispose();
    }
    linhaContainer.innerHTML = '';

    var formatValorLinha = metrica === 'var'
      ? function(valor){ return Number.isFinite(valor) ? valor.toFixed(2) + '%' : '—'; }
      : function(valor){ return Number.isFinite(valor) ? brl(valor) : '—'; };

    chRegionalLinha = anychart.cartesian();
    chRegionalLinha.animation(true);
    chRegionalLinha.background().fill('transparent');
    chRegionalLinha.xScale().mode('ordinal');
    chRegionalLinha.crosshair().enabled(true).yLabel(false).yStroke(null);
    chRegionalLinha.interactivity().hoverMode('by-x');
    chRegionalLinha.legend().enabled(true);
    chRegionalLinha.legend().position('bottom');
    chRegionalLinha.legend().itemsLayout('horizontal');
    chRegionalLinha.xAxis().labels().fontColor('#64748b');
    chRegionalLinha.yAxis().labels().format(function(){ return formatValorLinha(this.value); }).fontColor('#64748b');
    chRegionalLinha.tooltip().format(function(){
      return this.seriesName + ': ' + formatValorLinha(this.value);
    });

    datasetsLinha.forEach(function(dataset){
      var serieData = chartLabels.map(function(label, idx){
        var valor = dataset.data[idx];
        return {
          x: label,
          value: Number.isFinite(valor) ? valor : null
        };
      });
      var serie = chRegionalLinha.line(serieData);
      serie.name(dataset.label);
      serie.stroke({ color: dataset.borderColor, thickness: 2 });
      serie.hovered().stroke({ color: dataset.borderColor, thickness: 2 });
      serie.markers().enabled(true).type('circle').size(4).fill(dataset.borderColor).stroke('#ffffff');
      serie.hovered().markers().enabled(true).size(6);
    });

    chRegionalLinha.container(linhaContainer);
    chRegionalLinha.draw();
  }

  var barLabels = ids.map(function(id){ return nomeMunicipio(id); });
  var barData = ids.map(function(id){
    var serieCompleta = seriesMunicipio[id] || [];
    var serieFiltrada = filtrarSeriePorPeriodo(serieCompleta, periodo);
    if (!serieFiltrada.length) return 0;
    return serieFiltrada[serieFiltrada.length-1].cesta;
  });

  var barContainer = document.getElementById('chRegionalBar');
  if (barContainer) {
    if (chRegionalBar && typeof chRegionalBar.dispose === 'function') {
      chRegionalBar.dispose();
    }
    barContainer.innerHTML = '';

    chRegionalBar = anychart.column();
    chRegionalBar.animation(true);
    chRegionalBar.background().fill('transparent');
    chRegionalBar.legend(false);
    chRegionalBar.xAxis().labels().fontColor('#64748b');
    chRegionalBar.yAxis().labels().format(function(){
      return Number.isFinite(this.value) ? brl(this.value) : '—';
    }).fontColor('#64748b');
    chRegionalBar.tooltip().format(function(){
      return this.x + ': ' + (Number.isFinite(this.value) ? brl(this.value) : '—');
    });

    var barSeriesData = barLabels.map(function(label, idx){
      var valor = barData[idx];
      return {
        x: label,
        value: Number.isFinite(valor) ? valor : null
      };
    });
    var barSeries = chRegionalBar.column(barSeriesData);
    barSeries.stroke('#22c55e');
    barSeries.fill('#22c55e');

    chRegionalBar.container(barContainer);
    chRegionalBar.draw();
  }

  var tbody = document.getElementById('tbResumoRegional');
  var html = "";
  var tendenciasMunicipios = {};
  ids.forEach(function(id){
    var nome = nomeMunicipio(id);
    var serieCompleta = seriesMunicipio[id] || [];
    var serie = filtrarSeriePorPeriodo(serieCompleta, periodo);
    var custos = serie.map(function(x){return x.cesta;});
    var atual = custos.length ? custos[custos.length-1] : 0;
    var menor = custos.length ? Math.min.apply(null, custos) : 0;
    var maior = custos.length ? Math.max.apply(null, custos) : 0;
    var prev = (custos.length>1? custos[custos.length-2] : atual);
    var varMensal = (custos.length>1 && prev !== 0)? ((atual-prev)/prev)*100 : 0;
    var baseAnual = 0;
    if (custos.length>12) {
      baseAnual = custos[custos.length-13];
    } else if (custos.length>0) {
      baseAnual = custos[0];
    }
    var varAnual  = (baseAnual && baseAnual !== 0)? ((atual-baseAnual)/baseAnual)*100 : 0;
    var tendencia = calcularTendenciaSerie(serie);
    tendenciasMunicipios[id] = tendencia;
    var tendenciaPercent = Number.isFinite(tendencia.percentSlope) ? formatPercent(tendencia.percentSlope, 2) : null;
    var tendenciaHint = null;
    if (tendenciaPercent){
      tendenciaHint = tendenciaPercent + '/mês';
    } else if (tendencia && tendencia.status === 'indefinido'){
      tendenciaHint = 'Histórico insuficiente';
    }

    html += '<tr>'
          + '<td>' + nome + '</td>'
          + '<td class="td-num">' + brl(atual) + '</td>'
          + '<td class="td-num">' + brl(menor) + '</td>'
          + '<td class="td-num">' + brl(maior) + '</td>'
          + '<td class="td-num ' + (varMensal>=0?'text-pos':'text-neg') + '">' + pct(Math.abs(varMensal)) + '</td>'
          + '<td class="td-num ' + (varAnual>=0?'text-pos':'text-neg') + '">' + pct(Math.abs(varAnual)) + '</td>'
          + '<td class="td-num">'
          +   '<span class="trend-indicador ' + (tendencia.classe || '') + '">' + (tendencia.texto || '—') + '</span>'
          +   (tendenciaHint ? '<small class="d-block text-muted">' + tendenciaHint + '</small>' : '')
          + '</td>'
          + '</tr>';
  });
  tbody.innerHTML = html;
  buildRegional.tendencias = tendenciasMunicipios;

  buildMapaMunicipios(ids, periodo);
}

function ensureMapaMunicipios(maps){
  var container = document.getElementById('mapMunicipios');
  if (!container || !maps) return null;
  if (!mapaRegionalMap) {
    mapaRegionalMap = new maps.Map(container, {
      center: { lat: -14.235, lng: -51.9253 },
      zoom: 4,
      mapTypeControl: false,
      streetViewControl: false,
      fullscreenControl: false,
      gestureHandling: 'greedy'
    });
    mapaRegionalMarkers = new Map();
    mapaRegionalOverlays = [];
    mapaRegionalInfoWindow = new maps.InfoWindow();
    mapaRegionalBoundsApplied = false;

    setTimeout(function(){
      maps.event.trigger(mapaRegionalMap, 'resize');
    }, 200);
  }
  return mapaRegionalMap ? { map: mapaRegionalMap, maps: maps } : null;
}

function buildMapaMunicipios(idsSelecionados, periodo){
  var info = document.getElementById('mapMunicipioInfo');
  var selecionados = Array.isArray(idsSelecionados) ? idsSelecionados.slice() : [];
  var periodoLocal = periodo ? Object.assign({}, periodo) : null;

  loadGoogleMapsApi().then(function(maps){
    googleMapsApiInstance = maps;
    var env = ensureMapaMunicipios(maps);
    if (!env) {
      if (info) {
        info.textContent = 'Mapa indisponível no momento.';
      }
      return;
    }

    var mapa = env.map;
    mapaRegionalOverlays.forEach(function(overlay){
      if (overlay && typeof overlay.setMap === 'function') {
        overlay.setMap(null);
      }
    });
    mapaRegionalOverlays = [];
    mapaRegionalMarkers.clear();
    if (mapaRegionalInfoWindow) {
      mapaRegionalInfoWindow.close();
      mapaRegionalInfoWindow.__markerId = null;
    }

    var dados = [];
    var maxValor = 0;

    MUNICIPIOS.forEach(function(m){
      var coords = coordenadaMunicipio(m.id);
      if (!coords) return;
      var serieCompleta = seriesMunicipio[m.id] || [];
      var serieFiltrada = filtrarSeriePorPeriodo(serieCompleta, periodoLocal);
      if (!serieFiltrada.length) return;
      var ultimo = serieFiltrada[serieFiltrada.length-1];
      var valor = ultimo.cesta;
      if (valor == null) return;
      if (valor > maxValor) maxValor = valor;
      var infoMes = extrairAnoMesInfo(ultimo.mes);
      var periodoTexto = infoMes ? MONTH_FORMATTER.format(new Date(Date.UTC(infoMes.ano, infoMes.mes - 1, 1))) : '';
      var tooltipHtml = '<div class="mapa-tooltip"><strong>' + nomeMunicipio(m.id) + '</strong>' +
        (periodoTexto ? '<small>' + periodoTexto + '</small>' : '') +
        '<ul><li>Custo atual: <strong>' + brl(valor) + '</strong></li></ul></div>';

      dados.push({
        id: m.id,
        title: nomeMunicipio(m.id),
        latitude: coords.latitude,
        longitude: coords.longitude,
        value: valor,
        valorFormatado: brl(valor),
        ultimoMes: ultimo.mes,
        tooltipHtml: tooltipHtml
      });
    });

    if (!dados.length) {
      mapaRegionalBoundsApplied = false;
      ultimaSelecaoMapa = null;
      if (info) {
        info.textContent = 'Sem dados georreferenciados para exibir no momento.';
      }
      return;
    }

    var selecionadoAtual = null;
    if (ultimaSelecaoMapa) {
      selecionadoAtual = dados.find(function(item){ return item.id === ultimaSelecaoMapa; }) || null;
      if (!selecionadoAtual) {
        ultimaSelecaoMapa = null;
      }
    }
    if (!selecionadoAtual && selecionados.length) {
      selecionadoAtual = dados.find(function(item){ return selecionados.indexOf(item.id) >= 0; }) || null;
      if (selecionadoAtual) {
        ultimaSelecaoMapa = selecionadoAtual.id;
      }
    }

    var highlightId = selecionadoAtual ? selecionadoAtual.id : null;
    var selecionadosSet = new Set(selecionados);
    var bounds = new maps.LatLngBounds();
    var possuiBounds = false;

    dados.forEach(function(item){
      var proporcao = maxValor ? item.value / maxValor : 1;
      var tamanhoBase = Math.max(8, Math.round(6 + proporcao * 8));
      var ativo = !!highlightId && item.id === highlightId;
      var selecionado = selecionadosSet.has(item.id);
      var cor = '#94a3b8';
      if (selecionado) cor = '#0ea5e9';
      if (ativo) cor = '#22c55e';

      var position = { lat: item.latitude, lng: item.longitude };
      if (Number.isFinite(position.lat) && Number.isFinite(position.lng)) {
        bounds.extend(position);
        possuiBounds = true;
      }

      var icon = {
        path: maps.SymbolPath.CIRCLE,
        scale: ativo ? tamanhoBase + 2 : tamanhoBase,
        fillColor: cor,
        fillOpacity: 0.9,
        strokeColor: cor,
        strokeWeight: ativo ? 3 : 2
      };

      var marker = new maps.Marker({
        position: position,
        map: mapa,
        icon: icon,
        title: item.title,
        zIndex: ativo ? 3 : (selecionado ? 2 : 1)
      });

      marker.addListener('click', function(){
        ultimaSelecaoMapa = item.id;
        buildMapaMunicipios(ultimoFiltroRegional, filtroDashboardAtual ? filtroDashboardAtual.periodo : null);
      });

      marker.addListener('mouseover', function(){
        if (!mapaRegionalInfoWindow) {
          mapaRegionalInfoWindow = new maps.InfoWindow();
        }
        if (mapaRegionalInfoWindow) {
          mapaRegionalInfoWindow.setContent(item.tooltipHtml);
          mapaRegionalInfoWindow.setPosition(position);
          mapaRegionalInfoWindow.open(mapa, marker);
          mapaRegionalInfoWindow.__markerId = item.id;
        }
      });

      marker.addListener('mouseout', function(){
        if (!mapaRegionalInfoWindow) return;
        if (highlightId && mapaRegionalMarkers.has(highlightId)) {
          var destaque = mapaRegionalMarkers.get(highlightId);
          if (destaque && destaque.data && destaque.data.tooltipHtml) {
            mapaRegionalInfoWindow.setContent(destaque.data.tooltipHtml);
            mapaRegionalInfoWindow.setPosition({ lat: destaque.data.latitude, lng: destaque.data.longitude });
            mapaRegionalInfoWindow.open(mapa, destaque.marker);
            mapaRegionalInfoWindow.__markerId = destaque.data.id;
            return;
          }
        }
        if (mapaRegionalInfoWindow.__markerId === item.id) {
          mapaRegionalInfoWindow.close();
          mapaRegionalInfoWindow.__markerId = null;
        }
      });

      mapaRegionalMarkers.set(item.id, { marker: marker, data: item, ativo: ativo });
      mapaRegionalOverlays.push(marker);
    });

    if (possuiBounds && !mapaRegionalBoundsApplied) {
      mapa.fitBounds(bounds, { top: 40, right: 40, bottom: 40, left: 40 });
      mapaRegionalBoundsApplied = true;
    }

    if (highlightId && mapaRegionalMarkers.has(highlightId)) {
      var destaque = mapaRegionalMarkers.get(highlightId);
      if (destaque && destaque.marker && destaque.data && destaque.data.tooltipHtml) {
        if (!mapaRegionalInfoWindow) {
          mapaRegionalInfoWindow = new maps.InfoWindow();
        }
        mapaRegionalInfoWindow.setContent(destaque.data.tooltipHtml);
        mapaRegionalInfoWindow.setPosition({ lat: destaque.data.latitude, lng: destaque.data.longitude });
        mapaRegionalInfoWindow.open(mapa, destaque.marker);
        mapaRegionalInfoWindow.__markerId = destaque.data.id;
      }
    } else if (mapaRegionalInfoWindow) {
      mapaRegionalInfoWindow.close();
      mapaRegionalInfoWindow.__markerId = null;
    }

    if (!highlightId && mapaRegionalInfoWindow && mapaRegionalInfoWindow.__markerId && (!mapaRegionalMarkers.has(mapaRegionalInfoWindow.__markerId) || mapaRegionalInfoWindow.__markerId === null)) {
      mapaRegionalInfoWindow.close();
      mapaRegionalInfoWindow.__markerId = null;
    }

    mapaRegionalMarkers.forEach(function(infoMarker, markerId){
      var selecionado = selecionadosSet.has(markerId);
      var ativo = highlightId === markerId;
      var baseColor = '#94a3b8';
      if (selecionado) baseColor = '#0ea5e9';
      if (ativo) baseColor = '#22c55e';
      var proporcao = maxValor ? (infoMarker.data.value / maxValor) : 1;
      var tamanhoBase = Math.max(8, Math.round(6 + proporcao * 8));
      var iconUpdate = {
        path: maps.SymbolPath.CIRCLE,
        scale: ativo ? tamanhoBase + 2 : tamanhoBase,
        fillColor: baseColor,
        fillOpacity: 0.9,
        strokeColor: baseColor,
        strokeWeight: ativo ? 3 : 2
      };
      infoMarker.marker.setIcon(iconUpdate);
      infoMarker.marker.setZIndex(ativo ? 3 : (selecionado ? 2 : 1));
    });

    if (highlightId && mapaRegionalMarkers.has(highlightId)) {
      var ativoMarker = mapaRegionalMarkers.get(highlightId);
      if (mapaRegionalInfoWindow && ativoMarker && ativoMarker.data && ativoMarker.data.tooltipHtml) {
        mapaRegionalInfoWindow.setContent(ativoMarker.data.tooltipHtml);
        mapaRegionalInfoWindow.setPosition({ lat: ativoMarker.data.latitude, lng: ativoMarker.data.longitude });
        mapaRegionalInfoWindow.open(mapa, ativoMarker.marker);
        mapaRegionalInfoWindow.__markerId = highlightId;
      }
    }

    if (info) {
      if (selecionadoAtual) {
        var infoMes = extrairAnoMesInfo(selecionadoAtual.ultimoMes);
        var periodoTexto = infoMes ? MONTH_FORMATTER.format(new Date(Date.UTC(infoMes.ano, infoMes.mes - 1, 1))) : 'último mês disponível';
        info.innerHTML = '<strong>' + selecionadoAtual.title + '</strong>: ' + selecionadoAtual.valorFormatado + ' (' + periodoTexto + ')';
      } else {
        info.textContent = 'Clique em um município para ver o custo mais recente da cesta básica.';
      }
    }
  }).catch(function(err){
    console.warn('Não foi possível inicializar o mapa regional.', err);
    if (info) {
      info.textContent = 'Mapa indisponível no momento. Atualize a página para tentar novamente.';
    }
  });
}

// ---- Peso dos Itens ----
async function buildPeso(filtro){
  const periodoLabel = document.getElementById('pesoPeriodoDescricao');
  const resumoLabel = document.getElementById('pesoMunicipiosResumo');
  const listaContainer = document.getElementById('listaPesoMunicipios');

  if (periodoLabel) {
    periodoLabel.textContent = 'Carregando...';
  }
  if (resumoLabel) {
    resumoLabel.textContent = '';
  }
  if (listaContainer) {
    listaContainer.innerHTML = '';
    const loading = document.createElement('div');
    loading.className = 'col-12';
    const texto = document.createElement('p');
    texto.className = 'text-muted small mb-0';
    texto.textContent = 'Carregando destaques...';
    loading.appendChild(texto);
    listaContainer.appendChild(loading);
  }

  if (chPesoPizza && typeof chPesoPizza.dispose === 'function') {
    chPesoPizza.dispose();
  }
  chPesoPizza = null;
  if (chPesoBar && typeof chPesoBar.dispose === 'function') {
    chPesoBar.dispose();
  }
  chPesoBar = null;

  let resposta = [];
  if (PESO_MUNICIPIOS_URL) {
    const periodo = filtro ? filtro.periodo : null;
    const limites = obterLimitesPeriodo(periodo);
    const selecionados = obterMunicipiosDoFiltro(filtro, 6);
    const params = new URLSearchParams();
    selecionados.forEach(function(id){ params.append('municipio', id); });
    if (limites.inicioInfo) {
      params.set('mesInicio', limites.inicioInfo.label);
    }
    if (limites.fimInfo) {
      params.set('mesFim', limites.fimInfo.label);
    }
    if (periodo && periodo.anoReferencia != null) {
      params.set('anoRef', periodo.anoReferencia);
    }
    const url = params.toString() ? PESO_MUNICIPIOS_URL + '?' + params.toString() : PESO_MUNICIPIOS_URL;
    try {
      const respostaHttp = await fetch(url, { headers: { 'Accept': 'application/json' } });
      if (respostaHttp.ok) {
        const corpo = await respostaHttp.json();
        resposta = Array.isArray(corpo) ? corpo : [];
      } else {
        resposta = [];
      }
    } catch (erroPeso) {
      console.error('Falha ao carregar peso dos itens', erroPeso);
      resposta = [];
    }
  }

  atualizarResumoPeso(resposta, periodoLabel, resumoLabel);
  renderizarListaPesoMunicipios(resposta, listaContainer);
  const componentes = construirSeriesPeso(resposta);
  desenharGraficosPeso(componentes);
}

function atualizarResumoPeso(municipios, periodoLabel, resumoLabel){
  if (periodoLabel) {
    periodoLabel.textContent = '';
  }
  if (resumoLabel) {
    resumoLabel.textContent = '';
  }

  if (!Array.isArray(municipios) || !municipios.length) {
    if (periodoLabel) {
      periodoLabel.textContent = 'Sem dados disponíveis para o recorte selecionado.';
    }
    return;
  }

  const infosPeriodo = [];
  municipios.forEach(function(item){
    if (!item) return;
    const inicio = extrairAnoMesInfo(item.periodoInicio);
    const fim = extrairAnoMesInfo(item.periodoFim);
    if (inicio) infosPeriodo.push({ tipo: 'inicio', info: inicio });
    if (fim) infosPeriodo.push({ tipo: 'fim', info: fim });
  });

  let menor = null;
  let maior = null;
  infosPeriodo.forEach(function(entry){
    const info = entry.info;
    const key = info.ano * 100 + info.mes;
    if (!menor || key < menor.key) {
      menor = { key: key, info: info };
    }
    if (!maior || key > maior.key) {
      maior = { key: key, info: info };
    }
  });

  if (periodoLabel && (menor || maior)) {
    const inicio = menor ? menor.info : null;
    const fim = maior ? maior.info : null;
    const textoPeriodo = formatarPeriodoIntervalo(inicio, fim);
    periodoLabel.textContent = textoPeriodo || 'Período agregado conforme disponibilidade dos dados.';
  } else if (periodoLabel) {
    periodoLabel.textContent = 'Período dinâmico conforme disponibilidade dos dados.';
  }

  if (resumoLabel) {
    const total = municipios.length;
    resumoLabel.textContent = total === 1
      ? '1 município analisado'
      : total + ' municípios analisados';
  }
}

function formatarPeriodoIntervalo(inicioInfo, fimInfo){
  if (!inicioInfo && !fimInfo) {
    return '';
  }
  if (inicioInfo && fimInfo) {
    const inicioData = new Date(Date.UTC(inicioInfo.ano, inicioInfo.mes - 1, 1));
    const fimData = new Date(Date.UTC(fimInfo.ano, fimInfo.mes - 1, 1));
    const inicioTexto = MONTH_FORMATTER.format(inicioData);
    const fimTexto = MONTH_FORMATTER.format(fimData);
    if (inicioInfo.ano === fimInfo.ano && inicioInfo.mes === fimInfo.mes) {
      return 'Período: ' + inicioTexto;
    }
    return 'Período: ' + inicioTexto + ' — ' + fimTexto;
  }
  const unico = inicioInfo || fimInfo;
  const data = new Date(Date.UTC(unico.ano, unico.mes - 1, 1));
  return 'Período: ' + MONTH_FORMATTER.format(data);
}

function renderizarListaPesoMunicipios(municipios, container){
  if (!container) {
    return;
  }
  container.innerHTML = '';

  if (!Array.isArray(municipios) || !municipios.length) {
    const vazio = document.createElement('div');
    vazio.className = 'col-12';
    const mensagem = document.createElement('p');
    mensagem.className = 'text-muted small mb-0';
    mensagem.textContent = 'Nenhum dado disponível para os filtros selecionados.';
    vazio.appendChild(mensagem);
    container.appendChild(vazio);
    return;
  }

  municipios.forEach(function(item){
    const card = criarPesoMunicipioCard(item);
    if (card) {
      container.appendChild(card);
    }
  });
}

function criarPesoMunicipioCard(item){
  if (!item) {
    return null;
  }

  const col = document.createElement('div');
  col.className = 'col-12 col-md-6 col-xl-4';

  const card = document.createElement('div');
  card.className = 'peso-municipio-card h-100';

  const header = document.createElement('div');
  header.className = 'peso-municipio-card__header';

  const titulo = document.createElement('h4');
  titulo.className = 'peso-municipio-card__title';
  titulo.textContent = item.rotulo || item.nome || 'Município';

  const totalMedioNumero = toFiniteNumber(item.totalMedio);
  const totalBadge = document.createElement('span');
  totalBadge.className = 'peso-municipio-card__badge badge rounded-pill text-bg-light';
  totalBadge.textContent = totalMedioNumero != null ? brl(totalMedioNumero) : '—';

  header.appendChild(titulo);
  header.appendChild(totalBadge);

  const periodoTexto = formatarPeriodoIntervalo(
    extrairAnoMesInfo(item.periodoInicio),
    extrairAnoMesInfo(item.periodoFim)
  );
  const periodo = document.createElement('p');
  periodo.className = 'peso-municipio-card__periodo';
  periodo.textContent = periodoTexto || 'Período dinâmico conforme disponibilidade.';

  const lista = document.createElement('dl');
  lista.className = 'peso-municipio-card__list row g-2';

  const destaques = item.destaques || {};
  adicionarLinhaResumo(lista, 'Item mais caro', formatarDestaqueValor(destaques.itemMaisCaro));
  adicionarLinhaResumo(lista, 'Item mais barato', formatarDestaqueValor(destaques.itemMaisBarato));
  adicionarLinhaResumo(lista, 'Maior aumento', formatarDestaqueVariacao(destaques.maiorAumento));
  adicionarLinhaResumo(lista, 'Maior redução', formatarDestaqueVariacao(destaques.maiorReducao));

  card.appendChild(header);
  card.appendChild(periodo);
  card.appendChild(lista);
  col.appendChild(card);
  return col;
}

function adicionarLinhaResumo(lista, titulo, valor){
  const dt = document.createElement('dt');
  dt.className = 'col-6';
  dt.textContent = titulo;

  const dd = document.createElement('dd');
  dd.className = 'col-6 text-end';

  if (valor && typeof valor === 'object') {
    dd.textContent = valor.texto || '—';
    if (valor.classe) {
      dd.classList.add(valor.classe);
    }
    if (valor.titulo) {
      dd.title = valor.titulo;
    }
  } else {
    dd.textContent = valor || '—';
  }

  lista.appendChild(dt);
  lista.appendChild(dd);
}

function formatarDestaqueValor(item){
  if (!item) {
    return null;
  }
  const partes = [formatComponentLabel(item.chave)];
  const valor = toFiniteNumber(item.valor != null ? item.valor : item.valorFinal);
  const percentual = formatPercent(item.percentual, 1, false);
  if (valor != null) {
    partes.push(brl(valor));
  }
  if (percentual) {
    partes.push(percentual);
  }
  return {
    texto: partes.join(' • '),
    titulo: construirDescricaoPeriodo(item)
  };
}

function formatarDestaqueVariacao(item){
  if (!item) {
    return null;
  }
  const partes = [formatComponentLabel(item.chave)];
  const variacao = formatPercent(item.variacao, 1, true);
  const valorFinal = toFiniteNumber(item.valorFinal);
  if (variacao) {
    partes.push(variacao);
  }
  if (valorFinal != null) {
    partes.push(brl(valorFinal));
  }
  if (!variacao && valorFinal == null && item.percentual != null) {
    const percentual = formatPercent(item.percentual, 1, false);
    if (percentual) {
      partes.push(percentual);
    }
  }
  const variacaoNumero = toFiniteNumber(item.variacao);
  let classe = null;
  if (variacaoNumero != null) {
    if (variacaoNumero > 0) {
      classe = 'peso-municipio-card__var--alta';
    } else if (variacaoNumero < 0) {
      classe = 'peso-municipio-card__var--queda';
    }
  }
  return {
    texto: partes.join(' • '),
    classe: classe,
    titulo: construirDescricaoPeriodo(item)
  };
}

function construirDescricaoPeriodo(item){
  if (!item) {
    return '';
  }
  const inicio = extrairAnoMesInfo(item.mesInicial);
  const fim = extrairAnoMesInfo(item.mesFinal);
  const periodo = formatarPeriodoIntervalo(inicio, fim);
  return periodo.replace(/^Período:\s*/, '');
}

function construirSeriesPeso(municipios){
  if (!Array.isArray(municipios) || !municipios.length) {
    return [];
  }
  const agregados = new Map();
  municipios.forEach(function(item){
    if (!item || !Array.isArray(item.componentes)) {
      return;
    }
    item.componentes.forEach(function(comp){
      if (!comp || !comp.chave) {
        return;
      }
      const chave = String(comp.chave);
      const atual = agregados.get(chave) || { chave: chave, somaPercentual: 0, contagemPercentual: 0, somaMedia: 0, contagemMedia: 0 };
      const percentual = toFiniteNumber(comp.percentual);
      const media = toFiniteNumber(comp.media);
      if (percentual != null) {
        atual.somaPercentual += percentual;
        atual.contagemPercentual += 1;
      }
      if (media != null) {
        atual.somaMedia += media;
        atual.contagemMedia += 1;
      }
      agregados.set(chave, atual);
    });
  });

  const componentes = Array.from(agregados.values()).map(function(item){
    const percentualMedio = item.contagemPercentual > 0 ? item.somaPercentual / item.contagemPercentual : null;
    const mediaValor = item.contagemMedia > 0 ? item.somaMedia / item.contagemMedia : null;
    return {
      chave: item.chave,
      percentual: percentualMedio != null ? Number(percentualMedio.toFixed(2)) : null,
      media: mediaValor != null ? Number(mediaValor.toFixed(2)) : null
    };
  }).filter(function(item){
    return item.percentual != null || item.media != null;
  });

  componentes.sort(function(a, b){
    const pa = a.percentual != null ? a.percentual : -Infinity;
    const pb = b.percentual != null ? b.percentual : -Infinity;
    if (pb !== pa) {
      return pb - pa;
    }
    const ma = a.media != null ? a.media : -Infinity;
    const mb = b.media != null ? b.media : -Infinity;
    return mb - ma;
  });

  return componentes;
}

function desenharGraficosPeso(componentes){
  const pizzaCanvas = document.getElementById('chPesoPizza');
  const barCanvas = document.getElementById('chPesoBar');
  const pizzaWrap = pizzaCanvas ? pizzaCanvas.closest('.chart-wrap') : null;
  const barWrap = barCanvas ? barCanvas.closest('.chart-wrap') : null;

  if (pizzaWrap) {
    pizzaWrap.classList.remove('chart-wrap--empty');
  }
  if (barWrap) {
    barWrap.classList.remove('chart-wrap--empty');
  }

  if (!pizzaCanvas || !barCanvas || !Array.isArray(componentes) || !componentes.length) {
    if (pizzaWrap) {
      pizzaWrap.classList.add('chart-wrap--empty');
    }
    if (barWrap) {
      barWrap.classList.add('chart-wrap--empty');
    }
    return;
  }

  const labels = componentes.map(function(item){ return formatComponentLabel(item.chave); });
  const percentuais = componentes.map(function(item){
    const numero = toFiniteNumber(item.percentual);
    return numero != null ? Number(numero.toFixed(1)) : 0;
  });
  const cores = gerarCores(labels.length);

  pizzaCanvas.innerHTML = '';
  chPesoPizza = anychart.pie();
  chPesoPizza.animation(true);
  chPesoPizza.background().fill('transparent');
  chPesoPizza.palette(cores);
  var pizzaData = componentes.map(function(item, idx){
    return {
      x: labels[idx],
      value: percentuais[idx]
    };
  });
  chPesoPizza.data(pizzaData);
  chPesoPizza.legend().enabled(true).position('bottom');
  chPesoPizza.labels().format(function(){
    return this.x + '\n' + (Number.isFinite(this.value) ? this.value.toFixed(1) + '%' : '—');
  });
  chPesoPizza.tooltip().format(function(){
    return this.x + ': ' + (Number.isFinite(this.value) ? this.value.toFixed(1) + '%' : '—');
  });
  chPesoPizza.container(pizzaCanvas);
  chPesoPizza.draw();

  const topComponentes = componentes.slice(0, Math.min(componentes.length, 8));
  const barLabels = topComponentes.map(function(item){ return formatComponentLabel(item.chave); });
  const barPercentuais = topComponentes.map(function(item){
    const numero = toFiniteNumber(item.percentual);
    return numero != null ? Number(numero.toFixed(1)) : 0;
  });

  barCanvas.innerHTML = '';
  chPesoBar = anychart.bar();
  chPesoBar.animation(true);
  chPesoBar.background().fill('transparent');
  chPesoBar.legend(false);
  chPesoBar.xAxis().labels().fontColor('#475569');
  chPesoBar.yAxis().labels().format(function(){
    return Number.isFinite(this.value) ? this.value.toFixed(1) + '%' : '—';
  }).fontColor('#64748b');
  var barDataPoints = barLabels.map(function(label, idx){
    var valor = barPercentuais[idx];
    return {
      x: label,
      value: Number.isFinite(valor) ? valor : null
    };
  });
  chPesoBar.data(barDataPoints);
  chPesoBar.tooltip().format(function(){
    return this.x + ': ' + (Number.isFinite(this.value) ? this.value.toFixed(1) + '%' : '—');
  });
  chPesoBar.palette(['#f59e0b']);
  chPesoBar.container(barCanvas);
  chPesoBar.draw();
}

// ---- Eventos Externos ----
async function buildEventos(filtro){
  var periodo = filtro ? filtro.periodo : null;
  var limites = obterLimitesPeriodo(periodo);
  var canvasEventos = document.getElementById('chEventos');
  var wrapEventos = canvasEventos ? canvasEventos.closest('.chart-wrap') : null;

  if (wrapEventos) {
    wrapEventos.classList.remove('chart-wrap--empty');
  }

  if (EVENTOS_EXTERNOS_URL) {
    var params = new URLSearchParams();
    if (limites.inicioInfo) {
      params.set('inicio', construirDataIso(limites.inicioInfo.ano, limites.inicioInfo.mes, 1));
    }
    if (limites.fimInfo) {
      params.set('fim', construirDataIso(limites.fimInfo.ano, limites.fimInfo.mes, ultimoDiaDoMes(limites.fimInfo.ano, limites.fimInfo.mes)));
    }

    try {
      var urlEventos = params.toString() ? EVENTOS_EXTERNOS_URL + '?' + params.toString() : EVENTOS_EXTERNOS_URL;
      var resposta = await fetch(urlEventos, { headers: { 'Accept': 'application/json' } });
      if (resposta.ok) {
        var corpo = await resposta.json();
        eventosExternosBase = Array.isArray(corpo) ? corpo : [];
      } else {
        eventosExternosBase = [];
      }
    } catch (erroEventos) {
      console.error('Falha ao carregar eventos externos', erroEventos);
      eventosExternosBase = [];
    }
  }

  eventosExternosCache = Array.isArray(eventosExternosBase) ? eventosExternosBase.slice() : [];
  eventosExternosCache.sort(compararEventosPorPeriodo);

  var serieFiltrada = filtrarSeriePorPeriodo(evolucaoHistorica, periodo);
  if (!serieFiltrada.length && evolucaoHistorica.length) {
    serieFiltrada = evolucaoHistorica.slice();
  }
  if (!serieFiltrada.length && SERIE_HISTORICA_MOCK.length) {
    serieFiltrada = SERIE_HISTORICA_MOCK.slice();
  }

  var labelsIso = serieFiltrada.map(function(r){ return r.mes; });
  var labelKeys = labelsIso.map(function(m){
    var info = extrairAnoMesInfo(m);
    return { raw: m, key: info ? info.label : normalizarPeriodoChave(m) };
  });
  var chartLabels = labelKeys.map(function(item){
    var info = extrairAnoMesInfo(item.raw);
    if (!info) {
      return item.raw;
    }
    var data = new Date(Date.UTC(info.ano, info.mes - 1, 1));
    return MONTH_FORMATTER.format(data);
  });

  var eventosPorIndice = chartLabels.map(function(){ return []; });
  var eventMarkers = [];
  eventosExternosCache.forEach(function(ev){
    var inicio = normalizarPeriodoChave(ev.periodoInicio || ev.dataInicio);
    var fim = normalizarPeriodoChave(ev.periodoFim || ev.periodoInicio || ev.dataFim) || inicio;
    if (!inicio) {
      return;
    }
    if (fim && fim < inicio) {
      var troca = inicio;
      inicio = fim;
      fim = troca;
    }
    var startIndex = labelKeys.findIndex(function(item){ return item.key === inicio; });
    var endIndex = labelKeys.findIndex(function(item){ return item.key === (fim || inicio); });
    if (startIndex === -1 && endIndex === -1) {
      return;
    }
    if (startIndex === -1) {
      startIndex = endIndex;
    }
    if (endIndex === -1) {
      endIndex = startIndex;
    }
    if (startIndex > endIndex) {
      var invert = startIndex;
      startIndex = endIndex;
      endIndex = invert;
    }

    var startLabel = chartLabels[startIndex];
    var endLabel = chartLabels[endIndex];
    if (!startLabel || !endLabel) {
      return;
    }

    var coresImpacto = obterCoresImpacto(ev.impacto);
    var cor = coresImpacto.cor;
    var corTexto = coresImpacto.texto;
    var faixa = coresImpacto.faixa;
    var titulo = ev.titulo || ev.rotulo || 'Evento';
    var labelConteudo = titulo + ' • ' + rotuloImpacto(ev.impacto);

    var tooltipInfo = {
      titulo: titulo,
      impacto: rotuloImpacto(ev.impacto),
      descricao: ev.descricao || ev.comentario || '',
      fonte: ev.fonte || ev.origem || '',
      link: ev.link || ev.url || ''
    };

    for (var idx = startIndex; idx <= endIndex; idx++) {
      if (eventosPorIndice[idx]) {
        eventosPorIndice[idx].push(tooltipInfo);
      }
    }

    eventMarkers.push({
      inicio: startLabel,
      fim: endLabel,
      cor: cor,
      corTexto: corTexto,
      faixa: faixa,
      label: labelConteudo,
      unico: startIndex === endIndex,
      startIndex: startIndex,
      endIndex: endIndex
    });
  });

  var canvasEventos = document.getElementById('chEventos');
  if (canvasEventos) {
    if (!chartLabels.length) {
      if (wrapEventos) {
        wrapEventos.classList.add('chart-wrap--empty');
      }
      if (chEventos && typeof chEventos.dispose === 'function') {
        chEventos.dispose();
        chEventos = null;
      }
    } else {
      if (chEventos && typeof chEventos.dispose === 'function') {
        chEventos.dispose();
      }
      canvasEventos.innerHTML = '';

      var formatarEventosTooltip = function(eventos){
        if (!Array.isArray(eventos) || !eventos.length) {
          return '';
        }
        return eventos.map(function(info){
          var partes = [info.titulo + ' • ' + info.impacto];
          var detalhes = [];
          if (info.descricao) detalhes.push(info.descricao);
          if (info.fonte) detalhes.push('Fonte: ' + info.fonte);
          if (info.link) detalhes.push('Mais informações: ' + info.link);
          if (detalhes.length) {
            partes.push(detalhes.join('<br/>'));
          }
          return partes.join('<br/>');
        }).join('<br/><br/>');
      };

      chEventos = anychart.cartesian();
      chEventos.animation(true);
      chEventos.background().fill('transparent');
      chEventos.xScale().mode('ordinal');
      chEventos.crosshair().enabled(true).yLabel(false).yStroke(null);
      chEventos.interactivity().hoverMode('by-x');
      chEventos.legend(false);
      chEventos.xAxis().labels().fontColor('#64748b');
      chEventos.yAxis().labels().format(function(){
        return Number.isFinite(this.value) ? brl(this.value) : '—';
      }).fontColor('#64748b');

      var serieData = serieFiltrada.map(function(r, idx){
        var valor = r.cesta;
        return {
          x: chartLabels[idx],
          value: Number.isFinite(valor) ? valor : null,
          eventos: eventosPorIndice[idx]
        };
      });

      var valoresPorLabel = serieData.reduce(function(acumulado, ponto){
        if (ponto && ponto.x) {
          acumulado[ponto.x] = ponto.value;
        }
        return acumulado;
      }, {});

      eventMarkers.forEach(function(markerInfo){
        if (typeof markerInfo.startIndex !== 'number' || typeof markerInfo.endIndex !== 'number') {
          return;
        }
        var areaData = chartLabels.map(function(label, idx){
          if (idx < markerInfo.startIndex || idx > markerInfo.endIndex) {
            return { x: label, value: null };
          }
          var valor = valoresPorLabel[label];
          return { x: label, value: Number.isFinite(valor) ? valor : null };
        });
        var sombraSerie = chEventos.area(areaData);
        sombraSerie.fill(markerInfo.faixa);
        sombraSerie.stroke(null);
        if (typeof sombraSerie.hovered === 'function') {
          sombraSerie.hovered().fill(markerInfo.faixa);
          sombraSerie.hovered().stroke(null);
        }
        sombraSerie.tooltip(false);
        sombraSerie.zIndex(1);
      });

      var serieEventos = chEventos.line(serieData);
      serieEventos.name('Cesta (R$)');
      serieEventos.stroke({ color: '#0f172a', thickness: 2 });
      serieEventos.hovered().stroke({ color: '#0f172a', thickness: 2 });
      serieEventos.markers().enabled(true).type('circle').size(4).fill('#0f172a').stroke('#ffffff');
      serieEventos.hovered().markers().enabled(true).size(6);
      serieEventos.zIndex(3);

      chEventos.tooltip().useHtml(true);
      chEventos.tooltip().format(function(){
        var valor = Number.isFinite(this.value) ? brl(this.value) : '—';
        var eventos = typeof this.getData === 'function' ? this.getData('eventos') : null;
        var extras = formatarEventosTooltip(eventos);
        if (extras) {
          return this.seriesName + ': ' + valor + '<br/>' + extras;
        }
        return this.seriesName + ': ' + valor;
      });

      eventMarkers.forEach(function(markerInfo){
        var marker = chEventos.rangeMarker();
        marker.from(markerInfo.inicio);
        if (markerInfo.inicio === markerInfo.fim) {
          var idxAtual = chartLabels.indexOf(markerInfo.fim);
          var proximo = chartLabels[idxAtual + 1] || markerInfo.fim;
          marker.to(proximo);
          marker.stroke({ color: markerInfo.cor, thickness: 2, dash: '6 6' });
          marker.fill('transparent');
        } else {
          marker.to(markerInfo.fim);
          marker.fill(markerInfo.faixa);
          marker.stroke({ color: markerInfo.cor, thickness: 1 });
        }
        marker.zIndex(0);
        if (typeof marker.labels === 'function') {
          marker.labels().enabled(true);
          marker.labels().useHtml(true);
          marker.labels().format(markerInfo.label);
          marker.labels().fontColor(markerInfo.corTexto);
          marker.labels().background({ fill: 'rgba(255,255,255,0.85)', stroke: markerInfo.cor });
          marker.labels().padding(5);
          marker.labels().anchor('left-top');
          marker.labels().offsetY(5);
        }
      });

      chEventos.container(canvasEventos);
      chEventos.draw();
    }
  }

  var eventosVisiveis = eventosExternosCache.filter(function(ev){
    var inicio = normalizarPeriodoChave(ev.periodoInicio || ev.dataInicio);
    var fim = normalizarPeriodoChave(ev.periodoFim || ev.periodoInicio || ev.dataFim) || inicio;
    if (!inicio) return true;
    if (fim < inicio) {
      var troca = inicio;
      inicio = fim;
      fim = troca;
    }
    if (limites.inicioKey != null && fim != null && fim < limites.inicioKey) return false;
    if (limites.fimKey != null && inicio != null && inicio > limites.fimKey) return false;
    return true;
  });

  var ul = document.getElementById('listaEventos');
  if (ul) {
    if (!eventosVisiveis.length) {
      ul.innerHTML = '<li class="text-muted">Nenhum evento registrado para o recorte selecionado.</li>';
    } else {
      var html = '';
      eventosVisiveis.forEach(function(ev){
        var periodoTexto = formatarPeriodoEvento(ev);
        var descricao = ev.descricao || ev.comentario || '';
        var impactoRotulo = rotuloImpacto(ev.impacto);
        var impactoCores = obterCoresImpacto(ev.impacto);
        html += '<li class="d-flex justify-content-between align-items-start flex-wrap gap-2">';
        html += '<div class="me-2">';
        html += '<strong>' + periodoTexto + ' – ' + (ev.titulo || ev.rotulo || 'Evento') + '</strong>';
        html += '<div class="small"><span class="badge ' + impactoCores.badge + '">' + impactoRotulo + '</span></div>';
        if (descricao) {
          html += '<div class="small text-muted">' + descricao + '</div>';
        }
        html += '</div>';
        html += '</li>';
      });
      ul.innerHTML = html;
    }
  }
}

async function initDashboard(){
  var metricaSelect = document.getElementById('selMetricaRegional');
  if (metricaSelect){
    metricaSelect.addEventListener('change', function(){
      if (!dadosIniciaisCarregados) {
        return;
      }
      buildRegional(filtroDashboardAtual);
    });
  }

  var resetMapaRegional = document.getElementById('mapRegionalReset');
  if (resetMapaRegional) {
    resetMapaRegional.addEventListener('click', function(){
      if (!dadosIniciaisCarregados) {
        return;
      }
      mapaRegionalBoundsApplied = false;
      if (mapaRegionalMap) {
        mapaRegionalMap.invalidateSize();
      }
      buildMapaMunicipios(ultimoFiltroRegional, filtroDashboardAtual ? filtroDashboardAtual.periodo : null);
    });
  }

  const aplicarAtualizacaoFiltro = function(filtro){
    if (!filtro) {
      return;
    }
    atualizarSecoesComFiltro(filtro).catch(function(err){
      console.error('Falha ao atualizar o painel com os filtros selecionados', err);
    });
  };

  if (window.DashboardFilter) {
    if (typeof window.DashboardFilter.subscribe === 'function'){
      window.DashboardFilter.subscribe(aplicarAtualizacaoFiltro);
    }
    if (typeof window.DashboardFilter.getState === 'function') {
      var estadoInicial = window.DashboardFilter.getState();
      if (estadoInicial) {
        aplicarAtualizacaoFiltro(estadoInicial);
      }
    }
  }
}

const bootstrapDashboardWhenAnychartReady = (maxRetries = 50, delayMs = 100) => {
  let attempts = 0;
  let started = false;

  const start = () => {
    if (started) {
      return;
    }
    started = true;
    initDashboard();
  };

  const waitForAnychart = () => {
    const anychartLoaded =
      typeof window !== "undefined" &&
      window.anychart &&
      (typeof window.anychart.onDocumentReady === "function" || typeof window.anychart.map === "function");

    if (anychartLoaded) {
      if (typeof window.anychart.onDocumentReady === "function") {
        window.anychart.onDocumentReady(start);
      } else {
        start();
      }
      return;
    }

    if (attempts < maxRetries) {
      attempts += 1;
      window.setTimeout(waitForAnychart, delayMs);
      return;
    }

    console.warn(
      "AnyChart não carregou dentro do tempo esperado. Inicializando o painel mesmo assim; alguns recursos podem ficar indisponíveis."
    );
    start();
  };

  waitForAnychart();
};

if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", () => bootstrapDashboardWhenAnychartReady());
} else {
  bootstrapDashboardWhenAnychartReady();
}
