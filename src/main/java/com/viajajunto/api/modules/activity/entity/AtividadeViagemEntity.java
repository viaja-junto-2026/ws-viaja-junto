package com.viajajunto.api.modules.activity.entity;

import com.viajajunto.api.modules.destination.entity.DestinoViagemEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "atividades_viagem")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtividadeViagemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destino_viagem_id", nullable = false)
    private DestinoViagemEntity destinoViagem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalogo_atividade_id")
    private CatalogoAtividadeEntity catalogoAtividade;

    @Column(nullable = false)
    private String nome;

    private String tipo; // Passeio, Gastronomia, Hospedagem, Transporte, Cultura

    private String local;

    private String fotoUrl;

    @Column(length = 1000)
    private String descricao;

    private LocalDateTime dataHorario;

    private Integer duracaoMinutos;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal custoPrevisto = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusAtividade status = StatusAtividade.PENDENTE;
}
