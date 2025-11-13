package br.com.shop2.model.common;

import lombok.Builder;
import lombok.Getter;

import java.time.YearMonth;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Builder
public class EvolucaoFiltro {

    @Builder.Default
    private final Set<String> municipios = new LinkedHashSet<>();
    private final YearMonth mesInicio;
    private final YearMonth mesFim;
    private final Integer anoReferencia;

    public Set<String> getMunicipios() {
        if (municipios == null || municipios.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> resultado = new LinkedHashSet<>();
        for (String municipio : municipios) {
            if (municipio == null) {
                continue;
            }
            String valor = municipio.trim();
            if (!valor.isEmpty()) {
                resultado.add(valor);
            }
        }
        return Collections.unmodifiableSet(resultado);
    }

    public boolean possuiMunicipios() {
        return !getMunicipios().isEmpty();
    }

    public YearMonth getMesInicio() {
        return mesInicio;
    }

    public YearMonth getMesFim() {
        return mesFim;
    }

    public Integer getAnoReferencia() {
        return anoReferencia;
    }

    public EvolucaoFiltro normalizarMunicipios(java.util.function.Function<String, String> normalizador) {
        if (!possuiMunicipios()) {
            return this;
        }
        Set<String> normalizados = new LinkedHashSet<>();
        for (String municipio : municipios) {
            String normalizado = normalizador.apply(municipio);
            if (normalizado != null && !normalizado.isBlank()) {
                normalizados.add(normalizado);
            }
        }
        return EvolucaoFiltro.builder()
            .municipios(normalizados)
            .mesInicio(mesInicio)
            .mesFim(mesFim)
            .anoReferencia(anoReferencia)
            .build();
    }

    public EvolucaoFiltro limparMunicipiosVazios() {
        if (!possuiMunicipios()) {
            return this;
        }
        Set<String> filtrados = new LinkedHashSet<>();
        for (String municipio : municipios) {
            if (municipio != null && !municipio.isBlank()) {
                filtrados.add(municipio);
            }
        }
        return EvolucaoFiltro.builder()
            .municipios(filtrados)
            .mesInicio(mesInicio)
            .mesFim(mesFim)
            .anoReferencia(anoReferencia)
            .build();
    }
}
