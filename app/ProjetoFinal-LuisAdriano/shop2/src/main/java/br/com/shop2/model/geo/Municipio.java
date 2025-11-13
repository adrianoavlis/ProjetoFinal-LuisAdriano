package br.com.shop2.model.geo;

import br.com.shop2.model.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity @Table(name = "municipio",
        uniqueConstraints = @UniqueConstraint(name = "uk_municipio_nome_estado", columnNames = {"nome", "estado_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Municipio extends BaseEntity {

	private static final long serialVersionUID = 1L;
	
    @NotBlank @Size(max = 180)
    @Column(nullable = false, length = 180)
    private String nome;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_id", nullable = false)
    private Estado estado;

}