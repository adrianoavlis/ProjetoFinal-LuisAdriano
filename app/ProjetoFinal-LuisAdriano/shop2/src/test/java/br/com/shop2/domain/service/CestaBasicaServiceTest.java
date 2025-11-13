package br.com.shop2.domain.service;

import br.com.shop2.domain.repository.GastoMensalRepository;
import br.com.shop2.model.common.CestaBasicaSerieDTO;
import br.com.shop2.model.common.EvolucaoFiltro;
import br.com.shop2.model.common.EvolucaoMunicipioDTO;
import br.com.shop2.model.dados.GastoMensal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CestaBasicaServiceTest {

    @Mock
    private GastoMensalRepository gastoMensalRepository;

    @InjectMocks
    private CestaBasicaService service;

    @Test
    void deveGerarSerieAgrupadaPorMunicipioComDadosDoRepositorio() {
        YearMonth janeiro = YearMonth.of(2024, 1);
        YearMonth fevereiro = YearMonth.of(2024, 2);

        List<GastoMensal> registros = List.of(
            criarGasto("Florianópolis / SC", janeiro, BigDecimal.valueOf(110)),
            criarGasto("Florianopolis / SC", janeiro, BigDecimal.valueOf(90)),
            criarGasto("Florianópolis / SC", fevereiro, BigDecimal.valueOf(130)),
            criarGasto("São José / SC", janeiro, BigDecimal.valueOf(80))
        );

        when(gastoMensalRepository.findAll(any(Specification.class))).thenReturn(registros);
        EvolucaoFiltro filtro = EvolucaoFiltro.builder()
            .municipios(new LinkedHashSet<>(Set.of("FLORIANOPOLIS-SC")))
            .mesInicio(janeiro)
            .mesFim(fevereiro)
            .build();

        List<EvolucaoMunicipioDTO> resultado = service.evolucaoMunicipios(filtro);

        assertThat(resultado).hasSize(1);

        EvolucaoMunicipioDTO municipio = resultado.getFirst();
        assertThat(municipio.getId()).isEqualTo("FLORIANOPOLIS-SC");
        assertThat(municipio.getNome()).isEqualTo("Florianópolis");

        List<CestaBasicaSerieDTO> serie = municipio.getSerie();
        assertThat(serie).hasSize(2);
        assertThat(serie.getFirst().getMes()).isEqualTo("Jan-2024");
        assertThat(serie.getFirst().getCesta()).isEqualTo(100.0);
        assertThat(serie.get(1).getMes()).isEqualTo("Feb-2024");
        assertThat(serie.get(1).getCesta()).isEqualTo(130.0);
    }

    @Test
    void serieHistoricaCustoTotalDeveIgnorarValoresNaoPositivos() {
        YearMonth janeiro = YearMonth.of(2024, 1);
        YearMonth fevereiro = YearMonth.of(2024, 2);

        List<GastoMensal> registros = List.of(
            criarGasto("Florianópolis / SC", janeiro, BigDecimal.valueOf(120)),
            criarGasto("Florianópolis / SC", janeiro, BigDecimal.ZERO),
            criarGasto("Florianópolis / SC", fevereiro, BigDecimal.ZERO),
            criarGasto("Florianópolis / SC", fevereiro, BigDecimal.valueOf(-80))
        );

        when(gastoMensalRepository.findAll()).thenReturn(registros);

        List<CestaBasicaSerieDTO> serie = service.serieHistoricaCustoTotal();

        assertThat(serie).hasSize(1);
        CestaBasicaSerieDTO pontoJaneiro = serie.getFirst();
        assertThat(pontoJaneiro.getMes()).isEqualTo("Jan-2024");
        assertThat(pontoJaneiro.getCesta()).isEqualTo(120.0);
    }

    @Test
    void deveIgnorarAnoReferenciaQuandoPeriodoSelecionado() {
        YearMonth janeiro = YearMonth.of(2024, 1);
        YearMonth fevereiro = YearMonth.of(2024, 2);
        YearMonth janeiroSeguinte = YearMonth.of(2025, 1);

        List<GastoMensal> registros = List.of(
            criarGasto("Florianópolis / SC", janeiro, BigDecimal.valueOf(110)),
            criarGasto("Florianópolis / SC", fevereiro, BigDecimal.valueOf(130)),
            criarGasto("Florianópolis / SC", janeiroSeguinte, BigDecimal.valueOf(150))
        );

        when(gastoMensalRepository.findAll(any(Specification.class))).thenReturn(registros);

        EvolucaoFiltro filtro = EvolucaoFiltro.builder()
            .mesInicio(janeiro)
            .mesFim(fevereiro)
            .anoReferencia(2025)
            .build();

        List<EvolucaoMunicipioDTO> resultado = service.evolucaoMunicipios(filtro);
        assertThat(resultado).hasSize(1);

        EvolucaoMunicipioDTO municipio = resultado.getFirst();
        List<CestaBasicaSerieDTO> serie = municipio.getSerie();
        assertThat(serie)
            .extracting(CestaBasicaSerieDTO::getMes)
            .containsExactly("Jan-2024", "Feb-2024");
    }

    private static GastoMensal criarGasto(String municipio, YearMonth mes, BigDecimal total) {
        return GastoMensal.builder()
            .municipio(municipio)
            .mesAno(mes)
            .totalCesta(total)
            .build();
    }
}

