package br.com.shop2.model.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvolucaoMunicipioDTO {

    private String id;
    private String nome;
    private String uf;
    private List<CestaBasicaSerieDTO> serie;
}
