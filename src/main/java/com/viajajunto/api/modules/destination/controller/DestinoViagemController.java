package com.viajajunto.api.modules.destination.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/trips/{viagemId}/destinations")
@RequiredArgsConstructor
@Tag(name = "Destinos da viagem")
public class DestinoViagemController {

    private final DestinoViagemService destinoViagemService;

    @Operation(summary = "Adicionar destino à viagem")
    @PostMapping
    public ResponseEntity<DestinoViagemResponseDTO> addDestino(
            @PathVariable Long viagemId,
            @Valid @RequestBody CreateDestinoViagemDTO dto,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        DestinoViagemResponseDTO response = destinoViagemService.addDestinoToViagem(viagemId, dto, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Listar destinos da viagem")
    @GetMapping
    public ResponseEntity<List<DestinoViagemResponseDTO>> listDestinos(
            @PathVariable Long viagemId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<DestinoViagemResponseDTO> destinos = destinoViagemService.listDestinosByViagem(viagemId, principal.getId());
        return ResponseEntity.ok(destinos);
    }

    @Operation(summary = "Remover destino da viagem")
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
