package br.com.shop2.model.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvolucaoVariacaoDTO {

    private Double percentual;
    private String descricao;
    private Integer anoReferencia;
    private Integer municipiosConsiderados;
}
