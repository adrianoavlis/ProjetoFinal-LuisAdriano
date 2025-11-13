package br.com.shop2.controller;

import br.com.shop2.domain.service.EvolucaoService;
import br.com.shop2.model.common.CestaBasicaSerieMunicipioDTO;
import br.com.shop2.model.common.EvolucaoFiltro;
import br.com.shop2.model.common.EvolucaoIndicadoresDTO;
import br.com.shop2.model.common.EvolucaoMunicipioDTO;
import br.com.shop2.model.common.PeriodosDisponiveisDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/evolucao")
@RequiredArgsConstructor
public class EvolucaoController {

    private final EvolucaoService evolucaoService;

    @GetMapping("/municipios")
    public List<String> municipios() {
        return evolucaoService.listarMunicipiosDisponiveis();
    }

    @GetMapping("/periodos")
    public PeriodosDisponiveisDTO periodos() {
        return evolucaoService.listarPeriodosDisponiveis();
    }

    @GetMapping("/series")
    public List<EvolucaoMunicipioDTO> series(
        @RequestParam(value = "municipio", required = false) List<String> municipios,
        @RequestParam(value = "mesInicio", required = false) String mesInicio,
        @RequestParam(value = "mesFim", required = false) String mesFim,
        @RequestParam(value = "anoRef", required = false) Integer anoReferencia
    ) {
        EvolucaoFiltro filtro = montarFiltro(municipios, mesInicio, mesFim, anoReferencia);
        return evolucaoService.evolucaoMunicipios(filtro);
    }

    @GetMapping("/series-detalhadas")
    public List<CestaBasicaSerieMunicipioDTO> seriesDetalhadas(
        @RequestParam(value = "municipio", required = false) List<String> municipios,
        @RequestParam(value = "mesInicio", required = false) String mesInicio,
        @RequestParam(value = "mesFim", required = false) String mesFim,
        @RequestParam(value = "anoRef", required = false) Integer anoReferencia
    ) {
        EvolucaoFiltro filtro = montarFiltro(municipios, mesInicio, mesFim, anoReferencia);
        return evolucaoService.serieHistoricaPorMunicipio(filtro);
    }

    @GetMapping("/indicadores")
    public EvolucaoIndicadoresDTO indicadores(
        @RequestParam(value = "municipio", required = false) List<String> municipios,
        @RequestParam(value = "mesInicio", required = false) String mesInicio,
        @RequestParam(value = "mesFim", required = false) String mesFim,
        @RequestParam(value = "anoRef", required = false) Integer anoReferencia
    ) {
        EvolucaoFiltro filtro = montarFiltro(municipios, mesInicio, mesFim, anoReferencia);
        return evolucaoService.calcularIndicadores(filtro);
    }

    private EvolucaoFiltro montarFiltro(List<String> municipios,
                                        String mesInicio,
                                        String mesFim,
                                        Integer anoReferencia) {
        Set<String> selecionados = municipios == null ? Set.of() : municipios.stream()
            .filter(item -> item != null && !item.trim().isEmpty())
            .map(String::trim)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        YearMonth inicio = evolucaoService.interpretarMesAno(mesInicio);
        YearMonth fim = evolucaoService.interpretarMesAno(mesFim);
        Integer ano = (anoReferencia != null && anoReferencia > 0) ? anoReferencia : null;

        return EvolucaoFiltro.builder()
            .municipios(selecionados)
            .mesInicio(inicio)
            .mesFim(fim)
            .anoReferencia(ano)
            .build();
    }
}
