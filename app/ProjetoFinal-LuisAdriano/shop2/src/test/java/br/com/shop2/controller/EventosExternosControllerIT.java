package br.com.shop2.controller;

import br.com.shop2.domain.repository.EventoExternoRepository;
import br.com.shop2.domain.repository.GastoMensalRepository;
import br.com.shop2.model.dados.GastoMensal;
import br.com.shop2.model.evento.EventoExterno;
import br.com.shop2.model.evento.Impacto;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EventosExternosControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventoExternoRepository eventoExternoRepository;

    @Autowired
    private GastoMensalRepository gastoMensalRepository;

    @BeforeEach
    void limparBase() {
        eventoExternoRepository.deleteAll();
        gastoMensalRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /dados/eventos deve persistir novo evento e redirecionar")
    void deveCriarEventoViaFormulario() throws Exception {
        mockMvc.perform(post("/dados/eventos")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("titulo", "Greve de caminhoneiros")
                .param("descricao", "Impacto na logística em abril")
                .param("municipios", "São Paulo / SP")
                .param("impacto", "NEGATIVO")
                .param("dataInicio", "2024-04-01")
                .param("dataFim", "2024-04-30"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/dados#eventos-externos"));

        assertThat(eventoExternoRepository.count()).isEqualTo(1);
        EventoExterno salvo = eventoExternoRepository.findAll().get(0);
        assertThat(salvo.getTitulo()).isEqualTo("Greve de caminhoneiros");
        assertThat(salvo.getMunicipios()).containsExactly("São Paulo / SP");
        assertThat(salvo.getDataInicio()).isEqualTo(LocalDate.of(2024, 4, 1));
        assertThat(salvo.getDataFim()).isEqualTo(LocalDate.of(2024, 4, 30));
        assertThat(salvo.getImpacto()).isEqualTo(Impacto.NEGATIVO);
    }

    @Test
    @DisplayName("GET /api/eventos-externos deve aplicar filtros e calcular cesta média")
    void deveListarEventosFiltradosComValorDeCesta() throws Exception {
        gastoMensalRepository.save(GastoMensal.builder()
            .municipio("São Paulo / SP")
            .mesAno(YearMonth.of(2024, 1))
            .totalCesta(new BigDecimal("300.00"))
            .build());
        gastoMensalRepository.save(GastoMensal.builder()
            .municipio("São Paulo / SP")
            .mesAno(YearMonth.of(2024, 2))
            .totalCesta(new BigDecimal("320.00"))
            .build());
        gastoMensalRepository.save(GastoMensal.builder()
            .municipio("Rio de Janeiro / RJ")
            .mesAno(YearMonth.of(2024, 1))
            .totalCesta(new BigDecimal("280.00"))
            .build());
        gastoMensalRepository.save(GastoMensal.builder()
            .municipio("Rio de Janeiro / RJ")
            .mesAno(YearMonth.of(2024, 2))
            .totalCesta(new BigDecimal("290.00"))
            .build());

        eventoExternoRepository.save(EventoExterno.builder()
            .titulo("Chuvas intensas")
            .descricao("Aumento nos custos logísticos")
            .municipios(List.of("São Paulo / SP", "Rio de Janeiro / RJ"))
            .impacto(Impacto.NEGATIVO)
            .dataInicio(LocalDate.of(2024, 1, 10))
            .dataFim(LocalDate.of(2024, 2, 15))
            .build());

        mockMvc.perform(get("/api/eventos-externos")
                .param("municipios", "SAO PAULO / SP")
                .param("inicio", "2024-01-01")
                .param("fim", "2024-12-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.hasSize(1)))
            .andExpect(jsonPath("$[0].titulo").value("Chuvas intensas"))
            .andExpect(jsonPath("$[0].impacto").value("NEGATIVO"))
            .andExpect(jsonPath("$[0].municipios", Matchers.contains("São Paulo / SP", "Rio de Janeiro / RJ")))
            .andExpect(jsonPath("$[0].periodoInicio").value("2024-01"))
            .andExpect(jsonPath("$[0].valorMedioCesta", Matchers.closeTo(297.5, 0.001)));
    }
}
