package br.com.shop2.model.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class CestaBasicaSerieDTO {

    private String mes;
    private Double cesta;
    private Map<String, Double> componentes;
}
