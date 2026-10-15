package com.viajajunto.api.modules.activity.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "catalogo_atividades")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogoAtividadeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    private String tipo; // Passeio, Gastronomia, Hospedagem, Transporte, Cultura

    private String localizacao;

    private String cidade;

    private String pais;

    @Column(length = 2000)
    private String descricao;

    private String fotoUrl;

    private BigDecimal precoMedio;

    @Builder.Default
    private Double avaliacaoMedia = 0.0;

    @Builder.Default
    private Integer totalAvaliacoes = 0;
}
