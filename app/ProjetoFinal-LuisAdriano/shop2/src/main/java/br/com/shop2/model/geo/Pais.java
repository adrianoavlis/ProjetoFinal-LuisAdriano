package br.com.shop2.model.geo;

import br.com.shop2.model.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity @Table(name = "pais")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Pais extends BaseEntity {
	
	private static final long serialVersionUID = 1L;
	
    @NotBlank @Size(max = 120)
    @Column(nullable = false, unique = true, length = 120)
    private String nome;

    @NotBlank @Size(min = 2, max = 3)
    @Column(nullable = false, unique = true, length = 3)
    private String sigla; // BR, USA...
}