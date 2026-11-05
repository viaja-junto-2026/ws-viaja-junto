package com.viajajunto.api.modules.map.controller;

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
public class TripMapController {

    private final TripMapService tripMapService;

    @GetMapping("/trips/{tripId}/map")
    public ResponseEntity<MapDataDTO> getTripMap(
            @PathVariable Long tripId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(tripMapService.getMapData(tripId, principal.getId()));
    }
}
