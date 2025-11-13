package br.com.shop2.model.geo;

import br.com.shop2.model.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity @Table(name = "estado",
        uniqueConstraints = @UniqueConstraint(name = "uk_estado_sigla_pais", columnNames = {"sigla", "pais_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Estado extends BaseEntity {
	
	private static final long serialVersionUID = 1L;
	
    @NotBlank @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String nome;

    @NotBlank @Size(min = 2, max = 2)
    @Column(nullable = false, length = 2)
    private String sigla; // RJ, SP...

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "pais_id", nullable = false)
    private Pais pais;
}