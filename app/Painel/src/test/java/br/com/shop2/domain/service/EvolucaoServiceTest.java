package br.com.shop2.domain.service;

import br.com.shop2.domain.repository.EvolucaoRepository;
import br.com.shop2.model.common.CestaBasicaSerieDTO;
import br.com.shop2.model.common.EvolucaoFiltro;
import br.com.shop2.model.common.EvolucaoIndicadoresDTO;
import br.com.shop2.model.common.EvolucaoMunicipioDTO;
import br.com.shop2.model.dados.GastoMensal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvolucaoServiceTest {

    @Mock
    private EvolucaoRepository evolucaoRepository;

    @InjectMocks
    private EvolucaoService service;

    @Test
    void deveIgnorarValoresNaoPositivosEmSeriesEIndicadores() {
        YearMonth janeiro = YearMonth.of(2024, 1);
        YearMonth fevereiro = YearMonth.of(2024, 2);
        YearMonth marco = YearMonth.of(2024, 3);

        List<GastoMensal> registros = List.of(
            GastoMensal.builder()
                .municipio("Cidade / ST")
                .mesAno(janeiro)
                .totalCesta(BigDecimal.valueOf(100))
                .carne(BigDecimal.valueOf(50))
                .leite(BigDecimal.ZERO)
                .feijao(BigDecimal.TEN)
                .build(),
            GastoMensal.builder()
                .municipio("Cidade / ST")
                .mesAno(fevereiro)
                .totalCesta(BigDecimal.ZERO)
                .carne(BigDecimal.valueOf(40))
                .leite(BigDecimal.valueOf(5))
                .build(),
            GastoMensal.builder()
                .municipio("Cidade / ST")
                .mesAno(marco)
                .totalCesta(BigDecimal.valueOf(110))
                .carne(BigDecimal.valueOf(55))
                .arroz(BigDecimal.valueOf(30))
                .build()
        );

        when(evolucaoRepository.buscarPorFiltro(any(EvolucaoFiltro.class))).thenReturn(registros);

        List<GastoMensal> menorPreco = List.of(
            GastoMensal.builder()
                .municipio("Cidade / ST")
                .mesAno(janeiro)
                .totalCesta(BigDecimal.ZERO)
                .build(),
            GastoMensal.builder()
                .municipio("Cidade / ST")
                .mesAno(marco)
                .totalCesta(BigDecimal.valueOf(95))
                .build()
        );

        when(evolucaoRepository.buscarMenorPreco(any(), anyBoolean(), any(), any(), any()))
            .thenReturn(menorPreco);

        List<EvolucaoMunicipioDTO> municipios = service.evolucaoMunicipios();
        assertThat(municipios).hasSize(1);

        EvolucaoMunicipioDTO municipio = municipios.getFirst();
        List<CestaBasicaSerieDTO> serie = municipio.getSerie();
        assertThat(serie).hasSize(2);

        CestaBasicaSerieDTO primeiro = serie.get(0);
        assertThat(primeiro.getMes()).isEqualTo("Jan-2024");
        assertThat(primeiro.getCesta()).isEqualTo(100.0);
        assertThat(primeiro.getComponentes()).containsOnlyKeys("carne", "feijao");
        assertThat(primeiro.getComponentes().get("carne")).isEqualTo(50.0);
        assertThat(primeiro.getComponentes().get("feijao")).isEqualTo(10.0);

        CestaBasicaSerieDTO segundo = serie.get(1);
        assertThat(segundo.getMes()).isEqualTo("Mar-2024");
        assertThat(segundo.getCesta()).isEqualTo(110.0);
        assertThat(segundo.getComponentes()).containsOnlyKeys("carne", "arroz");
        assertThat(segundo.getComponentes().get("carne")).isEqualTo(55.0);
        assertThat(segundo.getComponentes().get("arroz")).isEqualTo(30.0);

        EvolucaoIndicadoresDTO indicadores = service.calcularIndicadores(EvolucaoFiltro.builder().build());
        assertThat(indicadores.getMenorPreco()).isNotNull();
        assertThat(indicadores.getMenorPreco().getValor()).isEqualByComparingTo("95.00");
        assertThat(indicadores.getVariacaoMensal().getPercentual()).isEqualTo(10.0);
        assertThat(indicadores.getVariacaoAnual().getPercentual()).isEqualTo(10.0);
        assertThat(indicadores.getTendencia().getPercentual()).isGreaterThan(0.0);
    }

    @Test
    void deveRestringirEvolucaoAoPeriodoDoFiltro() {
        YearMonth janeiro = YearMonth.of(2023, 1);
        YearMonth fevereiro = YearMonth.of(2023, 2);
        YearMonth marco = YearMonth.of(2023, 3);
        YearMonth abril = YearMonth.of(2023, 4);

        List<GastoMensal> registros = List.of(
            GastoMensal.builder()
                .municipio("Cidade / ST")
                .mesAno(janeiro)
                .totalCesta(BigDecimal.valueOf(100))
                .build(),
            GastoMensal.builder()
                .municipio("Cidade / ST")
                .mesAno(fevereiro)
                .totalCesta(BigDecimal.valueOf(110))
                .build(),
            GastoMensal.builder()
                .municipio("Cidade / ST")
                .mesAno(marco)
                .totalCesta(BigDecimal.valueOf(120))
                .build(),
            GastoMensal.builder()
                .municipio("Cidade / ST")
                .mesAno(abril)
                .totalCesta(BigDecimal.valueOf(130))
                .build()
        );

        when(evolucaoRepository.buscarPorFiltro(any(EvolucaoFiltro.class))).thenReturn(registros);

        EvolucaoFiltro filtro = EvolucaoFiltro.builder()
            .mesInicio(janeiro)
            .mesFim(marco)
            .build();

        List<EvolucaoMunicipioDTO> municipios = service.evolucaoMunicipios(filtro);
        assertThat(municipios).hasSize(1);

        EvolucaoMunicipioDTO municipio = municipios.getFirst();
        List<CestaBasicaSerieDTO> serie = municipio.getSerie();
        assertThat(serie).hasSize(3);
        assertThat(serie)
            .extracting(CestaBasicaSerieDTO::getMes)
            .containsExactly("Jan-2023", "Feb-2023", "Mar-2023");
    }

    @Test
    void devePriorizarPeriodoQuandoAnoReferenciaFornecido() {
        YearMonth janeiro = YearMonth.of(2024, 1);
        YearMonth fevereiro = YearMonth.of(2024, 2);
        YearMonth janeiroSeguinte = YearMonth.of(2025, 1);

        List<GastoMensal> registros = List.of(
            GastoMensal.builder()
                .municipio("Cidade / ST")
                .mesAno(janeiro)
                .totalCesta(BigDecimal.valueOf(100))
                .build(),
            GastoMensal.builder()
                .municipio("Cidade / ST")
                .mesAno(fevereiro)
                .totalCesta(BigDecimal.valueOf(120))
                .build(),
            GastoMensal.builder()
                .municipio("Cidade / ST")
                .mesAno(janeiroSeguinte)
                .totalCesta(BigDecimal.valueOf(140))
                .build()
        );

        when(evolucaoRepository.buscarPorFiltro(any(EvolucaoFiltro.class))).thenReturn(registros);

        EvolucaoFiltro filtro = EvolucaoFiltro.builder()
            .mesInicio(janeiro)
            .mesFim(fevereiro)
            .anoReferencia(2025)
            .build();

        List<EvolucaoMunicipioDTO> municipios = service.evolucaoMunicipios(filtro);
        assertThat(municipios).hasSize(1);

        EvolucaoMunicipioDTO municipio = municipios.getFirst();
        List<CestaBasicaSerieDTO> serie = municipio.getSerie();
        assertThat(serie)
            .extracting(CestaBasicaSerieDTO::getMes)
            .containsExactly("Jan-2024", "Feb-2024");
    }
}
