package com.viajajunto.api.modules.destination.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.viajajunto.api.core.exception.BusinessRuleException;
import com.viajajunto.api.core.exception.ResourceNotFoundException;
import com.viajajunto.api.core.security.TripSecurityService;
import com.viajajunto.api.modules.destination.dto.CreateDestinoViagemDTO;
import com.viajajunto.api.modules.destination.dto.DestinoViagemResponseDTO;
import com.viajajunto.api.modules.destination.entity.DestinoCatalogoEntity;
import com.viajajunto.api.modules.destination.entity.DestinoViagemEntity;
import com.viajajunto.api.modules.destination.repository.DestinoCatalogoRepository;
import com.viajajunto.api.modules.destination.repository.DestinoViagemRepository;
import com.viajajunto.api.modules.trip.entity.ViagemEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class DestinoViagemServiceTest {

    @Mock
    private DestinoViagemRepository destinoViagemRepository;

    @Mock
    private DestinoCatalogoRepository destinoCatalogoRepository;

    @Mock
    private TripSecurityService tripSecurityService;

    @InjectMocks
    private DestinoViagemService destinoViagemService;

    private ViagemEntity sampleViagem;

    @BeforeEach
    void setUp() {
        sampleViagem = ViagemEntity.builder()
                .id(1L)
                .nome("Viagem Teste")
                .build();
    }

    @Test
    @DisplayName("Deve adicionar destino utilizando valores do DTO quando preenchidos")
    void addDestinoToViagem_WithExplicitDtoValues() {
        CreateDestinoViagemDTO dto = CreateDestinoViagemDTO.builder()
                .nome("Paris Custom")
                .pais("França Custom")
                .codigoPaisIso("FRC")
                .localizacao("Centro de Paris")
                .fotoUrl("https://foto.com/paris-custom.jpg")
                .descricao("Descrição personalizada")
                .categoria("urbano")
                .build();

        when(tripSecurityService.validateUserCanEditTrip(1L, 10L)).thenReturn(sampleViagem);
        when(destinoViagemRepository.findAllByViagemIdOrderByOrdemVisitaAsc(1L)).thenReturn(List.of());
        when(destinoViagemRepository.save(any(DestinoViagemEntity.class))).thenAnswer(i -> {
            DestinoViagemEntity entity = i.getArgument(0);
            entity.setId(100L);
            return entity;
        });

        DestinoViagemResponseDTO response = destinoViagemService.addDestinoToViagem(1L, dto, 10L);

        assertNotNull(response);
        assertEquals("Paris Custom", response.getNome());
        assertEquals("França Custom", response.getPais());
        assertEquals("FRC", response.getCodigoPaisIso());
        assertEquals("Centro de Paris", response.getLocalizacao());
        assertEquals("https://foto.com/paris-custom.jpg", response.getFotoUrl());
        assertEquals("Descrição personalizada", response.getDescricao());
        assertEquals("urbano", response.getCategoria());
        assertEquals(1, response.getOrdemVisita());
    }

    @Test
    @DisplayName("Deve preencher campos com fallback do catálogo quando os do DTO forem nulos")
    void addDestinoToViagem_WithCatalogFallback() {
        DestinoCatalogoEntity catalogo = DestinoCatalogoEntity.builder()
                .id(50L)
                .nome("Paris")
                .pais("França")
                .codigoPaisIso("FRA")
                .cidade("Paris")
                .fotoUrl("https://foto.com/paris.jpg")
                .descricao("Cidade Luz")
                .categoria("cultural")
                .build();

        CreateDestinoViagemDTO dto = CreateDestinoViagemDTO.builder()
                .nome("Paris Viagem")
                .destinoCatalogoId(50L)
                .build();

        when(tripSecurityService.validateUserCanEditTrip(1L, 10L)).thenReturn(sampleViagem);
        when(destinoCatalogoRepository.findById(50L)).thenReturn(Optional.of(catalogo));
        when(destinoViagemRepository.findAllByViagemIdOrderByOrdemVisitaAsc(1L)).thenReturn(List.of());
        when(destinoViagemRepository.save(any(DestinoViagemEntity.class))).thenAnswer(i -> {
            DestinoViagemEntity entity = i.getArgument(0);
            entity.setId(101L);
            return entity;
        });

        DestinoViagemResponseDTO response = destinoViagemService.addDestinoToViagem(1L, dto, 10L);

        assertNotNull(response);
        assertEquals("Paris Viagem", response.getNome());
        assertEquals("França", response.getPais());
        assertEquals("FRA", response.getCodigoPaisIso());
        assertEquals("Paris", response.getLocalizacao());
        assertEquals("https://foto.com/paris.jpg", response.getFotoUrl());
        assertEquals("Cidade Luz", response.getDescricao());
        assertEquals("cultural", response.getCategoria());
    }

    @Test
    @DisplayName("Deve listar destinos da viagem ordenados por ordem de visita")
    void listDestinosByViagem_Success() {
        DestinoViagemEntity d1 = DestinoViagemEntity.builder()
                .id(1L)
                .viagem(sampleViagem)
                .nome("Destino 1")
                .ordemVisita(1)
                .build();

        when(destinoViagemRepository.findAllByViagemIdOrderByOrdemVisitaAsc(1L)).thenReturn(List.of(d1));

        List<DestinoViagemResponseDTO> response = destinoViagemService.listDestinosByViagem(1L, 10L);

        assertEquals(1, response.size());
        assertEquals("Destino 1", response.get(0).getNome());
        verify(tripSecurityService).validateUserCanViewTrip(1L, 10L);
    }

    @Test
    @DisplayName("Deve remover destino com sucesso quando pertencer à viagem")
    void removeDestino_Success() {
        DestinoViagemEntity destino = DestinoViagemEntity.builder()
                .id(20L)
                .viagem(sampleViagem)
                .build();

        when(destinoViagemRepository.findById(20L)).thenReturn(Optional.of(destino));

        destinoViagemService.removeDestino(1L, 20L, 10L);

        verify(destinoViagemRepository).delete(destino);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando o destino não for encontrado")
    void removeDestino_ThrowsResourceNotFoundException() {
        when(destinoViagemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                destinoViagemService.removeDestino(1L, 999L, 10L)
        );

        verify(destinoViagemRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Deve lançar BusinessRuleException quando o destino pertencer a outra viagem")
    void removeDestino_ThrowsBusinessRuleException_WhenDifferentTrip() {
        ViagemEntity outraViagem = ViagemEntity.builder().id(2L).build();
        DestinoViagemEntity destino = DestinoViagemEntity.builder()
                .id(20L)
                .viagem(outraViagem)
                .build();

        when(destinoViagemRepository.findById(20L)).thenReturn(Optional.of(destino));

        assertThrows(BusinessRuleException.class, () ->
                destinoViagemService.removeDestino(1L, 20L, 10L)
        );

        verify(destinoViagemRepository, never()).delete(any());
    }
}

