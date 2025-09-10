package br.com.shop2.model.common;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CestaBasicaPreviewDTO {

    private String municipio;


    private String mesAno;


    private Double valor;
}
