package br.com.shop2.model.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class CestaBasicaPesoResumoItemDTO {

    private String chave;
    private Double valor;
    private Double percentual;
    private Double variacao;
    private Double valorInicial;
    private Double valorFinal;
    private String mesInicial;
    private String mesFinal;
}
