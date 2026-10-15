package com.viajajunto.api.modules.destination.entity;

import com.viajajunto.api.modules.trip.entity.ViagemEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "destinos_viagem")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DestinoViagemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viagem_id", nullable = false)
    private ViagemEntity viagem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destino_catalogo_id")
    private DestinoCatalogoEntity destinoCatalogo;

    @Column(nullable = false)
    private String nome;

    private String pais;

    private String codigoPaisIso;

    private String localizacao;

    private String fotoUrl;

    @Column(length = 1000)
    private String descricao;

    private String categoria;

    private LocalDate dataChegada;

    private LocalDate dataSaida;

    @Column(nullable = false)
    @Builder.Default
    private Integer ordemVisita = 1;
}
