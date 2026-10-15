package com.viajajunto.api.modules.budget.service;

import com.viajajunto.api.core.security.TripSecurityService;
import com.viajajunto.api.modules.activity.entity.AtividadeViagemEntity;
import com.viajajunto.api.modules.activity.repository.AtividadeViagemRepository;
import com.viajajunto.api.modules.budget.dto.CategoriaGastoDTO;
import com.viajajunto.api.modules.budget.dto.OrcamentoResponseDTO;
import com.viajajunto.api.modules.budget.dto.UpdateOrcamentoDTO;
import com.viajajunto.api.modules.budget.entity.OrcamentoEntity;
import com.viajajunto.api.modules.budget.repository.OrcamentoRepository;
import com.viajajunto.api.modules.trip.entity.ViagemEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrcamentoService {

    private final OrcamentoRepository orcamentoRepository;
    private final AtividadeViagemRepository atividadeViagemRepository;
    private final TripSecurityService tripSecurityService;

    @Transactional(readOnly = true)
    public OrcamentoResponseDTO getResumoOrcamento(Long viagemId, Long userId) {
        tripSecurityService.validateUserCanViewTrip(viagemId, userId);

        OrcamentoEntity orcamento = orcamentoRepository.findByViagemId(viagemId)
                .orElseGet(() -> OrcamentoEntity.builder()
                        .orcamentoTotal(BigDecimal.ZERO)
                        .build());

        List<AtividadeViagemEntity> atividades = atividadeViagemRepository.findAllByViagemId(viagemId);

        BigDecimal totalPlanejado = atividades.stream()
                .map(a -> a.getCustoPrevisto() != null ? a.getCustoPrevisto() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal orcamentoTotal = orcamento.getOrcamentoTotal() != null ? orcamento.getOrcamentoTotal() : BigDecimal.ZERO;
        BigDecimal saldoDisponivel = orcamentoTotal.subtract(totalPlanejado);

        Double percentualConsumidoTotal = 0.0;
        if (orcamentoTotal.compareTo(BigDecimal.ZERO) > 0) {
            percentualConsumidoTotal = totalPlanejado
                    .divide(orcamentoTotal, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        // Agrupamento por categoria
        Map<String, BigDecimal> gastosPorCatMap = atividades.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getTipo() != null ? a.getTipo() : "Outros",
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                a -> a.getCustoPrevisto() != null ? a.getCustoPrevisto() : BigDecimal.ZERO,
                                BigDecimal::add
                        )
                ));

        List<CategoriaGastoDTO> gastosPorCategoria = gastosPorCatMap.entrySet().stream()
                .map(entry -> {
                    Double pct = 0.0;
                    if (totalPlanejado.compareTo(BigDecimal.ZERO) > 0) {
                        pct = entry.getValue()
                                .divide(totalPlanejado, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                                .doubleValue();
                    }
                    return CategoriaGastoDTO.builder()
                            .categoria(entry.getKey())
                            .valorTotal(entry.getValue())
                            .percentualConsumido(pct)
                            .build();
                })
                .collect(Collectors.toList());

        return OrcamentoResponseDTO.builder()
                .viagemId(viagemId)
                .orcamentoTotal(orcamentoTotal)
                .totalPlanejado(totalPlanejado)
                .saldoDisponivel(saldoDisponivel)
                .percentualConsumidoTotal(percentualConsumidoTotal)
                .gastosPorCategoria(gastosPorCategoria)
                .build();
    }

    @Transactional
    public OrcamentoResponseDTO updateOrcamentoTotal(Long viagemId, UpdateOrcamentoDTO dto, Long userId) {
        ViagemEntity viagem = tripSecurityService.validateUserCanEditTrip(viagemId, userId);

        OrcamentoEntity orcamento = orcamentoRepository.findByViagemId(viagemId)
                .orElseGet(() -> OrcamentoEntity.builder()
                        .viagem(viagem)
                        .build());

        orcamento.setOrcamentoTotal(dto.getOrcamentoTotal());
        orcamentoRepository.save(orcamento);

        return getResumoOrcamento(viagemId, userId);
    }
}
