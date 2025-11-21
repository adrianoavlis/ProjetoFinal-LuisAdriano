package br.com.shop2.model.evento;

import br.com.shop2.model.common.BaseEntity;
import br.com.shop2.model.converter.MunicipiosListaConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "eventos_externos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventoExterno extends BaseEntity {

    @NotBlank
    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @NotNull
    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @NotNull
    @Column(name = "data_fim", nullable = false)
    private LocalDate dataFim;

    @NotNull
    @Convert(converter = MunicipiosListaConverter.class)
    @Column(name = "municipio", nullable = false, length = 1000)
    @Builder.Default
    private List<String> municipios = new ArrayList<>();

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "impacto", nullable = false, length = 20)
    private Impacto impacto;
}
