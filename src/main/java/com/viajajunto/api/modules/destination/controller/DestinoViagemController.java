package com.viajajunto.api.modules.destination.controller;

import com.viajajunto.api.core.security.UserPrincipal;
import com.viajajunto.api.modules.destination.dto.CreateDestinoViagemDTO;
import com.viajajunto.api.modules.destination.dto.DestinoViagemResponseDTO;
import com.viajajunto.api.modules.destination.service.DestinoViagemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/trips/{viagemId}/destinations", "/api/viagens/{viagemId}/destinos"})
@RequiredArgsConstructor
public class DestinoViagemController {

    private final DestinoViagemService destinoViagemService;

    @PostMapping
    public ResponseEntity<DestinoViagemResponseDTO> addDestino(
            @PathVariable Long viagemId,
            @Valid @RequestBody CreateDestinoViagemDTO dto,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        DestinoViagemResponseDTO response = destinoViagemService.addDestinoToViagem(viagemId, dto, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<DestinoViagemResponseDTO>> listDestinos(
            @PathVariable Long viagemId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<DestinoViagemResponseDTO> destinos = destinoViagemService.listDestinosByViagem(viagemId, principal.getId());
        return ResponseEntity.ok(destinos);
    }

    @DeleteMapping("/{destinoId}")
    public ResponseEntity<Void> removeDestino(
            @PathVariable Long viagemId,
            @PathVariable Long destinoId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        destinoViagemService.removeDestino(viagemId, destinoId, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
