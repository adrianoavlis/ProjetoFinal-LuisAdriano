package br.com.shop2.domain.service;

import br.com.shop2.domain.repository.EventoExternoRepository;
import br.com.shop2.model.evento.EventoExterno;
import br.com.shop2.model.evento.EventoExternoDetalheDTO;
import br.com.shop2.model.evento.EventoExternoForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class EventoExternoService {

    private static final DateTimeFormatter PERIODO_FORMATTER = DateTimeFormatter.ofPattern("MM/yyyy");

    private final EventoExternoRepository eventoExternoRepository;

    @Transactional
    public void criar(EventoExternoForm form) {
        EventoExterno entidade = EventoExterno.builder()
            .titulo(sanitizarTexto(form.getTitulo()))
            .descricao(sanitizarDescricao(form.getDescricao()))
            .dataInicio(form.getDataInicio())
            .dataFim(form.getDataFim())
            .impacto(Objects.requireNonNull(form.getImpacto(), "Informe o impacto do evento."))
            .build();
        eventoExternoRepository.save(entidade);
    }

    @Transactional
    public void atualizar(Long id, EventoExternoForm form) {
        EventoExterno existente = eventoExternoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Evento não encontrado para edição."));

        existente.setTitulo(sanitizarTexto(form.getTitulo()));
        existente.setDescricao(sanitizarDescricao(form.getDescricao()));
        existente.setDataInicio(form.getDataInicio());
        existente.setDataFim(form.getDataFim());
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

    @Transactional(readOnly = true)
    public List<EventoExternoDetalheDTO> listarTodos() {
        return converterParaDetalhe(eventoExternoRepository.findAll(), null, null);
    }

    @Transactional(readOnly = true)
    public List<EventoExternoDetalheDTO> buscarPorFiltro(LocalDate dataInicio, LocalDate dataFim) {
        List<EventoExterno> candidatos = eventoExternoRepository.buscarPorPeriodo(dataInicio, dataFim);
        return converterParaDetalhe(candidatos, dataInicio, dataFim);
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

        return EventoExternoDetalheDTO.builder()
            .id(evento.getId())
            .titulo(evento.getTitulo())
            .descricao(evento.getDescricao())
            .dataInicio(evento.getDataInicio())
            .dataFim(evento.getDataFim())
            .impacto(evento.getImpacto())
            .periodoInicio(periodoInicio != null ? PERIODO_FORMATTER.format(periodoInicio) : null)
            .periodoFim(periodoFim != null ? PERIODO_FORMATTER.format(periodoFim) : null)
            .build();
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
