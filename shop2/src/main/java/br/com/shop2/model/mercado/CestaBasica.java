package br.com.shop2.model.mercado;

import br.com.shop2.model.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cesta_basica",
       indexes = {
           @Index(name = "idx_municipio_mes", columnList = "municipio, mesAno")
       }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class CestaBasica extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(length = 100, nullable = false)
    private String municipio;   

    @Column(length = 7, nullable = false) // "MM-YYYY"
    private String mesAno;

    @Column(nullable = false)
    private Double valor;
}
