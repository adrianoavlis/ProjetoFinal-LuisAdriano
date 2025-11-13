package br.com.shop2.controller;

import br.com.shop2.domain.service.CestaBasicaService;
import br.com.shop2.domain.service.EventoExternoService;
import br.com.shop2.domain.service.GastoMensalService;
import br.com.shop2.domain.service.GastoMensalService.ImportacaoResultado;
import br.com.shop2.model.dados.GastoMensal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(DadosController.class)
class DadosControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GastoMensalService gastoMensalService;

    @MockBean
    private EventoExternoService eventoExternoService;

    @MockBean
    private CestaBasicaService cestaBasicaService;

    @Test
    @DisplayName("GET /dados deve renderizar a view com filtros e paginação")
    void deveRenderizarTelaDados() throws Exception {
        YearMonth periodo = YearMonth.of(2024, 7);
        Page<GastoMensal> pagina = new PageImpl<>(List.of(), PageRequest.of(0, 20, Sort.by("mesAno")), 0);

        when(gastoMensalService.parseEntrada("072024")).thenReturn(periodo);
        when(gastoMensalService.listar(eq("São Paulo"), eq(periodo), any(Pageable.class))).thenReturn(pagina);
        when(gastoMensalService.listarPeriodosImportados()).thenReturn(List.of(periodo));
        when(eventoExternoService.listarTodos()).thenReturn(List.of());
        when(cestaBasicaService.listarMunicipiosDisponiveis()).thenReturn(List.of("São Paulo / SP"));

        mockMvc.perform(get("/dados")
                .param("municipio", "São Paulo")
                .param("periodo", "072024")
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(view().name("dados"))
            .andExpect(model().attributeExists("dados", "municipioFiltro", "periodoFiltro", "tamanhosPermitidos", "eventosExternos", "municipiosEventos", "eventoForm"));
    }

    @Test
    @DisplayName("POST /dados/importar deve delegar ao serviço e redirecionar com resumo")
    void deveImportarDadosComSucesso() throws Exception {
        ImportacaoResultado resultado = ImportacaoResultado.builder()
            .totalProcessados(10)
            .totalInseridos(5)
            .totalAtualizados(3)
            .periodosSucesso(List.of(YearMonth.of(2024, 7)))
            .errosPorPeriodo(Map.of())
            .build();

        when(gastoMensalService.importar("012024", "022024", false)).thenReturn(resultado);

        mockMvc.perform(post("/dados/importar")
                .param("dataInicial", "012024")
                .param("dataFinal", "022024")
                .param("sequencial", "false"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/dados"))
            .andExpect(flash().attributeExists("resultadoImportacao"));

        Mockito.verify(gastoMensalService).importar("012024", "022024", false);
    }
}

