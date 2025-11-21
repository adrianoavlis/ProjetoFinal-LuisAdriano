package br.com.shop2.model.evento;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventoExternoDetalheDTO {

    private Long id;
    private String titulo;
    private String descricao;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private List<String> municipios;
    private List<String> municipiosId;
    private String municipiosConcatenados;
    private Impacto impacto;
    private BigDecimal valorMedioCesta;
    private String periodoInicio;
    private String periodoFim;
}
