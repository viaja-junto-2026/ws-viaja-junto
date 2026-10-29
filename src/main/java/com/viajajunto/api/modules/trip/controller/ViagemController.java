package com.viajajunto.api.modules.trip.controller;

import com.viajajunto.api.core.dto.PagedResponse;
import com.viajajunto.api.core.exception.ResourceNotFoundException;
import com.viajajunto.api.core.security.UserPrincipal;
import com.viajajunto.api.modules.trip.dto.CreateViagemDTO;
import com.viajajunto.api.modules.trip.dto.UpdateViagemDTO;
import com.viajajunto.api.modules.trip.dto.ViagemResponseDTO;
import com.viajajunto.api.modules.trip.service.ViagemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ViagemController {

    private final ViagemService viagemService;

    @PostMapping("/api/viagens")
    public ResponseEntity<ViagemResponseDTO> createViagem(
            @Valid @RequestBody CreateViagemDTO dto,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ViagemResponseDTO response = viagemService.createViagem(dto, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/trips")
    public ResponseEntity<PagedResponse<ViagemResponseDTO>> listUserTrips(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Page<ViagemResponseDTO> trips = viagemService.listUserTrips(principal.getId(), PagedResponse.toPageable(page, limit));
        return ResponseEntity.ok(PagedResponse.of(trips));
    }

    @GetMapping({"/trips/{id}", "/api/viagens/{id}"})
    public ResponseEntity<ViagemResponseDTO> getViagemById(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long numericId;
        try {
            numericId = Long.parseLong(id.replace("trp_", ""));
        } catch (NumberFormatException e) {
            throw new ResourceNotFoundException("Viagem não encontrada com o ID: " + id);
        }
        ViagemResponseDTO response = viagemService.getViagemById(numericId, principal.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/api/viagens/{id}")
    public ResponseEntity<ViagemResponseDTO> updateViagem(
            @PathVariable Long id,
            @Valid @RequestBody UpdateViagemDTO dto,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ViagemResponseDTO response = viagemService.updateViagem(id, dto, principal.getId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/api/viagens/{id}")
    public ResponseEntity<Void> deleteViagem(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        viagemService.deleteViagem(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
