package com.viajajunto.api.modules.budget.service;

import com.viajajunto.api.core.security.TripSecurityService;
import com.viajajunto.api.modules.activity.entity.AtividadeViagemEntity;
import com.viajajunto.api.modules.activity.repository.AtividadeViagemRepository;
import com.viajajunto.api.modules.budget.dto.OrcamentoResponseDTO;
import com.viajajunto.api.modules.budget.entity.OrcamentoEntity;
import com.viajajunto.api.modules.budget.repository.OrcamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrcamentoServiceTest {

    @Mock
    private OrcamentoRepository orcamentoRepository;

    @Mock
    private AtividadeViagemRepository atividadeViagemRepository;

    @Mock
    private TripSecurityService tripSecurityService;

    @InjectMocks
    private OrcamentoService orcamentoService;

    private OrcamentoEntity sampleOrcamento;
    private List<AtividadeViagemEntity> sampleAtividades;

    @BeforeEach
    void setUp() {
        sampleOrcamento = OrcamentoEntity.builder()
                .id(1L)
                .orcamentoTotal(BigDecimal.valueOf(1000.00))
                .build();

        sampleAtividades = List.of(
                AtividadeViagemEntity.builder()
                        .id(1L)
                        .nome("Jantar Típico")
                        .tipo("Gastronomia")
                        .custoPrevisto(BigDecimal.valueOf(200.00))
                        .build(),
                AtividadeViagemEntity.builder()
                        .id(2L)
                        .nome("Museu do Vaticano")
                        .tipo("Passeio")
                        .custoPrevisto(BigDecimal.valueOf(300.00))
                        .build()
        );
    }

    @Test
    @DisplayName("Deve calcular resumo de orçamento, saldo disponível e percentuais por categoria corretamente")
    void shouldCalculateBudgetSummaryCorrectly() {
        when(orcamentoRepository.findByViagemId(10L)).thenReturn(Optional.of(sampleOrcamento));
        when(atividadeViagemRepository.findAllByViagemId(10L)).thenReturn(sampleAtividades);

        OrcamentoResponseDTO resumo = orcamentoService.getResumoOrcamento(10L, 1L);

        assertNotNull(resumo);
        assertEquals(BigDecimal.valueOf(1000.00), resumo.getOrcamentoTotal());
        assertEquals(BigDecimal.valueOf(500.00), resumo.getTotalPlanejado());
        assertEquals(BigDecimal.valueOf(500.00), resumo.getSaldoDisponivel());
        assertEquals(50.0, resumo.getPercentualConsumidoTotal());
        assertEquals(2, resumo.getGastosPorCategoria().size());

        verify(tripSecurityService, times(1)).validateUserCanViewTrip(10L, 1L);
    }
}
