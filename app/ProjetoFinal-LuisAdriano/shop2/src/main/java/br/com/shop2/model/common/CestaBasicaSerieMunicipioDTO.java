package br.com.shop2.model.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class CestaBasicaSerieMunicipioDTO {

    private String municipio;
    private List<CestaBasicaSerieDTO> serie;
}
