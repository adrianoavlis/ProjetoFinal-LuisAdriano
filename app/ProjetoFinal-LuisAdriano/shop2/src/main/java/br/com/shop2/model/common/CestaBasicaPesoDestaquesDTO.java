package br.com.shop2.model.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class CestaBasicaPesoDestaquesDTO {

    private CestaBasicaPesoResumoItemDTO itemMaisCaro;
    private CestaBasicaPesoResumoItemDTO itemMaisBarato;
    private CestaBasicaPesoResumoItemDTO maiorAumento;
    private CestaBasicaPesoResumoItemDTO maiorReducao;
}
