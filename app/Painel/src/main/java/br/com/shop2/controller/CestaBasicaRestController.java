package br.com.shop2.controller;

import br.com.shop2.domain.service.CestaBasicaService;
import br.com.shop2.domain.service.EvolucaoService;
import br.com.shop2.model.common.CestaBasicaPesoMunicipioDTO;
import br.com.shop2.model.common.CestaBasicaSerieDTO;
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
@RequiredArgsConstructor
@RequestMapping("/api/cesta")
public class CestaBasicaRestController {

    private final CestaBasicaService service;
    private final EvolucaoService evolucaoService;

    @GetMapping("/serie-historica")
    public List<CestaBasicaSerieDTO> serieHistorica() {
        return service.serieHistoricaCustoTotal();
    }

    @GetMapping("/municipios")
    public List<String> municipios() {
        return service.listarMunicipiosDisponiveis();
    }

    @GetMapping("/periodos")
    public PeriodosDisponiveisDTO periodosDisponiveis() {
        return service.listarPeriodosDisponiveis();
    }

    @GetMapping("/serie-municipios")
    public List<CestaBasicaSerieMunicipioDTO> seriePorMunicipio(
            @RequestParam(value = "municipio", required = false) List<String> municipios,
            @RequestParam(value = "mesInicio", required = false) String mesInicio,
            @RequestParam(value = "mesFim", required = false) String mesFim,
            @RequestParam(value = "anoRef", required = false) Integer anoReferencia
    ) {
        EvolucaoFiltro filtro = montarFiltro(municipios, mesInicio, mesFim, anoReferencia);
        return evolucaoService.serieHistoricaPorMunicipio(filtro);
    }

    @GetMapping("/evolucao-municipios")
    public List<EvolucaoMunicipioDTO> evolucaoMunicipios(
            @RequestParam(value = "municipio", required = false) List<String> municipios,
            @RequestParam(value = "mesInicio", required = false) String mesInicio,
            @RequestParam(value = "mesFim", required = false) String mesFim,
            @RequestParam(value = "anoRef", required = false) Integer anoReferencia
    ) {
        EvolucaoFiltro filtro = montarFiltro(municipios, mesInicio, mesFim, anoReferencia);
        return evolucaoService.evolucaoMunicipios(filtro);
    }

    @GetMapping("/evolucao/indicadores")
    public EvolucaoIndicadoresDTO indicadoresEvolucao(
            @RequestParam(value = "municipio", required = false) List<String> municipios,
            @RequestParam(value = "mesInicio", required = false) String mesInicio,
            @RequestParam(value = "mesFim", required = false) String mesFim,
            @RequestParam(value = "anoRef", required = false) Integer anoReferencia
    ) {
        EvolucaoFiltro filtro = montarFiltro(municipios, mesInicio, mesFim, anoReferencia);
        return evolucaoService.calcularIndicadores(filtro);
    }

    @GetMapping("/peso-municipios")
    public List<CestaBasicaPesoMunicipioDTO> pesoPorMunicipio(
            @RequestParam(value = "municipio", required = false) List<String> municipios,
            @RequestParam(value = "mesInicio", required = false) String mesInicio,
            @RequestParam(value = "mesFim", required = false) String mesFim,
            @RequestParam(value = "anoRef", required = false) Integer anoReferencia
    ) {
        EvolucaoFiltro filtro = montarFiltro(municipios, mesInicio, mesFim, anoReferencia);
        return evolucaoService.calcularPesoComponentes(filtro);
    }

    private EvolucaoFiltro montarFiltro(List<String> municipios,
                                        String mesInicio,
                                        String mesFim,
                                        Integer anoReferencia) {
        Set<String> selecionados = municipios == null ? Set.of() : municipios.stream()
            .filter(item -> item != null && !item.trim().isEmpty())
            .map(String::trim)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        YearMonth inicio = service.interpretarMesAno(mesInicio);
        YearMonth fim = service.interpretarMesAno(mesFim);
        Integer ano = (anoReferencia != null && anoReferencia > 0) ? anoReferencia : null;

        return EvolucaoFiltro.builder()
            .municipios(selecionados)
            .mesInicio(inicio)
            .mesFim(fim)
            .anoReferencia(ano)
            .build();
    }
}
