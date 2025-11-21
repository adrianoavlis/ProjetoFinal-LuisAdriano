package br.com.shop2.model.evento;

import br.com.shop2.model.common.BaseEntity;
import br.com.shop2.model.common.Municipios;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "eventos_externos_municipios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventoExternoMunicipio extends BaseEntity {

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "evento_externo_id", nullable = false)
    private EventoExterno eventoExterno;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "municipio", nullable = false, length = 50)
    private Municipios municipio;
}
