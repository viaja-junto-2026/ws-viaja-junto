package com.viajajunto.api.modules.activity.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.viajajunto.api.core.exception.ScheduleConflictException;
import com.viajajunto.api.core.security.TripSecurityService;
import com.viajajunto.api.modules.activity.dto.AtividadeResponseDTO;
import com.viajajunto.api.modules.activity.dto.CreateAtividadeDTO;
import com.viajajunto.api.modules.activity.entity.AtividadeViagemEntity;
import com.viajajunto.api.modules.activity.repository.AtividadeViagemRepository;
import com.viajajunto.api.modules.activity.repository.CatalogoAtividadeRepository;
import com.viajajunto.api.modules.destination.entity.DestinoViagemEntity;
import com.viajajunto.api.modules.destination.repository.DestinoViagemRepository;
import com.viajajunto.api.modules.trip.entity.ViagemEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AtividadeViagemServiceTest {

    @Mock
    private AtividadeViagemRepository atividadeViagemRepository;

    @Mock
    private DestinoViagemRepository destinoViagemRepository;

    @Mock
    private CatalogoAtividadeRepository catalogoAtividadeRepository;

    @Mock
    private TripSecurityService tripSecurityService;

    @InjectMocks
    private AtividadeViagemService atividadeViagemService;

    private ViagemEntity sampleViagem;
    private DestinoViagemEntity sampleDestino;

    @BeforeEach
    void setUp() {
        sampleViagem = ViagemEntity.builder()
                .id(1L)
                .nome("Viagem Teste")
                .build();

        sampleDestino = DestinoViagemEntity.builder()
                .id(10L)
                .nome("Destino Teste")
                .viagem(sampleViagem)
                .build();
    }

    @Test
    @DisplayName("Deve adicionar atividade com sucesso quando não há conflito de horário")
    void addAtividade_Success() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 1, 10, 0);
        CreateAtividadeDTO dto = CreateAtividadeDTO.builder()
                .nome("Passeio de Barco")
                .dataHorario(start)
                .duracaoMinutos(60)
                .build();

        when(destinoViagemRepository.findById(10L)).thenReturn(Optional.of(sampleDestino));
        when(atividadeViagemRepository.findAllByViagemId(1L)).thenReturn(List.of());
        when(atividadeViagemRepository.save(any(AtividadeViagemEntity.class))).thenAnswer(i -> {
            AtividadeViagemEntity a = i.getArgument(0);
            a.setId(100L);
            return a;
        });

        AtividadeResponseDTO response = atividadeViagemService.addAtividade(1L, 10L, dto, 1L);

        assertNotNull(response);
        assertEquals("Passeio de Barco", response.getNome());
        verify(atividadeViagemRepository).save(any(AtividadeViagemEntity.class));
    }

    @Test
    @DisplayName("Deve lançar ScheduleConflictException quando horários de atividades se sobrepõem")
    void addAtividade_ThrowsScheduleConflictException() {
        LocalDateTime existingStart = LocalDateTime.of(2026, 11, 1, 10, 0);
        AtividadeViagemEntity existing = AtividadeViagemEntity.builder()
                .id(50L)
                .nome("Tour Museu")
                .dataHorario(existingStart)
                .duracaoMinutos(90) // 10:00 às 11:30
                .build();

        LocalDateTime newStart = LocalDateTime.of(2026, 11, 1, 11, 0); // 11:00 às 12:00 -> conflito!
        CreateAtividadeDTO dto = CreateAtividadeDTO.builder()
                .nome("Almoço Especial")
                .dataHorario(newStart)
                .duracaoMinutos(60)
                .build();

        when(destinoViagemRepository.findById(10L)).thenReturn(Optional.of(sampleDestino));
        when(atividadeViagemRepository.findAllByViagemId(1L)).thenReturn(List.of(existing));

        assertThrows(ScheduleConflictException.class, () ->
                atividadeViagemService.addAtividade(1L, 10L, dto, 1L)
        );

        verify(atividadeViagemRepository, never()).save(any(AtividadeViagemEntity.class));
    }

    @Test
    @DisplayName("Deve lançar ScheduleConflictException para atividades sem duração no mesmo horário")
    void addAtividade_ThrowsScheduleConflictException_WhenSameStartWithoutDuration() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 1, 10, 0);
        AtividadeViagemEntity existing = AtividadeViagemEntity.builder()
                .id(50L)
                .nome("Check-in")
                .dataHorario(start)
                .build();

        CreateAtividadeDTO dto = CreateAtividadeDTO.builder()
                .nome("Retirar chaves")
                .dataHorario(start)
                .build();

        when(destinoViagemRepository.findById(10L)).thenReturn(Optional.of(sampleDestino));
        when(atividadeViagemRepository.findAllByViagemId(1L)).thenReturn(List.of(existing));

        assertThrows(ScheduleConflictException.class, () ->
                atividadeViagemService.addAtividade(1L, 10L, dto, 1L)
        );

        verify(atividadeViagemRepository, never()).save(any(AtividadeViagemEntity.class));
    }
}
