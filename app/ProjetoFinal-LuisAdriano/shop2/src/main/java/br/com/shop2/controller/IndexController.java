package br.com.shop2.controller;

import br.com.shop2.domain.service.EvolucaoService;
import br.com.shop2.model.common.EvolucaoMunicipioDTO;
import br.com.shop2.model.common.PeriodosDisponiveisDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
@Controller
@RequiredArgsConstructor
public class IndexController {

    private final EvolucaoService evolucaoService;
    private final ObjectMapper objectMapper;

    @GetMapping("/")
    public String index(Model model) {
        popularDadosEvolucao(model);
        return "index"; // busca index.jsp em /WEB-INF/views/
    }

    private void popularDadosEvolucao(Model model) {
        List<EvolucaoMunicipioDTO> series = evolucaoService.evolucaoMunicipios();
        if (series == null) {
            series = List.of();
        }

        List<String> municipiosDisponiveis = obterMunicipiosDisponiveis();
        PeriodosDisponiveisDTO periodos = obterPeriodosDisponiveis();

        model.addAttribute("evolucaoPrefetch", series);
        model.addAttribute("municipiosDisponiveis", municipiosDisponiveis);
        model.addAttribute("periodosDisponiveis", periodos);

        Map<String, Object> prefetchPayload = new LinkedHashMap<>();
        prefetchPayload.put("municipios", municipiosDisponiveis);
        prefetchPayload.put("series", series);

        model.addAttribute("evolucaoPrefetchJson", toJson(prefetchPayload, "{}"));
        model.addAttribute("periodosDisponiveisJson", toJson(periodos, "{}"));
    }

    private List<String> obterMunicipiosDisponiveis() {
        List<String> municipiosDisponiveis = evolucaoService.listarMunicipiosDisponiveis();
        if (municipiosDisponiveis == null) {
            return List.of();
        }
        return municipiosDisponiveis;
    }

    private PeriodosDisponiveisDTO obterPeriodosDisponiveis() {
        PeriodosDisponiveisDTO periodos = evolucaoService.listarPeriodosDisponiveis();
        if (periodos == null) {
            return PeriodosDisponiveisDTO.builder()
                .anos(List.of())
                .meses(List.of())
                .mesesPorAno(Map.of())
                .build();
        }
        return periodos;
    }

    private String toJson(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return fallback;
        }
    }

}
