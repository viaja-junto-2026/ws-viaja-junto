package com.viajajunto.api.modules.budget.entity;

import com.viajajunto.api.modules.trip.entity.ViagemEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "orcamentos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrcamentoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viagem_id", nullable = false, unique = true)
    private ViagemEntity viagem;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal orcamentoTotal = BigDecimal.ZERO;
}
