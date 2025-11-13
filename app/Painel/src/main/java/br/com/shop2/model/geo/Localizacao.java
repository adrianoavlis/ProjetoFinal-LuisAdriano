package br.com.shop2.model.geo;

import br.com.shop2.model.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity @Table(name = "localizacao")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Localizacao extends BaseEntity {

	private static final long serialVersionUID = 1L;
	
    @NotBlank @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String logradouro;

    @Size(max = 20)
    @Column(length = 20)
    private String numero;

    @Size(max = 9)
    @Column(length = 9)
    private String cep;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "municipio_id", nullable = false)
    private Municipio municipio;

    @Size(max = 120)
    @Column(length = 120)
    private String complemento;
}