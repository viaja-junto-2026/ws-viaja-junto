package com.viajajunto.api.modules.map.service;

import com.viajajunto.api.core.exception.UnauthorizedAccessException;
import com.viajajunto.api.core.security.TripSecurityService;
import com.viajajunto.api.integrations.google.MapsClientPort;
import com.viajajunto.api.integrations.google.PlacesResponseDTO;
import com.viajajunto.api.modules.activity.entity.AtividadeViagemEntity;
import com.viajajunto.api.modules.activity.repository.AtividadeViagemRepository;
import com.viajajunto.api.modules.destination.entity.DestinoViagemEntity;
import com.viajajunto.api.modules.destination.repository.DestinoViagemRepository;
import com.viajajunto.api.modules.map.dto.MapDataDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripMapServiceTest {

    @Mock
    private TripSecurityService tripSecurityService;

    @Mock
    private DestinoViagemRepository destinoViagemRepository;

    @Mock
    private AtividadeViagemRepository atividadeViagemRepository;

    @Mock
    private MapsClientPort mapsClient;

    @InjectMocks
    private TripMapService tripMapService;

    private PlacesResponseDTO found(double lat, double lng) {
        return PlacesResponseDTO.builder()
                .status("OK")
                .results(List.of(PlacesResponseDTO.PlaceResult.builder()
                        .geometry(PlacesResponseDTO.Geometry.builder()
                                .location(PlacesResponseDTO.Location.builder().lat(lat).lng(lng).build())
                                .build())
                        .build()))
                .build();
    }

    private PlacesResponseDTO notFound() {
        return PlacesResponseDTO.builder().status("ZERO_RESULTS").results(List.of()).build();
    }

    @Test
    @DisplayName("Deve montar marcadores de destino e atividade com as coordenadas encontradas")
    void shouldBuildMarkersForDestinationsAndActivities() {
        DestinoViagemEntity destino = DestinoViagemEntity.builder()
                .id(10L).nome("Paris").pais("França").build();
        AtividadeViagemEntity atividade = AtividadeViagemEntity.builder()
                .id(20L).nome("Torre Eiffel").local("Champ de Mars").destinoViagem(destino).build();

        when(destinoViagemRepository.findAllByViagemIdOrderByOrdemVisitaAsc(1L)).thenReturn(List.of(destino));
        when(atividadeViagemRepository.findAllByViagemId(1L)).thenReturn(List.of(atividade));
        when(mapsClient.searchPlaces("Paris, França")).thenReturn(found(48.85, 2.35));
        when(mapsClient.searchPlaces("Champ de Mars, Paris")).thenReturn(found(48.86, 2.29));

        MapDataDTO result = tripMapService.getMapData(1L, 5L);

        assertEquals(2, result.getMarkers().size());
        assertEquals("destination", result.getMarkers().get(0).getType());
        assertEquals("10", result.getMarkers().get(0).getId());
        assertEquals("Paris", result.getMarkers().get(0).getLabel());
        assertEquals(48.85, result.getMarkers().get(0).getLatitude());
        assertEquals("activity", result.getMarkers().get(1).getType());
        assertEquals("20", result.getMarkers().get(1).getId());
        assertEquals(2.29, result.getMarkers().get(1).getLongitude());
        verify(tripSecurityService).validateUserCanViewTrip(1L, 5L);
    }

    @Test
    @DisplayName("Itens que o serviço de mapas não encontra ficam de fora, sem erro")
    void shouldSkipItemsWithoutCoordinates() {
        DestinoViagemEntity destino = DestinoViagemEntity.builder().id(10L).nome("Lugar Inexistente").build();

        when(destinoViagemRepository.findAllByViagemIdOrderByOrdemVisitaAsc(1L)).thenReturn(List.of(destino));
        when(atividadeViagemRepository.findAllByViagemId(1L)).thenReturn(List.of());
        when(mapsClient.searchPlaces(anyString())).thenReturn(notFound());

        MapDataDTO result = tripMapService.getMapData(1L, 5L);

        assertTrue(result.getMarkers().isEmpty());
    }

    @Test
    @DisplayName("Usuário sem acesso à viagem não consulta o serviço de mapas")
    void shouldNotCallMapsWhenUserHasNoAccess() {
        when(tripSecurityService.validateUserCanViewTrip(1L, 5L))
                .thenThrow(new UnauthorizedAccessException("Sem permissão"));

        assertThrows(UnauthorizedAccessException.class, () -> tripMapService.getMapData(1L, 5L));

        verifyNoInteractions(mapsClient);
        verify(destinoViagemRepository, never()).findAllByViagemIdOrderByOrdemVisitaAsc(1L);
    }
}
