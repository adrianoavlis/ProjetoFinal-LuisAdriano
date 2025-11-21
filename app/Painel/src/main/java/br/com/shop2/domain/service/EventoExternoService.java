package br.com.shop2.domain.service;

import br.com.shop2.domain.repository.EventoExternoRepository;
import br.com.shop2.model.common.CestaBasicaSerieDTO;
import br.com.shop2.model.common.CestaBasicaSerieMunicipioDTO;
import br.com.shop2.model.common.Municipios;
import br.com.shop2.model.evento.EventoExterno;
import br.com.shop2.model.evento.EventoExternoDetalheDTO;
import br.com.shop2.model.evento.EventoExternoForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
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
        List<Municipios> municipios = form.getMunicipios();
        if (municipios == null || municipios.isEmpty()) {
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

        List<Municipios> municipios = form.getMunicipios();
        if (municipios == null || municipios.isEmpty()) {
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
        Set<Municipios> municipiosNormalizados = municipios == null ? Set.of()
            : municipios.stream()
            .map(Municipios::fromTexto)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        List<EventoExterno> filtrados = candidatos.stream()
            .filter(evento -> filtraPorMunicipio(evento, municipiosNormalizados))
            .toList();

        return converterParaDetalhe(filtrados, dataInicio, dataFim);
    }

    public Set<Municipios> listarMunicipiosDisponiveis() {
        return eventoExternoRepository.findAll().stream()
            .map(EventoExterno::getMunicipios)
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Municipios::getNome))));
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
        List<Municipios> municipiosEvento = Optional.ofNullable(evento.getMunicipios()).orElse(List.of()).stream()
            .filter(Objects::nonNull)
            .toList();
        List<String> municipiosNomes = municipiosEvento.stream()
            .map(Municipios::getNome)
            .toList();
        List<String> municipiosId = municipiosEvento.stream()
            .map(Municipios::name)
            .toList();

        return EventoExternoDetalheDTO.builder()
            .id(evento.getId())
            .titulo(evento.getTitulo())
            .descricao(evento.getDescricao())
            .dataInicio(evento.getDataInicio())
            .dataFim(evento.getDataFim())
            .municipios(municipiosEvento)
            .municipiosId(municipiosId)
            .municipiosConcatenados(String.join(":::", municipiosNomes))
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

        List<Municipios> municipiosEvento = Optional.ofNullable(evento.getMunicipios()).orElse(List.of()).stream()
            .filter(Objects::nonNull)
            .toList();
        if (municipiosEvento.isEmpty()) {
            return null;
        }

        List<CestaBasicaSerieMunicipioDTO> series = cestaBasicaService
            .serieHistoricaPorMunicipios(municipiosEvento);

        if (series == null || series.isEmpty()) {
            return null;
        }

        Set<Municipios> municipiosNormalizados = new HashSet<>(municipiosEvento);

        List<Double> valores = new ArrayList<>();
        for (CestaBasicaSerieMunicipioDTO serieMunicipio : series) {
            if (serieMunicipio == null || serieMunicipio.getSerie() == null) {
                continue;
            }
            Municipios municipioId = Municipios.fromTexto(serieMunicipio.getMunicipio()).orElse(null);
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

    private boolean filtraPorMunicipio(EventoExterno evento, Set<Municipios> municipiosNormalizados) {
        if (municipiosNormalizados == null || municipiosNormalizados.isEmpty()) {
            return true;
        }
        List<Municipios> municipiosEvento = Optional.ofNullable(evento.getMunicipios()).orElse(List.of());
        return municipiosEvento.stream()
            .filter(Objects::nonNull)
            .anyMatch(municipiosNormalizados::contains);
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
