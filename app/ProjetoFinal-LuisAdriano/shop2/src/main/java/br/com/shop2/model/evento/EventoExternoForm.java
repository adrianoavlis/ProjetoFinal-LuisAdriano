package br.com.shop2.model.evento;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventoExternoForm {

    private Long id;

    @NotNull(message = "Informe um título para o evento.")
    @Size(max = 150, message = "O título deve ter no máximo 150 caracteres.")
    private String titulo;

    @Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres.")
    private String descricao;

    @NotNull(message = "Informe a data inicial do evento.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataInicio;

    @NotNull(message = "Informe a data final do evento.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataFim;

    @Builder.Default
    private List<String> municipios = new ArrayList<>();

    @NotNull(message = "Informe o impacto do evento.")
    private Impacto impacto;

    @AssertTrue(message = "A data final deve ser igual ou posterior à data inicial.")
    public boolean isPeriodoValido() {
        if (dataInicio == null || dataFim == null) {
            return true;
        }
        return !dataFim.isBefore(dataInicio);
    }

    @AssertTrue(message = "Informe ao menos um município impactado.")
    public boolean isMunicipiosValidos() {
        if (municipios == null || municipios.isEmpty()) {
            return false;
        }
        List<String> normalizados = municipios.stream()
            .map(valor -> valor != null ? valor.trim() : null)
            .filter(Objects::nonNull)
            .filter(valor -> !valor.isEmpty())
            .collect(Collectors.toList());
        if (normalizados.isEmpty()) {
            return false;
        }
        return normalizados.stream().allMatch(valor -> valor.length() <= 180);
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo != null ? titulo.trim() : null;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public void setMunicipios(Collection<String> municipios) {
        this.municipios = municipios == null ? new ArrayList<>() : new ArrayList<>(municipios);
    }
}
