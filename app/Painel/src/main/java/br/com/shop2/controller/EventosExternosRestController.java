package br.com.shop2.controller;

import br.com.shop2.domain.service.EventoExternoService;
import br.com.shop2.model.evento.EventoExternoDetalheDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/api/eventos-externos")
@RequiredArgsConstructor
public class EventosExternosRestController {

    private final EventoExternoService eventoExternoService;

    @GetMapping
    public List<EventoExternoDetalheDTO> listar(@RequestParam(value = "inicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                                @RequestParam(value = "fim", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return eventoExternoService.buscarPorFiltro(inicio, fim);
    }
}
