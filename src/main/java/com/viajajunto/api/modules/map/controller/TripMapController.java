package com.viajajunto.api.modules.map.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.viajajunto.api.core.security.UserPrincipal;
import com.viajajunto.api.modules.map.dto.MapDataDTO;
import com.viajajunto.api.modules.map.service.TripMapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Mapa da viagem")
public class TripMapController {

    private final TripMapService tripMapService;

    @Operation(summary = "Marcadores do mapa da viagem (resultado em cache)")
    @GetMapping("/trips/{tripId}/map")
    public ResponseEntity<MapDataDTO> getTripMap(
            @PathVariable Long tripId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(tripMapService.getMapData(tripId, principal.getId()));
    }
}
