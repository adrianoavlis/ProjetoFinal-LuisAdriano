package com.api.shop2.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cesta_basica")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CestaBasica {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String estado;
    private String mesAno;
    private Double valor;
}
