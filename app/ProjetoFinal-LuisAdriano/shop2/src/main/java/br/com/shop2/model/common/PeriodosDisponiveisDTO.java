package br.com.shop2.model.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeriodosDisponiveisDTO {

    private List<Integer> anos;
    private List<String> meses;
    private Map<Integer, List<String>> mesesPorAno;
}

