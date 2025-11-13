package br.com.shop2.model.mercado;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(
    name = "cesta_basica",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_cesta_municipio_mes", columnNames = {"municipio", "mes_ano"})
    },
    indexes = {
        @Index(name = "idx_cesta_municipio_mes", columnList = "municipio, mes_ano")
    }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CestaBasica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // só UM @Id; nada de identity em String
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "municipio", length = 100, nullable = false)
    private String municipio;

    // formato "mm/aaaa" ou "mmaaaa" conforme você estiver persistindo na Service
    @Column(name = "mes_ano", length = 12, nullable = false)
    private String mesAno;

    @Column(name = "valor", nullable = false)
    private Double valor;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;
}
