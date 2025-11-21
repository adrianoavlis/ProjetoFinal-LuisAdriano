package br.com.shop2.domain.service;

import br.com.shop2.domain.repository.EvolucaoRepository;
import br.com.shop2.model.common.CestaBasicaComponentePesoDTO;
import br.com.shop2.model.common.CestaBasicaPesoDestaquesDTO;
import br.com.shop2.model.common.CestaBasicaPesoMunicipioDTO;
import br.com.shop2.model.common.CestaBasicaPesoResumoItemDTO;
import br.com.shop2.model.common.CestaBasicaSerieDTO;
import br.com.shop2.model.common.CestaBasicaSerieMunicipioDTO;
import br.com.shop2.model.common.EvolucaoFiltro;
import br.com.shop2.model.common.EvolucaoIndicadorPrecoDTO;
import br.com.shop2.model.common.EvolucaoIndicadoresDTO;
import br.com.shop2.model.common.EvolucaoMunicipioDTO;
import br.com.shop2.model.common.EvolucaoTendenciaDTO;
import br.com.shop2.model.common.EvolucaoVariacaoDTO;
import br.com.shop2.model.common.PeriodosDisponiveisDTO;
import br.com.shop2.model.dados.GastoMensal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Collator;
import java.text.Normalizer;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EvolucaoService {

    private final EvolucaoRepository evolucaoRepository;

    private static final Locale LOCALE_PT_BR = Locale.of("pt", "BR");

    private static final DateTimeFormatter FRONT_YEAR_MONTH_FORMATTER =
        new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("MMM-uuuu")
            .toFormatter(Locale.ENGLISH);

    private static final DateTimeFormatter MES_LONGO_FORMATTER =
        new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("MMMM 'de' uuuu")
            .toFormatter(LOCALE_PT_BR);

    private static final DateTimeFormatter YEAR_MONTH_NUMERIC =
        DateTimeFormatter.ofPattern("MM/yyyy", LOCALE_PT_BR);

    private static final List<DateTimeFormatter> YEAR_MONTH_FORMATS = List.of(
        new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("uuuu-MM").toFormatter(),
        new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("uuuu/MM").toFormatter(),
        new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MM/uuuu").toFormatter(),
        new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("M/uuuu").toFormatter(),
        new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MMuuuu").toFormatter(),
        new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("uuuuMM").toFormatter(),
        new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MM-uuuu").toFormatter(),
        new DateTimeFormatterBuilder().parseCaseInsensitive()
            .appendPattern("MMM/uuuu")
            .toFormatter(Locale.of("pt", "BR"))
            .withResolverStyle(ResolverStyle.LENIENT)
    );

    private static final Map<String, Integer> PORTUGUESE_MONTHS = Map.ofEntries(
        Map.entry("jan", 1), Map.entry("janeiro", 1),
        Map.entry("fev", 2), Map.entry("fevereiro", 2),
        Map.entry("mar", 3), Map.entry("marco", 3), Map.entry("março", 3),
        Map.entry("abr", 4), Map.entry("abril", 4),
        Map.entry("mai", 5), Map.entry("maio", 5),
        Map.entry("jun", 6), Map.entry("junho", 6),
        Map.entry("jul", 7), Map.entry("julho", 7),
        Map.entry("ago", 8), Map.entry("agosto", 8),
        Map.entry("set", 9), Map.entry("setembro", 9),
        Map.entry("out", 10), Map.entry("outubro", 10),
        Map.entry("nov", 11), Map.entry("novembro", 11),
        Map.entry("dez", 12), Map.entry("dezembro", 12)
    );

    private static final Pattern UF_PARENTESIS = Pattern.compile("(.+?)\\(([^)\\s]{2})\\)\\s*$");
    private static final Pattern UF_HIFEN = Pattern.compile("(.+?)[\\s\\-]+([A-Za-z]{2})$");
    private static final char APOSTROFO = '\'';

    private static final List<ComponenteExtractor> COMPONENTES = List.of(
        new ComponenteExtractor("carne", GastoMensal::getCarne),
        new ComponenteExtractor("leite", GastoMensal::getLeite),
        new ComponenteExtractor("feijao", GastoMensal::getFeijao),
        new ComponenteExtractor("arroz", GastoMensal::getArroz),
        new ComponenteExtractor("farinha", GastoMensal::getFarinha),
        new ComponenteExtractor("batata", GastoMensal::getBatata),
        new ComponenteExtractor("tomate", GastoMensal::getTomate),
        new ComponenteExtractor("pao", GastoMensal::getPao),
        new ComponenteExtractor("cafe", GastoMensal::getCafe),
        new ComponenteExtractor("banana", GastoMensal::getBanana),
        new ComponenteExtractor("acucar", GastoMensal::getAcucar),
        new ComponenteExtractor("oleo", GastoMensal::getOleo),
        new ComponenteExtractor("manteiga", GastoMensal::getManteiga)
    );

    public List<EvolucaoMunicipioDTO> evolucaoMunicipios() {
        return evolucaoMunicipios(EvolucaoFiltro.builder().build());
    }

    public List<EvolucaoMunicipioDTO> evolucaoMunicipios(Collection<String> municipios) {
        return evolucaoMunicipios(construirFiltro(municipios));
    }

    public List<EvolucaoMunicipioDTO> evolucaoMunicipios(EvolucaoFiltro filtro) {
        EvolucaoContext contexto = prepararContexto(filtro);
        Collator collator = Collator.getInstance(LOCALE_PT_BR);
        collator.setStrength(Collator.PRIMARY);
        return contexto.municipios().stream()
            .map(this::converterParaEvolucao)
            .filter(Objects::nonNull)
            .sorted((a, b) -> collator.compare(a.getNome(), b.getNome()))
            .toList();
    }

    public List<CestaBasicaSerieMunicipioDTO> serieHistoricaPorMunicipio(Collection<String> municipios) {
        return serieHistoricaPorMunicipio(construirFiltro(municipios));
    }

    public List<CestaBasicaSerieMunicipioDTO> serieHistoricaPorMunicipio(EvolucaoFiltro filtro) {
        EvolucaoContext contexto = prepararContexto(filtro);
        return contexto.municipios().stream()
            .map(MunicipioSerieResultado::toSerieMunicipioDto)
            .toList();
    }

    public EvolucaoIndicadoresDTO calcularIndicadores(EvolucaoFiltro filtro) {
        EvolucaoContext contexto = prepararContexto(filtro);
        List<MunicipioSerieResultado> municipios = contexto.municipios();
        if (municipios.isEmpty()) {
            return EvolucaoIndicadoresDTO.builder()
                .menorPreco(null)
                .variacaoMensal(EvolucaoVariacaoDTO.builder()
                    .percentual(null)
                    .descricao("Sem dados mensais suficientes.")
                    .municipiosConsiderados(0)
                    .build())
                .variacaoAnual(EvolucaoVariacaoDTO.builder()
                    .percentual(null)
                    .descricao("Sem dados anuais suficientes.")
                    .municipiosConsiderados(0)
                    .build())
                .tendencia(EvolucaoTendenciaDTO.builder()
                    .percentual(null)
                    .texto("Informações insuficientes para tendência.")
                    .descricao("Adicione municípios ou ajuste o período para estimar a tendência.")
                    .classe("text-muted")
                    .status("INDEFINIDO")
                    .municipiosConsiderados(0)
                    .build())
                .build();
        }

        EvolucaoIndicadorPrecoDTO menorPreco = calcularMenorPrecoIndicador(contexto);
        Integer anoReferencia = anoReferenciaAplicavel(contexto.filtro());
        if (anoReferencia == null && contexto.filtro().getMesInicio() != null) {
            anoReferencia = contexto.filtro().getMesInicio().getYear();
        }

        EvolucaoVariacaoDTO variacaoMensal = calcularVariacaoMensalIndicador(municipios);
        EvolucaoVariacaoDTO variacaoAnual = calcularVariacaoAnualIndicador(municipios, anoReferencia);
        EvolucaoTendenciaDTO tendencia = calcularTendenciaIndicador(municipios);

        return EvolucaoIndicadoresDTO.builder()
            .menorPreco(menorPreco)
            .variacaoMensal(variacaoMensal)
            .variacaoAnual(variacaoAnual)
            .tendencia(tendencia)
            .build();
    }

    public List<CestaBasicaPesoMunicipioDTO> calcularPesoComponentes(EvolucaoFiltro filtro) {
        EvolucaoContext contexto = prepararContexto(filtro);
        return contexto.municipios().stream()
            .map(this::construirPesoMunicipio)
            .filter(Objects::nonNull)
            .toList();
    }

    public List<String> listarMunicipiosDisponiveis() {
        List<GastoMensal> registros = evolucaoRepository.listarTodos();
        if (registros == null || registros.isEmpty()) {
            return List.of();
        }

        Collator collator = Collator.getInstance(LOCALE_PT_BR);
        collator.setStrength(Collator.PRIMARY);

        Map<String, MunicipioInfo> porId = new LinkedHashMap<>();
        for (GastoMensal gasto : registros) {
            if (gasto == null) {
                continue;
            }
            MunicipioInfo info = extrairMunicipioInfo(gasto.getMunicipio());
            if (info == null || info.id() == null || info.id().isBlank()) {
                continue;
            }
            porId.putIfAbsent(info.id(), info);
        }

        return porId.values().stream()
            .map(info -> {
                String nome = info.displayName();
                String uf = info.uf();
                if (nome == null || nome.isBlank()) {
                    nome = info.id();
                }
                if (uf == null || uf.isBlank()) {
                    return nome;
                }
                return nome + " / " + uf;
            })
            .sorted(collator::compare)
            .toList();
    }

    public PeriodosDisponiveisDTO listarPeriodosDisponiveis() {
        SortedMap<YearMonth, DoubleSummaryStatistics> agregados =
            agruparMediaTotalPorMes(evolucaoRepository.listarTodos());
        if (agregados.isEmpty()) {
            return periodosVazios();
        }

        SortedSet<YearMonth> periodosOrdenados = new TreeSet<>(agregados.keySet());
        Map<Integer, List<String>> mesesPorAno = new LinkedHashMap<>();
        for (YearMonth anoMes : periodosOrdenados) {
            mesesPorAno.computeIfAbsent(anoMes.getYear(), chave -> new ArrayList<>()).add(anoMes.toString());
        }

        List<Integer> anosOrdenados = new ArrayList<>(mesesPorAno.keySet());
        List<String> mesesOrdenados = periodosOrdenados.stream()
            .map(YearMonth::toString)
            .toList();

        return PeriodosDisponiveisDTO.builder()
            .anos(anosOrdenados)
            .meses(mesesOrdenados)
            .mesesPorAno(mesesPorAno)
            .build();
    }

    public YearMonth interpretarMesAno(String raw) {
        return parseYearMonth(raw);
    }

    private EvolucaoContext prepararContexto(EvolucaoFiltro filtro) {
        EvolucaoFiltro base = filtro == null ? EvolucaoFiltro.builder().build() : filtro;
        EvolucaoFiltro normalizado = base.limparMunicipiosVazios().normalizarMunicipios(this::normalizarMunicipioKey);

        Set<String> filtroMunicipios = normalizado.getMunicipios();
        List<GastoMensal> registros = evolucaoRepository.buscarPorFiltro(normalizado);
        if (registros == null || registros.isEmpty()) {
            return new EvolucaoContext(normalizado, List.of());
        }

        YearMonth mesInicio = normalizado.getMesInicio();
        YearMonth mesFim = normalizado.getMesFim();
        Integer anoReferencia = anoReferenciaAplicavel(normalizado);

        Map<String, MunicipioSerieAcumulador> agrupado = new LinkedHashMap<>();
        for (GastoMensal gasto : registros) {
            if (gasto == null) {
                continue;
            }
            YearMonth mes = gasto.getMesAno();
            if (mes == null) {
                continue;
            }
            if (mesInicio != null && mes.isBefore(mesInicio)) {
                continue;
            }
            if (mesFim != null && mes.isAfter(mesFim)) {
                continue;
            }
            if (anoReferencia != null && mes.getYear() != anoReferencia) {
                continue;
            }
            MunicipioInfo info = extrairMunicipioInfo(gasto.getMunicipio());
            if (info == null || info.id() == null) {
                continue;
            }
            String chaveNormalizada = normalizarMunicipioKey(info.id());
            if (!filtroMunicipios.isEmpty() && !filtroMunicipios.contains(chaveNormalizada)) {
                continue;
            }
            agrupado
                .computeIfAbsent(info.id(), chave -> new MunicipioSerieAcumulador(info))
                .adicionar(gasto);
        }

        Collator collator = Collator.getInstance(LOCALE_PT_BR);
        collator.setStrength(Collator.PRIMARY);

        List<MunicipioSerieResultado> municipios = agrupado.values().stream()
            .map(MunicipioSerieAcumulador::toResultado)
            .filter(Objects::nonNull)
            .sorted((a, b) -> collator.compare(a.info().displayName(), b.info().displayName()))
            .toList();

        return new EvolucaoContext(normalizado, municipios);
    }

    private EvolucaoMunicipioDTO converterParaEvolucao(MunicipioSerieResultado resultado) {
        if (resultado == null || resultado.serie() == null || resultado.serie().isEmpty()) {
            return null;
        }

        MunicipioInfo info = resultado.info();
        if (info == null || info.id() == null || info.id().isBlank()) {
            return null;
        }

        String nomeDisplay = (info.displayName() == null || info.displayName().isBlank())
            ? info.id()
            : info.displayName();

        List<CestaBasicaSerieDTO> serie = resultado.serie().stream()
            .map(SerieMensal::toDto)
            .toList();

        return EvolucaoMunicipioDTO.builder()
            .id(info.id())
            .nome(nomeDisplay)
            .uf(info.uf())
            .serie(serie)
            .build();
    }

    private CestaBasicaPesoMunicipioDTO construirPesoMunicipio(MunicipioSerieResultado resultado) {
        if (resultado == null || resultado.serie() == null || resultado.serie().isEmpty()) {
            return null;
        }

        List<SerieMensal> serie = resultado.serie();
        Map<String, ComponentAggregator> componentes = new LinkedHashMap<>();
        YearMonth primeiroMes = null;
        YearMonth ultimoMes = null;

        for (SerieMensal ponto : serie) {
            if (ponto == null) {
                continue;
            }
            YearMonth mes = ponto.mes();
            if (mes != null) {
                if (primeiroMes == null || mes.isBefore(primeiroMes)) {
                    primeiroMes = mes;
                }
                if (ultimoMes == null || mes.isAfter(ultimoMes)) {
                    ultimoMes = mes;
                }
            }
            Map<String, Double> valores = ponto.componentes();
            if (valores == null || valores.isEmpty()) {
                continue;
            }
            for (Map.Entry<String, Double> entry : valores.entrySet()) {
                String chave = entry.getKey();
                Double valor = entry.getValue();
                if (chave == null || valor == null) {
                    continue;
                }
                ComponentAggregator agregado = componentes.computeIfAbsent(chave, key -> new ComponentAggregator());
                agregado.adicionar(valor, mes);
            }
        }

        double mediaTotal = serie.stream()
            .mapToDouble(SerieMensal::total)
            .average()
            .orElse(0.0);

        List<CestaBasicaComponentePesoDTO> componentesDto = componentes.entrySet().stream()
            .map(entry -> criarComponentePeso(entry.getKey(), entry.getValue(), mediaTotal))
            .filter(Objects::nonNull)
            .sorted((a, b) -> {
                Double p1 = a.getPercentual();
                Double p2 = b.getPercentual();
                if (p1 == null && p2 == null) {
                    return 0;
                }
                if (p1 == null) {
                    return 1;
                }
                if (p2 == null) {
                    return -1;
                }
                int percentualCompare = Double.compare(p2, p1);
                if (percentualCompare != 0) {
                    return percentualCompare;
                }
                Double mediaA = a.getMedia();
                Double mediaB = b.getMedia();
                if (mediaA == null && mediaB == null) {
                    return 0;
                }
                if (mediaA == null) {
                    return 1;
                }
                if (mediaB == null) {
                    return -1;
                }
                return Double.compare(mediaB, mediaA);
            })
            .toList();

        CestaBasicaPesoDestaquesDTO destaques = construirDestaques(componentesDto);
        MunicipioInfo info = resultado.info();

        return CestaBasicaPesoMunicipioDTO.builder()
            .id(info != null ? info.id() : null)
            .nome(info != null ? info.displayName() : null)
            .uf(info != null ? info.uf() : null)
            .rotulo(construirRotuloMunicipio(info))
            .periodoInicio(formatYearMonthForFrontend(primeiroMes))
            .periodoFim(formatYearMonthForFrontend(ultimoMes))
            .totalMedio(mediaTotal > 0 ? arredondarDuasCasas(mediaTotal) : null)
            .componentes(componentesDto.isEmpty() ? List.of() : componentesDto)
            .destaques(destaques)
            .build();
    }

    private CestaBasicaComponentePesoDTO criarComponentePeso(String chave,
                                                             ComponentAggregator agregado,
                                                             double mediaTotal) {
        if (chave == null || agregado == null || agregado.getQuantidade() == 0) {
            return null;
        }

        double media = agregado.getSoma() / agregado.getQuantidade();
        Double percentual = null;
        if (mediaTotal > 0) {
            percentual = arredondarUmaCasa((media / mediaTotal) * 100.0);
        }

        Double variacao = null;
        Double primeiroValor = agregado.getPrimeiroValor();
        Double ultimoValor = agregado.getUltimoValor();
        if (primeiroValor != null && ultimoValor != null && primeiroValor != 0.0) {
            variacao = arredondarUmaCasa(((ultimoValor - primeiroValor) / primeiroValor) * 100.0);
        }

        Double valorInicial = primeiroValor != null ? arredondarDuasCasas(primeiroValor) : null;
        Double valorFinal = ultimoValor != null ? arredondarDuasCasas(ultimoValor) : null;

        return CestaBasicaComponentePesoDTO.builder()
            .chave(chave)
            .media(arredondarDuasCasas(media))
            .percentual(percentual)
            .variacao(variacao)
            .valorInicial(valorInicial)
            .valorFinal(valorFinal)
            .mesInicial(formatYearMonthForFrontend(agregado.getPrimeiroMes()))
            .mesFinal(formatYearMonthForFrontend(agregado.getUltimoMes()))
            .build();
    }

    private CestaBasicaPesoDestaquesDTO construirDestaques(List<CestaBasicaComponentePesoDTO> componentes) {
        if (componentes == null || componentes.isEmpty()) {
            return null;
        }

        CestaBasicaComponentePesoDTO maiorValor = componentes.stream()
            .filter(componente -> componente.getMedia() != null)
            .max(Comparator.comparing(CestaBasicaComponentePesoDTO::getMedia))
            .orElse(null);

        CestaBasicaComponentePesoDTO menorValor = componentes.stream()
            .filter(componente -> componente.getMedia() != null)
            .min(Comparator.comparing(CestaBasicaComponentePesoDTO::getMedia))
            .orElse(null);

        CestaBasicaComponentePesoDTO maiorAlta = componentes.stream()
            .filter(componente -> componente.getVariacao() != null && componente.getVariacao() > 0)
            .max(Comparator.comparing(CestaBasicaComponentePesoDTO::getVariacao))
            .orElse(null);

        CestaBasicaComponentePesoDTO maiorReducao = componentes.stream()
            .filter(componente -> componente.getVariacao() != null && componente.getVariacao() < 0)
            .min(Comparator.comparing(CestaBasicaComponentePesoDTO::getVariacao))
            .orElse(null);

        if (maiorValor == null && menorValor == null && maiorAlta == null && maiorReducao == null) {
            return null;
        }

        return CestaBasicaPesoDestaquesDTO.builder()
            .itemMaisCaro(criarResumo(maiorValor))
            .itemMaisBarato(criarResumo(menorValor))
            .maiorAumento(criarResumo(maiorAlta))
            .maiorReducao(criarResumo(maiorReducao))
            .build();
    }

    private CestaBasicaPesoResumoItemDTO criarResumo(CestaBasicaComponentePesoDTO componente) {
        if (componente == null) {
            return null;
        }

        Double valorPreferencial = componente.getValorFinal() != null
            ? componente.getValorFinal()
            : componente.getMedia();

        return CestaBasicaPesoResumoItemDTO.builder()
            .chave(componente.getChave())
            .valor(valorPreferencial)
            .percentual(componente.getPercentual())
            .variacao(componente.getVariacao())
            .valorInicial(componente.getValorInicial())
            .valorFinal(componente.getValorFinal())
            .mesInicial(componente.getMesInicial())
            .mesFinal(componente.getMesFinal())
            .build();
    }

    private EvolucaoIndicadorPrecoDTO calcularMenorPrecoIndicador(EvolucaoContext contexto) {
        Set<String> nomesOrigem = contexto.nomesOrigem().stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(valor -> !valor.isEmpty())
            .map(valor -> valor.toUpperCase(Locale.ROOT))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        boolean semMunicipios = nomesOrigem.isEmpty();
        Collection<String> consultaMunicipios = semMunicipios
            ? List.of("__ALL__")
            : nomesOrigem;

        Integer anoReferencia = anoReferenciaAplicavel(contexto.filtro());

        List<GastoMensal> registros = evolucaoRepository.buscarMenorPreco(
            consultaMunicipios,
            semMunicipios,
            contexto.filtro().getMesInicio(),
            contexto.filtro().getMesFim(),
            anoReferencia
        );

        if (registros != null && !registros.isEmpty()) {
            for (GastoMensal registro : registros) {
                if (registro == null) {
                    continue;
                }
                BigDecimal total = registro.getTotalCesta();
                if (total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                MunicipioInfo info = extrairMunicipioInfo(registro.getMunicipio());
                String nomeMunicipio = info != null && info.displayName() != null && !info.displayName().isBlank()
                    ? info.displayName()
                    : registro.getMunicipio();
                String municipioId = info != null ? info.id() : null;
                return construirIndicadorPreco(nomeMunicipio, municipioId, registro.getMesAno(), total);
            }
        }

        SerieMensal menorSerie = null;
        MunicipioSerieResultado municipioMenor = null;
        for (MunicipioSerieResultado municipio : contexto.municipios()) {
            for (SerieMensal ponto : municipio.serie()) {
                if (ponto == null || ponto.total() <= 0) {
                    continue;
                }
                if (menorSerie == null || ponto.total() < menorSerie.total()) {
                    menorSerie = ponto;
                    municipioMenor = municipio;
                }
            }
        }

        if (menorSerie == null || municipioMenor == null) {
            return null;
        }

        MunicipioInfo info = municipioMenor.info();
        String nomeMunicipio = info != null && info.displayName() != null && !info.displayName().isBlank()
            ? info.displayName()
            : (info != null ? info.id() : null);
        BigDecimal valor = BigDecimal.valueOf(menorSerie.total());
        return construirIndicadorPreco(nomeMunicipio, info != null ? info.id() : null, menorSerie.mes(), valor);
    }

    private EvolucaoIndicadorPrecoDTO construirIndicadorPreco(String municipioNome,
                                                              String municipioId,
                                                              YearMonth mes,
                                                              BigDecimal valor) {
        if (valor == null) {
            return null;
        }
        BigDecimal ajustado = valor.setScale(2, RoundingMode.HALF_UP);
        String mesIso = mes != null ? mes.toString() : null;
        String mesDescricao = formatarMesDescricao(mes);
        String observacao;
        if (municipioNome != null && mesDescricao != null) {
            observacao = municipioNome + " • " + mesDescricao;
        } else if (municipioNome != null) {
            observacao = municipioNome;
        } else {
            observacao = mesDescricao;
        }

        return EvolucaoIndicadorPrecoDTO.builder()
            .valor(ajustado)
            .municipio(municipioNome)
            .municipioId(municipioId)
            .mes(mesIso)
            .mesDescricao(mesDescricao)
            .observacao(observacao)
            .build();
    }

    private EvolucaoVariacaoDTO calcularVariacaoMensalIndicador(List<MunicipioSerieResultado> municipios) {
        List<Double> valores = municipios.stream()
            .map(this::calcularVariacaoMensal)
            .filter(Objects::nonNull)
            .toList();
        if (valores.isEmpty()) {
            return EvolucaoVariacaoDTO.builder()
                .percentual(null)
                .descricao("Sem dados mensais suficientes.")
                .municipiosConsiderados(0)
                .build();
        }
        double media = valores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        return EvolucaoVariacaoDTO.builder()
            .percentual(arredondarUmaCasa(media))
            .descricao("Variação média entre os dois últimos registros do recorte selecionado.")
            .municipiosConsiderados(valores.size())
            .build();
    }

    private EvolucaoVariacaoDTO calcularVariacaoAnualIndicador(List<MunicipioSerieResultado> municipios,
                                                               Integer anoReferencia) {
        List<Double> valores = municipios.stream()
            .map(resultado -> calcularVariacaoAnual(resultado, anoReferencia))
            .filter(Objects::nonNull)
            .toList();
        if (valores.isEmpty()) {
            return EvolucaoVariacaoDTO.builder()
                .percentual(null)
                .descricao("Sem dados anuais suficientes.")
                .anoReferencia(anoReferencia)
                .municipiosConsiderados(0)
                .build();
        }
        double media = valores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        String descricao = anoReferencia != null
            ? "Variação anual média dos municípios selecionados no ano " + anoReferencia + "."
            : "Variação anual média dos municípios selecionados.";
        return EvolucaoVariacaoDTO.builder()
            .percentual(arredondarUmaCasa(media))
            .descricao(descricao)
            .anoReferencia(anoReferencia)
            .municipiosConsiderados(valores.size())
            .build();
    }

    private EvolucaoTendenciaDTO calcularTendenciaIndicador(List<MunicipioSerieResultado> municipios) {
        List<Double> slopes = municipios.stream()
            .map(this::calcularTendenciaPercentual)
            .filter(Objects::nonNull)
            .toList();
        if (slopes.isEmpty()) {
            return EvolucaoTendenciaDTO.builder()
                .percentual(null)
                .texto("Informações insuficientes para tendência.")
                .descricao("Adicione municípios ou ajuste o período para estimar a tendência.")
                .classe("text-muted")
                .status("INDEFINIDO")
                .municipiosConsiderados(0)
                .build();
        }

        double media = slopes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double arredondado = arredondarUmaCasa(media);
        TendenciaInterpretacao interpretacao = interpretarTendencia(arredondado);
        String descricao = "Inclinação média de " + formatarPercentual(arredondado)
            + " ao mês considerando os municípios selecionados.";

        return EvolucaoTendenciaDTO.builder()
            .percentual(arredondado)
            .texto(interpretacao.texto())
            .descricao(descricao)
            .classe(interpretacao.classe())
            .status(interpretacao.status())
            .municipiosConsiderados(slopes.size())
            .build();
    }

    private Double calcularVariacaoMensal(MunicipioSerieResultado resultado) {
        List<SerieMensal> serie = resultado.serie();
        if (serie == null) {
            return null;
        }
        List<SerieMensal> validos = serie.stream()
            .filter(Objects::nonNull)
            .filter(ponto -> ponto.total() > 0)
            .toList();
        if (validos.size() < 2) {
            return null;
        }
        SerieMensal ultimo = validos.get(validos.size() - 1);
        SerieMensal penultimo = validos.get(validos.size() - 2);
        double anterior = penultimo.total();
        if (anterior <= 0) {
            return null;
        }
        double atual = ultimo.total();
        if (atual <= 0) {
            return null;
        }
        return ((atual - anterior) / anterior) * 100.0;
    }

    private Double calcularVariacaoAnual(MunicipioSerieResultado resultado, Integer anoReferencia) {
        List<SerieMensal> serie = resultado.serie();
        if (serie == null || serie.isEmpty()) {
            return null;
        }
        List<SerieMensal> base = serie;
        if (anoReferencia != null) {
            base = serie.stream()
                .filter(Objects::nonNull)
                .filter(ponto -> ponto.mes() != null && ponto.mes().getYear() == anoReferencia)
                .toList();
            if (base.size() < 2) {
                return null;
            }
        }
        List<SerieMensal> validos = base.stream()
            .filter(Objects::nonNull)
            .filter(ponto -> ponto.total() > 0)
            .toList();
        if (validos.size() < 2) {
            return null;
        }
        SerieMensal primeiro = validos.get(0);
        SerieMensal ultimo = validos.get(validos.size() - 1);
        double inicial = primeiro.total();
        if (inicial <= 0) {
            return null;
        }
        double valorFinal = ultimo.total();
        if (valorFinal <= 0) {
            return null;
        }
        return ((valorFinal - inicial) / inicial) * 100.0;
    }

    private Double calcularTendenciaPercentual(MunicipioSerieResultado resultado) {
        List<SerieMensal> serie = resultado.serie();
        if (serie == null) {
            return null;
        }
        List<SerieMensal> validos = serie.stream()
            .filter(Objects::nonNull)
            .filter(ponto -> ponto.total() > 0)
            .toList();
        if (validos.size() < 2) {
            return null;
        }
        int n = validos.size();
        double sumX = 0.0;
        double sumY = 0.0;
        double sumXY = 0.0;
        double sumX2 = 0.0;
        for (int i = 0; i < n; i++) {
            double x = i;
            double y = validos.get(i).total();
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }
        double denominator = n * sumX2 - sumX * sumX;
        if (denominator == 0) {
            return 0.0;
        }
        double slope = (n * sumXY - sumX * sumY) / denominator;
        double media = sumY / n;
        if (media <= 0) {
            return 0.0;
        }
        return (slope / media) * 100.0;
    }

    private TendenciaInterpretacao interpretarTendencia(double percentual) {
        if (percentual > 0.6) {
            return new TendenciaInterpretacao("Tendência de alta acentuada no período.", "ALTA", "text-danger");
        }
        if (percentual > 0.2) {
            return new TendenciaInterpretacao("Tendência de alta moderada nos últimos meses.", "ALTA", "text-danger");
        }
        if (percentual < -0.6) {
            return new TendenciaInterpretacao("Tendência de queda acentuada no período.", "QUEDA", "text-success");
        }
        if (percentual < -0.2) {
            return new TendenciaInterpretacao("Tendência de queda moderada nos últimos meses.", "QUEDA", "text-success");
        }
        return new TendenciaInterpretacao("Estabilidade de preços.", "ESTAVEL", "text-warning");
    }

    private String formatarMesDescricao(YearMonth mes) {
        if (mes == null) {
            return null;
        }
        String texto = MES_LONGO_FORMATTER.format(mes);
        if (texto == null || texto.isBlank()) {
            return null;
        }
        if (texto.length() == 1) {
            return texto.toUpperCase(LOCALE_PT_BR);
        }
        return texto.substring(0, 1).toUpperCase(LOCALE_PT_BR) + texto.substring(1);
    }

    private String formatarPercentual(double valor) {
        return String.format(LOCALE_PT_BR, "%+.1f%%", valor);
    }

    private EvolucaoFiltro construirFiltro(Collection<String> municipios) {
        if (municipios == null || municipios.isEmpty()) {
            return EvolucaoFiltro.builder().build();
        }
        Set<String> selecionados = municipios.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(valor -> !valor.isEmpty())
            .collect(Collectors.toCollection(LinkedHashSet::new));
        return EvolucaoFiltro.builder()
            .municipios(selecionados)
            .build();
    }

    private PeriodosDisponiveisDTO periodosVazios() {
        return PeriodosDisponiveisDTO.builder()
            .anos(List.of())
            .meses(List.of())
            .mesesPorAno(Map.of())
            .build();
    }

    private SortedMap<YearMonth, DoubleSummaryStatistics> agruparMediaTotalPorMes(Collection<GastoMensal> registros) {
        SortedMap<YearMonth, DoubleSummaryStatistics> mapa = new TreeMap<>();
        if (registros == null) {
            return mapa;
        }

        for (GastoMensal gasto : registros) {
            if (gasto == null) {
                continue;
            }
            YearMonth mes = gasto.getMesAno();
            BigDecimal total = gasto.getTotalCesta();
            if (mes == null || total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            mapa.computeIfAbsent(mes, chave -> new DoubleSummaryStatistics()).accept(total.doubleValue());
        }

        return mapa;
    }

    private MunicipioInfo extrairMunicipioInfo(String raw) {
        if (raw == null) {
            return null;
        }

        String texto = raw.trim();
        if (texto.isEmpty()) {
            return null;
        }

        String nome = texto;
        String uf = "";

        String[] barraSplit = texto.split("/");
        if (barraSplit.length == 2 && barraSplit[1].trim().length() == 2) {
            nome = barraSplit[0];
            uf = barraSplit[1].trim();
        } else {
            Matcher parentese = UF_PARENTESIS.matcher(texto);
            if (parentese.find()) {
                nome = parentese.group(1);
                uf = parentese.group(2);
            } else {
                Matcher hifen = UF_HIFEN.matcher(texto);
                if (hifen.find()) {
                    nome = hifen.group(1);
                    uf = hifen.group(2);
                }
            }
        }

        nome = nome.replaceAll("[\\s\\-]+$", "").trim();
        if (nome.isEmpty()) {
            return null;
        }

        String ufUpper = uf == null ? "" : uf.trim().toUpperCase(Locale.ROOT);
        String display = formatarDisplayName(nome);
        String idBase = normalizarMunicipioId(nome);
        if (idBase == null || idBase.isBlank()) {
            return null;
        }

        String id = ufUpper.isEmpty() ? idBase : idBase + "-" + ufUpper;
        return new MunicipioInfo(display, ufUpper, id);
    }

    private static String construirRotuloMunicipio(MunicipioInfo info) {
        if (info == null) {
            return "";
        }
        String nome = info.displayName();
        if (nome == null || nome.isBlank()) {
            nome = info.id() != null ? info.id() : "";
        }
        String uf = info.uf();
        if (uf == null || uf.isBlank()) {
            return nome;
        }
        return nome + " / " + uf;
    }

    private static class MunicipioSerieAcumulador {
        private final MunicipioInfo info;
        private final SortedMap<YearMonth, EstatisticaMensal> porMes = new TreeMap<>();
        private final Set<String> nomesOrigem = new LinkedHashSet<>();

        private MunicipioSerieAcumulador(MunicipioInfo info) {
            this.info = info;
        }

        void adicionar(GastoMensal gasto) {
            if (gasto == null) {
                return;
            }
            YearMonth mes = gasto.getMesAno();
            if (mes == null) {
                return;
            }
            if (gasto.getMunicipio() != null) {
                nomesOrigem.add(gasto.getMunicipio());
            }
            porMes.computeIfAbsent(mes, chave -> new EstatisticaMensal()).adicionar(gasto);
        }

        MunicipioSerieResultado toResultado() {
            List<SerieMensal> serie = porMes.entrySet().stream()
                .map(entry -> entry.getValue().toSerieMensal(entry.getKey()))
                .filter(Objects::nonNull)
                .toList();
            if (serie.isEmpty()) {
                return null;
            }
            Set<String> origem = nomesOrigem.isEmpty()
                ? Set.of()
                : Collections.unmodifiableSet(new LinkedHashSet<>(nomesOrigem));
            return new MunicipioSerieResultado(info, serie, origem);
        }
    }

    private static class EstatisticaMensal {
        private double somaTotal = 0;
        private int quantidade = 0;
        private final Map<String, DoubleSummaryStatistics> componentes = new LinkedHashMap<>();

        void adicionar(GastoMensal gasto) {
            if (gasto == null) {
                return;
            }
            BigDecimal total = gasto.getTotalCesta();
            if (total != null && total.compareTo(BigDecimal.ZERO) > 0) {
                somaTotal += total.doubleValue();
                quantidade++;
            }
            for (ComponenteExtractor extractor : COMPONENTES) {
                BigDecimal valor = extractor.extractor().apply(gasto);
                if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                componentes
                    .computeIfAbsent(extractor.chave(), chave -> new DoubleSummaryStatistics())
                    .accept(valor.doubleValue());
            }
        }

        SerieMensal toSerieMensal(YearMonth mes) {
            if (mes == null) {
                return null;
            }
            Map<String, Double> mediasComponentes = componentes.entrySet().stream()
                .filter(entry -> entry.getValue().getCount() > 0)
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> arredondarDuasCasas(entry.getValue().getAverage()),
                    (a, b) -> a,
                    LinkedHashMap::new
                ));

            Map<String, Double> componentesFinal = mediasComponentes.isEmpty()
                ? null
                : Collections.unmodifiableMap(mediasComponentes);

            if (quantidade == 0) {
                return null;
            }
            double mediaTotal = somaTotal / quantidade;
            double mediaArredondada = arredondarDuasCasas(mediaTotal);
            if (mediaArredondada <= 0 && componentesFinal == null) {
                return null;
            }
            return new SerieMensal(mes, mediaArredondada, componentesFinal);
        }
    }

    private static class ComponentAggregator {
        private double soma = 0;
        private int quantidade = 0;
        private Double primeiroValor = null;
        private Double ultimoValor = null;
        private YearMonth primeiroMes = null;
        private YearMonth ultimoMes = null;

        void adicionar(double valor, YearMonth mes) {
            soma += valor;
            quantidade++;
            if (primeiroValor == null) {
                primeiroValor = valor;
                primeiroMes = mes;
            }
            ultimoValor = valor;
            ultimoMes = mes;
        }

        double getSoma() {
            return soma;
        }

        int getQuantidade() {
            return quantidade;
        }

        Double getPrimeiroValor() {
            return primeiroValor;
        }

        Double getUltimoValor() {
            return ultimoValor;
        }

        YearMonth getPrimeiroMes() {
            return primeiroMes;
        }

        YearMonth getUltimoMes() {
            return ultimoMes;
        }
    }

    private record SerieMensal(YearMonth mes, double total, Map<String, Double> componentes) {
        CestaBasicaSerieDTO toDto() {
            return CestaBasicaSerieDTO.builder()
                .mes(formatYearMonthForFrontend(mes))
                .cesta(total)
                .componentes(componentes)
                .build();
        }
    }

    private record MunicipioSerieResultado(MunicipioInfo info,
                                           List<SerieMensal> serie,
                                           Set<String> nomesOrigem) {

        CestaBasicaSerieMunicipioDTO toSerieMunicipioDto() {
            List<CestaBasicaSerieDTO> serieDto = serie.stream()
                .map(SerieMensal::toDto)
                .toList();
            return CestaBasicaSerieMunicipioDTO.builder()
                .municipio(construirRotuloMunicipio(info))
                .serie(serieDto)
                .build();
        }
    }

    private record EvolucaoContext(EvolucaoFiltro filtro, List<MunicipioSerieResultado> municipios) {
        Set<String> nomesOrigem() {
            return municipios.stream()
                .flatMap(resultado -> resultado.nomesOrigem().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        public List<MunicipioSerieResultado> municipios() {
            return municipios;
        }

        public EvolucaoFiltro filtro() {
            return filtro;
        }
    }

    private record ComponenteExtractor(String chave, Function<GastoMensal, BigDecimal> extractor) {
    }

    private record MunicipioInfo(String displayName, String uf, String id) {
    }

    private record TendenciaInterpretacao(String texto, String status, String classe) {
    }

    private static double arredondarDuasCasas(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    private double arredondarUmaCasa(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }

    private String normalizarMunicipioKey(String raw) {
        if (raw == null) {
            return null;
        }

        String texto = raw.trim();
        if (texto.isEmpty()) {
            return null;
        }

        String colapsado = texto.replaceAll("\\s+", " ");
        String semAcento = Normalizer.normalize(colapsado, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "");
        return semAcento.toUpperCase(Locale.ROOT);
    }

    private static String formatYearMonthForFrontend(YearMonth yearMonth) {
        if (yearMonth == null) {
            return null;
        }
        return FRONT_YEAR_MONTH_FORMATTER.format(yearMonth);
    }

    private static String formatarDisplayName(String nome) {
        String texto = nome == null ? "" : nome.trim();
        if (texto.isEmpty()) {
            return "";
        }

        String lower = texto.toLowerCase(LOCALE_PT_BR);
        StringBuilder builder = new StringBuilder(lower.length());
        boolean capitalizeNext = true;

        for (int i = 0; i < lower.length(); i++) {
            char ch = lower.charAt(i);
            if (capitalizeNext && Character.isLetter(ch)) {
                builder.append(String.valueOf(ch).toUpperCase(LOCALE_PT_BR));
                capitalizeNext = false;
            } else {
                builder.append(ch);
            }

            if (Character.isWhitespace(ch) || ch == '-' || ch == APOSTROFO) {
                capitalizeNext = true;
            }
        }

        return builder.toString().replaceAll("\\s+", " ").trim();
    }

    private static String normalizarMunicipioId(String raw) {
        if (raw == null) {
            return null;
        }

        String texto = raw.trim();
        if (texto.isEmpty()) {
            return null;
        }

        String semEspacos = texto.replaceAll("\\s+", " ");
        String semAcento = Normalizer.normalize(semEspacos, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "");
        return semAcento.toUpperCase(Locale.ROOT);
    }

    private Integer anoReferenciaAplicavel(EvolucaoFiltro filtro) {
        if (filtro == null) {
            return null;
        }
        Integer ano = filtro.getAnoReferencia();
        if (ano == null) {
            return null;
        }
        if (filtro.getMesInicio() != null || filtro.getMesFim() != null) {
            return null;
        }
        return ano;
    }

    private YearMonth parseYearMonth(String raw) {
        String valor = raw == null ? null : raw.trim();
        if (valor == null || valor.isEmpty()) {
            return null;
        }

        String normalized = valor.replace('.', '/');
        if (!normalized.isEmpty()) {
            normalized = normalized.replaceAll("^[^\\p{Alnum}]+", "");
        }
        if (normalized.matches("\\d{6}")) {
            String firstTwo = normalized.substring(0, 2);
            if (isValidMonth(firstTwo)) {
                normalized = firstTwo + "/" + normalized.substring(2);
            } else {
                String lastTwo = normalized.substring(4);
                if (isValidMonth(lastTwo)) {
                    normalized = normalized.substring(0, 4) + "-" + lastTwo;
                }
            }
        }

        YearMonth parsed = tryParseYearMonth(normalized);
        if (parsed != null) {
            return parsed;
        }

        int espaco = normalized.indexOf(' ');
        if (espaco > 0) {
            parsed = tryParseYearMonth(normalized.substring(0, espaco));
            if (parsed != null) {
                return parsed;
            }
        }

        return parsePortugueseMonth(normalized);
    }

    private boolean isValidMonth(String value) {
        try {
            int month = Integer.parseInt(value);
            return month >= 1 && month <= 12;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private YearMonth tryParseYearMonth(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return null;
        }
        for (DateTimeFormatter formatter : YEAR_MONTH_FORMATS) {
            try {
                return YearMonth.parse(candidate, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private YearMonth parsePortugueseMonth(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String sanitized = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT);

        String[] tokens = sanitized.split("[^a-z0-9]+");
        Integer month = null;
        Integer year = null;

        for (String token : tokens) {
            if (token.isEmpty()) {
                continue;
            }
            if (month == null) {
                month = PORTUGUESE_MONTHS.get(token);
                if (month != null) {
                    continue;
                }
            }
            if (year == null && token.matches("\\d{4}")) {
                year = Integer.parseInt(token);
            }
        }

        if (month != null && year != null) {
            return YearMonth.of(year, month);
        }
        return null;
    }

    public String formatarMes(YearMonth mes) {
        if (mes == null) {
            return null;
        }
        return YEAR_MONTH_NUMERIC.format(mes);
    }
}
