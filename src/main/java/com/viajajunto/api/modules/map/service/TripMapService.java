package com.viajajunto.api.modules.map.service;

import com.viajajunto.api.core.security.TripSecurityService;
import com.viajajunto.api.integrations.google.MapsClientPort;
import com.viajajunto.api.integrations.google.PlacesResponseDTO;
import com.viajajunto.api.modules.activity.entity.AtividadeViagemEntity;
import com.viajajunto.api.modules.activity.repository.AtividadeViagemRepository;
import com.viajajunto.api.modules.destination.entity.DestinoViagemEntity;
import com.viajajunto.api.modules.destination.repository.DestinoViagemRepository;
import com.viajajunto.api.modules.map.dto.MapDataDTO;
import com.viajajunto.api.modules.map.dto.MapMarkerDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class TripMapService {

    private static final String TYPE_DESTINATION = "destination";
    private static final String TYPE_ACTIVITY = "activity";

    private final TripSecurityService tripSecurityService;
    private final DestinoViagemRepository destinoViagemRepository;
    private final AtividadeViagemRepository atividadeViagemRepository;
    private final MapsClientPort mapsClient;

    /**
     * Monta os marcadores de destinos e atividades da viagem. As coordenadas vêm da busca de lugares
     * (que usa cache); itens que o serviço de mapas não encontra ficam de fora do resultado.
     */
    @Transactional(readOnly = true)
    public MapDataDTO getMapData(Long tripId, Long userId) {
        tripSecurityService.validateUserCanViewTrip(tripId, userId);

        List<MapMarkerDTO> markers = new ArrayList<>();

        for (DestinoViagemEntity destino : destinoViagemRepository.findAllByViagemIdOrderByOrdemVisitaAsc(tripId)) {
            String query = joinNonBlank(destino.getNome(), destino.getLocalizacao(), destino.getPais());
            locate(query).ifPresent(location -> markers.add(
                    marker(TYPE_DESTINATION, destino.getId(), destino.getNome(), location)));
        }

        for (AtividadeViagemEntity atividade : atividadeViagemRepository.findAllByViagemId(tripId)) {
            String place = isBlank(atividade.getLocal()) ? atividade.getNome() : atividade.getLocal();
            String query = joinNonBlank(place, atividade.getDestinoViagem().getNome());
            locate(query).ifPresent(location -> markers.add(
                    marker(TYPE_ACTIVITY, atividade.getId(), atividade.getNome(), location)));
        }

        return MapDataDTO.builder().markers(markers).build();
    }

    private Optional<PlacesResponseDTO.Location> locate(String query) {
        if (isBlank(query)) {
            return Optional.empty();
        }
        PlacesResponseDTO response = mapsClient.searchPlaces(query);
        if (response == null || response.getResults() == null) {
            return Optional.empty();
        }
        return response.getResults().stream()
                .filter(Objects::nonNull)
                .map(PlacesResponseDTO.PlaceResult::getGeometry)
                .filter(Objects::nonNull)
                .map(PlacesResponseDTO.Geometry::getLocation)
                .filter(location -> location != null && location.getLat() != null && location.getLng() != null)
                .findFirst();
    }

    private MapMarkerDTO marker(String type, Long id, String label, PlacesResponseDTO.Location location) {
        return MapMarkerDTO.builder()
                .type(type)
                .id(String.valueOf(id))
                .label(label)
                .latitude(location.getLat())
                .longitude(location.getLng())
                .build();
    }

    private String joinNonBlank(String... parts) {
        return Stream.of(parts)
                .filter(part -> !isBlank(part))
                .map(String::trim)
                .collect(Collectors.joining(", "));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
