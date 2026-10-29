package com.viajajunto.api.modules.review.controller;

import com.viajajunto.api.core.dto.PagedResponse;
import com.viajajunto.api.core.security.UserPrincipal;
import com.viajajunto.api.modules.review.dto.AvaliacaoResponseDTO;
import com.viajajunto.api.modules.review.dto.CreateAvaliacaoDTO;
import com.viajajunto.api.modules.review.service.AvaliacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AvaliacaoController {

    private final AvaliacaoService avaliacaoService;

    @PostMapping("/api/avaliacoes")
    public ResponseEntity<AvaliacaoResponseDTO> createAvaliacao(
            @Valid @RequestBody CreateAvaliacaoDTO dto,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AvaliacaoResponseDTO response = avaliacaoService.createAvaliacao(dto, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/places/{placeId}/reviews")
    public ResponseEntity<PagedResponse<AvaliacaoResponseDTO>> listAvaliacoesByPlace(
            @PathVariable Long placeId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit
    ) {
        Page<AvaliacaoResponseDTO> avaliacoes = avaliacaoService.listAvaliacoesByDestino(placeId, PagedResponse.toPageable(page, limit));
        return ResponseEntity.ok(PagedResponse.of(avaliacoes));
    }

    @GetMapping("/api/avaliacoes/atividade/{atividadeId}")
    public ResponseEntity<Page<AvaliacaoResponseDTO>> listAvaliacoesByAtividade(
            @PathVariable Long atividadeId,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<AvaliacaoResponseDTO> avaliacoes = avaliacaoService.listAvaliacoesByAtividade(atividadeId, pageable);
        return ResponseEntity.ok(avaliacoes);
    }
}
