package br.com.shop2.model.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvolucaoIndicadorPrecoDTO {

    private BigDecimal valor;
    private String municipio;
    private String municipioId;
    private String mes;
    private String mesDescricao;
    private String observacao;
}
