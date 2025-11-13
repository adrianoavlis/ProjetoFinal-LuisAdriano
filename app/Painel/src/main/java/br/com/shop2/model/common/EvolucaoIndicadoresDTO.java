package br.com.shop2.model.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvolucaoIndicadoresDTO {

    private EvolucaoIndicadorPrecoDTO menorPreco;
    private EvolucaoVariacaoDTO variacaoMensal;
    private EvolucaoVariacaoDTO variacaoAnual;
    private EvolucaoTendenciaDTO tendencia;
}
