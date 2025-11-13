package br.com.shop2.model.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvolucaoTendenciaDTO {

    private Double percentual;
    private String texto;
    private String descricao;
    private String classe;
    private String status;
    private Integer municipiosConsiderados;
}
