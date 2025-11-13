package br.com.shop2.model.dados;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder
public class ImportacaoResumoView {
    int totalProcessados;
    int totalInseridos;
    int totalAtualizados;
    List<String> periodosSucesso;
    Map<String, String> errosPorPeriodo;
}
