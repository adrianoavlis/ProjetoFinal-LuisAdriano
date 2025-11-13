package br.com.shop2.model.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class CestaBasicaComponentePesoDTO {

    private String chave;
    private Double media;
    private Double percentual;
    private Double variacao;
    private Double valorInicial;
    private Double valorFinal;
    private String mesInicial;
    private String mesFinal;
}
