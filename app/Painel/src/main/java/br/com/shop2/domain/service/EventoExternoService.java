package br.com.shop2.domain.service;

import br.com.shop2.domain.repository.EventoExternoRepository;
import br.com.shop2.model.common.CestaBasicaSerieDTO;
import br.com.shop2.model.common.CestaBasicaSerieMunicipioDTO;
import br.com.shop2.model.evento.EventoExterno;
import br.com.shop2.model.evento.EventoExternoDetalheDTO;
import br.com.shop2.model.evento.EventoExternoForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventoExternoService {

    private static final DateTimeFormatter SERIE_MES_FORMATTER =
        new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("MMM-uuuu")
            .toFormatter(Locale.ENGLISH);

    private static final DateTimeFormatter PERIODO_FORMATTER = DateTimeFormatter.ofPattern("MM/yyyy");

    private final EventoExternoRepository eventoExternoRepository;
    private final CestaBasicaService cestaBasicaService;

    @Transactional
    public void criar(EventoExternoForm form) {
        List<String> municipios = sanitizarMunicipios(form.getMunicipios());
        if (municipios.isEmpty()) {
            throw new IllegalArgumentException("Informe ao menos um município impactado.");
        }

        EventoExterno entidade = EventoExterno.builder()
            .titulo(sanitizarTexto(form.getTitulo()))
            .descricao(sanitizarDescricao(form.getDescricao()))
            .dataInicio(form.getDataInicio())
            .dataFim(form.getDataFim())
            .municipios(new ArrayList<>(municipios))
            .impacto(Objects.requireNonNull(form.getImpacto(), "Informe o impacto do evento."))
            .build();
        eventoExternoRepository.save(entidade);
    }

    @Transactional
    public void atualizar(Long id, EventoExternoForm form) {
        EventoExterno existente = eventoExternoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Evento não encontrado para edição."));

        List<String> municipios = sanitizarMunicipios(form.getMunicipios());
        if (municipios.isEmpty()) {
            throw new IllegalArgumentException("Informe ao menos um município impactado.");
        }

        existente.setTitulo(sanitizarTexto(form.getTitulo()));
        existente.setDescricao(sanitizarDescricao(form.getDescricao()));
        existente.setDataInicio(form.getDataInicio());
        existente.setDataFim(form.getDataFim());
        existente.setMunicipios(new ArrayList<>(municipios));
        existente.setImpacto(Objects.requireNonNull(form.getImpacto(), "Informe o impacto do evento."));

        eventoExternoRepository.save(existente);
    }

    @Transactional
    public void excluir(Long id) {
        if (!eventoExternoRepository.existsById(id)) {
            return;
        }
        eventoExternoRepository.deleteById(id);
    }

    public List<EventoExternoDetalheDTO> listarTodos() {
        return converterParaDetalhe(eventoExternoRepository.findAll(), null, null);
    }

    public List<EventoExternoDetalheDTO> buscarPorFiltro(Collection<String> municipios,
                                                         LocalDate dataInicio,
                                                         LocalDate dataFim) {
        List<EventoExterno> candidatos = eventoExternoRepository.buscarPorPeriodo(dataInicio, dataFim);
        Set<String> municipiosNormalizados = normalizarMunicipios(municipios);

        List<EventoExterno> filtrados = candidatos.stream()
            .filter(evento -> filtraPorMunicipio(evento, municipiosNormalizados))
            .toList();

        return converterParaDetalhe(filtrados, dataInicio, dataFim);
    }

    public Set<String> listarMunicipiosDisponiveis() {
        return eventoExternoRepository.findAll().stream()
            .map(EventoExterno::getMunicipios)
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .map(valor -> valor != null ? valor.trim() : null)
            .filter(Objects::nonNull)
            .filter(valor -> !valor.isEmpty())
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private List<EventoExternoDetalheDTO> converterParaDetalhe(List<EventoExterno> eventos,
                                                               LocalDate filtroInicio,
                                                               LocalDate filtroFim) {
        if (eventos == null || eventos.isEmpty()) {
            return List.of();
        }

        Comparator<EventoExterno> comparator = Comparator
            .comparing(EventoExterno::getDataInicio, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(EventoExterno::getTitulo, Comparator.nullsLast(String::compareToIgnoreCase));

        return eventos.stream()
            .sorted(comparator)
            .map(evento -> toDetalhe(evento, filtroInicio, filtroFim))
            .toList();
    }

    private EventoExternoDetalheDTO toDetalhe(EventoExterno evento,
                                              LocalDate filtroInicio,
                                              LocalDate filtroFim) {
        LocalDate inicio = evento.getDataInicio();
        LocalDate fim = evento.getDataFim();
        if (filtroInicio != null && (inicio == null || filtroInicio.isAfter(inicio))) {
            inicio = filtroInicio;
        }
        if (filtroFim != null && (fim == null || filtroFim.isBefore(fim))) {
            fim = filtroFim;
        }
        YearMonth periodoInicio = inicio != null ? YearMonth.from(inicio) : null;
        YearMonth periodoFim = fim != null ? YearMonth.from(fim) : null;

        BigDecimal valorMedio = calcularValorMedioCesta(evento, periodoInicio, periodoFim);
        List<String> municipiosEvento = Optional.ofNullable(evento.getMunicipios()).orElse(List.of()).stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(valor -> !valor.isEmpty())
            .toList();
        List<String> municipiosId = municipiosEvento.stream()
            .map(this::normalizarMunicipio)
            .filter(Objects::nonNull)
            .toList();

        return EventoExternoDetalheDTO.builder()
            .id(evento.getId())
            .titulo(evento.getTitulo())
            .descricao(evento.getDescricao())
            .dataInicio(evento.getDataInicio())
            .dataFim(evento.getDataFim())
            .municipios(municipiosEvento)
            .municipiosId(municipiosId)
            .municipiosConcatenados(String.join(":::", municipiosEvento))
            .impacto(evento.getImpacto())
            .valorMedioCesta(valorMedio)
            .periodoInicio(periodoInicio != null ? PERIODO_FORMATTER.format(periodoInicio) : null)
            .periodoFim(periodoFim != null ? PERIODO_FORMATTER.format(periodoFim) : null)
            .build();
    }

    private BigDecimal calcularValorMedioCesta(EventoExterno evento,
                                               YearMonth periodoInicio,
                                               YearMonth periodoFim) {
        if (evento == null || periodoInicio == null || periodoFim == null) {
            return null;
        }
        if (periodoFim.isBefore(periodoInicio)) {
            return null;
        }

        List<String> municipiosEvento = Optional.ofNullable(evento.getMunicipios()).orElse(List.of()).stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(valor -> !valor.isEmpty())
            .toList();
        if (municipiosEvento.isEmpty()) {
            return null;
        }

        List<CestaBasicaSerieMunicipioDTO> series = cestaBasicaService
            .serieHistoricaPorMunicipio(municipiosEvento);

        if (series == null || series.isEmpty()) {
            return null;
        }

        Set<String> municipiosNormalizados = municipiosEvento.stream()
            .map(this::normalizarMunicipio)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        List<Double> valores = new ArrayList<>();
        for (CestaBasicaSerieMunicipioDTO serieMunicipio : series) {
            if (serieMunicipio == null || serieMunicipio.getSerie() == null) {
                continue;
            }
            String municipioId = normalizarMunicipio(serieMunicipio.getMunicipio());
            if (municipioId == null || !municipiosNormalizados.contains(municipioId)) {
                continue;
            }
            for (CestaBasicaSerieDTO ponto : serieMunicipio.getSerie()) {
                if (ponto == null || ponto.getMes() == null || ponto.getCesta() == null) {
                    continue;
                }
                YearMonth mes = parseSerieMes(ponto.getMes());
                if (mes == null) {
                    continue;
                }
                if (mes.isBefore(periodoInicio) || mes.isAfter(periodoFim)) {
                    continue;
                }
                valores.add(ponto.getCesta());
            }
        }

        if (valores.isEmpty()) {
            return null;
        }

        double media = valores.stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(Double.NaN);

        if (Double.isNaN(media)) {
            return null;
        }

        return BigDecimal.valueOf(media).setScale(2, RoundingMode.HALF_UP);
    }

    private YearMonth parseSerieMes(String valor) {
        if (valor == null) {
            return null;
        }
        String texto = valor.trim();
        if (texto.isEmpty()) {
            return null;
        }
        try {
            return YearMonth.parse(texto, SERIE_MES_FORMATTER);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private boolean filtraPorMunicipio(EventoExterno evento, Set<String> municipiosNormalizados) {
        if (municipiosNormalizados == null || municipiosNormalizados.isEmpty()) {
            return true;
        }
        List<String> municipiosEvento = Optional.ofNullable(evento.getMunicipios()).orElse(List.of());
        return municipiosEvento.stream()
            .map(this::normalizarMunicipio)
            .filter(Objects::nonNull)
            .anyMatch(municipiosNormalizados::contains);
    }

    private Set<String> normalizarMunicipios(Collection<String> municipios) {
        if (municipios == null) {
            return Set.of();
        }
        return municipios.stream()
            .filter(Objects::nonNull)
            .map(this::normalizarMunicipio)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    private String normalizarMunicipio(String valor) {
        if (valor == null) {
            return null;
        }
        String texto = valor.trim();
        if (texto.isEmpty()) {
            return null;
        }
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "");
        return semAcento.toUpperCase(Locale.ROOT);
    }

    private List<String> sanitizarMunicipios(Collection<String> municipios) {
        if (municipios == null) {
            return List.of();
        }
        LinkedHashSet<String> valores = new LinkedHashSet<>();
        for (String municipio : municipios) {
            String texto = sanitizarTexto(municipio);
            if (texto == null) {
                continue;
            }
            if (texto.length() > 180) {
                throw new IllegalArgumentException("O município deve ter no máximo 180 caracteres.");
            }
            valores.add(texto);
        }
        return new ArrayList<>(valores);
    }

    private String sanitizarTexto(String texto) {
        if (texto == null) {
            return null;
        }
        String trimmed = texto.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String sanitizarDescricao(String descricao) {
        if (descricao == null) {
            return null;
        }
        String texto = descricao.trim();
        return texto.isEmpty() ? null : texto;
    }
}
