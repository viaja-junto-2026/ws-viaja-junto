package com.viajajunto.api.modules.activity.controller;

import com.viajajunto.api.core.dto.PagedResponse;
import com.viajajunto.api.core.security.UserPrincipal;
import com.viajajunto.api.modules.activity.dto.AtividadeResponseDTO;
import com.viajajunto.api.modules.activity.dto.CreateAtividadeDTO;
import com.viajajunto.api.modules.activity.service.AtividadeViagemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AtividadeViagemController {

    private final AtividadeViagemService atividadeViagemService;

    @PostMapping("/api/viagens/{viagemId}/destinos/{destinoId}/atividades")
    public ResponseEntity<AtividadeResponseDTO> addAtividade(
            @PathVariable Long viagemId,
            @PathVariable Long destinoId,
            @Valid @RequestBody CreateAtividadeDTO dto,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AtividadeResponseDTO response = atividadeViagemService.addAtividade(viagemId, destinoId, dto, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/viagens/{viagemId}/destinos/{destinoId}/atividades")
    public ResponseEntity<List<AtividadeResponseDTO>> listAtividades(
            @PathVariable Long viagemId,
            @PathVariable Long destinoId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<AtividadeResponseDTO> atividades = atividadeViagemService.listAtividadesByDestino(viagemId, destinoId, principal.getId());
        return ResponseEntity.ok(atividades);
    }

    @GetMapping("/trips/{tripId}/activities")
    public ResponseEntity<PagedResponse<AtividadeResponseDTO>> listAtividadesByTrip(
            @PathVariable Long tripId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Page<AtividadeResponseDTO> atividades = atividadeViagemService.listAtividadesByViagem(tripId, principal.getId(), PagedResponse.toPageable(page, limit));
        return ResponseEntity.ok(PagedResponse.of(atividades));
    }

    @DeleteMapping("/api/viagens/{viagemId}/destinos/{destinoId}/atividades/{atividadeId}")
    public ResponseEntity<Void> removeAtividade(
            @PathVariable Long viagemId,
            @PathVariable Long destinoId,
            @PathVariable Long atividadeId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        atividadeViagemService.removeAtividade(viagemId, destinoId, atividadeId, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
