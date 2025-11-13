package br.com.shop2.model.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class CestaBasicaPesoMunicipioDTO {

    private String id;
    private String nome;
    private String uf;
    private String rotulo;
    private String periodoInicio;
    private String periodoFim;
    private Double totalMedio;
    private List<CestaBasicaComponentePesoDTO> componentes;
    private CestaBasicaPesoDestaquesDTO destaques;
}
