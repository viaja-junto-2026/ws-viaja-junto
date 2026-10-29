package com.viajajunto.api.modules.trip.controller;

import com.viajajunto.api.core.dto.PagedResponse;
import com.viajajunto.api.core.security.UserPrincipal;
import com.viajajunto.api.modules.trip.dto.AddMembroDTO;
import com.viajajunto.api.modules.trip.dto.MembroResponseDTO;
import com.viajajunto.api.modules.trip.dto.UpdatePermissaoDTO;
import com.viajajunto.api.modules.trip.service.MembroViagemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class MembroViagemController {

    private final MembroViagemService membroViagemService;

    @PostMapping("/api/viagens/{viagemId}/membros/join")
    public ResponseEntity<MembroResponseDTO> joinViagem(
            @Valid @RequestBody AddMembroDTO dto,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        MembroResponseDTO response = membroViagemService.joinViagemByCode(dto, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/trips/{tripId}/members")
    public ResponseEntity<PagedResponse<MembroResponseDTO>> listMembros(
            @PathVariable Long tripId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Page<MembroResponseDTO> membros = membroViagemService.listMembros(tripId, principal.getId(), PagedResponse.toPageable(page, limit));
        return ResponseEntity.ok(PagedResponse.of(membros));
    }

    @PatchMapping("/api/viagens/{viagemId}/membros/{membroId}")
    public ResponseEntity<MembroResponseDTO> updatePermissao(
            @PathVariable Long viagemId,
            @PathVariable Long membroId,
            @Valid @RequestBody UpdatePermissaoDTO dto,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        MembroResponseDTO response = membroViagemService.updatePermissao(viagemId, membroId, dto, principal.getId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/api/viagens/{viagemId}/membros/{membroId}")
    public ResponseEntity<Void> removeMembro(
            @PathVariable Long viagemId,
            @PathVariable Long membroId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        membroViagemService.removeMembro(viagemId, membroId, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
