package com.viajajunto.api.modules.destination.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "destinos_catalogo")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DestinoCatalogoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String pais;

    private String codigoPaisIso; // Ex: BRA, USA, FRA (para o jsVectormap do RF11)

    private String cidade;

    private String estado;

    @Column(length = 2000)
    private String descricao;

    private String categoria; // praia, cidade, natureza, cultural

    private String fotoUrl;

    @Builder.Default
    private Double avaliacaoMedia = 0.0;

    @Builder.Default
    private Integer totalAvaliacoes = 0;
}
