package com.viajajunto.api.modules.trip.service;

import com.viajajunto.api.core.exception.BusinessRuleException;
import com.viajajunto.api.core.security.TripSecurityService;
import com.viajajunto.api.modules.auth.entity.UserEntity;
import com.viajajunto.api.modules.auth.repository.UserRepository;
import com.viajajunto.api.modules.budget.entity.OrcamentoEntity;
import com.viajajunto.api.modules.budget.repository.OrcamentoRepository;
import com.viajajunto.api.modules.trip.dto.CreateViagemDTO;
import com.viajajunto.api.modules.trip.dto.ViagemResponseDTO;
import com.viajajunto.api.modules.trip.entity.PermissaoMembro;
import com.viajajunto.api.modules.trip.entity.StatusViagem;
import com.viajajunto.api.modules.trip.entity.ViagemEntity;
import com.viajajunto.api.modules.trip.repository.MembroViagemRepository;
import com.viajajunto.api.modules.trip.repository.ViagemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ViagemServiceTest {

    @Mock
    private ViagemRepository viagemRepository;

    @Mock
    private MembroViagemRepository membroViagemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrcamentoRepository orcamentoRepository;

    @Mock
    private TripSecurityService tripSecurityService;

    @InjectMocks
    private ViagemService viagemService;

    private UserEntity sampleUser;
    private ViagemEntity sampleViagem;

    @BeforeEach
    void setUp() {
        sampleUser = UserEntity.builder()
                .id(1L)
                .nome("Gabriel Domingues")
                .email("gabriel@example.com")
                .build();

        sampleViagem = ViagemEntity.builder()
                .id(10L)
                .nome("Férias em Roma")
                .descricao("Viagem cultural")
                .dataInicio(LocalDate.of(2026, 10, 1))
                .dataFim(LocalDate.of(2026, 10, 15))
                .status(StatusViagem.EM_PLANEJAMENTO)
                .codigoConvite("ROMA2026")
                .criador(sampleUser)
                .build();
    }

    @Test
    @DisplayName("Deve criar viagem com sucesso e gerar orçamento associado")
    void shouldCreateViagemSuccessfully() {
        CreateViagemDTO dto = CreateViagemDTO.builder()
                .nome("Férias em Roma")
                .descricao("Viagem cultural")
                .dataInicio(LocalDate.of(2026, 10, 1))
                .dataFim(LocalDate.of(2026, 10, 15))
                .orcamentoTotal(BigDecimal.valueOf(5000))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(viagemRepository.findByCodigoConvite(anyString())).thenReturn(Optional.empty());
        when(viagemRepository.save(any(ViagemEntity.class))).thenReturn(sampleViagem);
        when(orcamentoRepository.save(any(OrcamentoEntity.class))).thenReturn(new OrcamentoEntity());
        when(membroViagemRepository.findAllByViagemId(10L)).thenReturn(Collections.emptyList());

        ViagemResponseDTO response = viagemService.createViagem(dto, 1L);

        assertNotNull(response);
        assertEquals("Férias em Roma", response.getNome());
        assertEquals(PermissaoMembro.CRIADOR, response.getPermissaoUsuarioAutenticado());
        assertEquals(BigDecimal.valueOf(5000), response.getOrcamentoTotal());
        verify(viagemRepository, times(1)).save(any(ViagemEntity.class));
        verify(orcamentoRepository, times(1)).save(any(OrcamentoEntity.class));
    }

    @Test
    @DisplayName("Deve lançar BusinessRuleException quando data final for anterior à data inicial")
    void shouldThrowExceptionWhenEndDateIsBeforeStartDate() {
        CreateViagemDTO dto = CreateViagemDTO.builder()
                .nome("Férias Inválidas")
                .dataInicio(LocalDate.of(2026, 10, 15))
                .dataFim(LocalDate.of(2026, 10, 1))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        assertThrows(BusinessRuleException.class, () -> viagemService.createViagem(dto, 1L));
        verify(viagemRepository, never()).save(any(ViagemEntity.class));
    }
}
