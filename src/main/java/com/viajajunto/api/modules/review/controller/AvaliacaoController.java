package com.viajajunto.api.modules.review.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
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
@Tag(name = "Avaliações")
public class AvaliacaoController {

    private final AvaliacaoService avaliacaoService;

    @Operation(summary = "Criar avaliação de um destino")
    @PostMapping("/places/{placeId}/reviews")
    public ResponseEntity<AvaliacaoResponseDTO> createAvaliacao(
            @PathVariable Long placeId,
            @Valid @RequestBody CreateAvaliacaoDTO dto,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        // O destino vem do caminho: esta rota avalia o destino, nunca uma atividade.
        dto.setDestinoCatalogoId(placeId);
        dto.setCatalogoAtividadeId(null);
        AvaliacaoResponseDTO response = avaliacaoService.createAvaliacao(dto, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Criar avaliação de uma atividade do catálogo")
    @PostMapping("/places/{placeId}/activities/{atividadeId}/reviews")
    public ResponseEntity<AvaliacaoResponseDTO> createAvaliacaoDeAtividade(
            @PathVariable Long placeId,
            @PathVariable Long atividadeId,
            @Valid @RequestBody CreateAvaliacaoDTO dto,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        // A atividade vem do caminho: esta rota avalia a atividade, nunca o destino.
        dto.setDestinoCatalogoId(null);
        dto.setCatalogoAtividadeId(atividadeId);
        AvaliacaoResponseDTO response = avaliacaoService.createAvaliacao(dto, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Listar avaliações de um destino (público, paginado)")
    @SecurityRequirements
    @GetMapping("/places/{placeId}/reviews")
    public ResponseEntity<PagedResponse<AvaliacaoResponseDTO>> listAvaliacoesByPlace(
            @PathVariable Long placeId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit
    ) {
        Page<AvaliacaoResponseDTO> avaliacoes = avaliacaoService.listAvaliacoesByDestino(placeId, PagedResponse.toPageable(page, limit));
        return ResponseEntity.ok(PagedResponse.of(avaliacoes));
    }

    @Operation(summary = "Listar avaliações de uma atividade (público)")
    @SecurityRequirements
    @GetMapping("/places/{placeId}/activities/{atividadeId}/reviews")
    public ResponseEntity<Page<AvaliacaoResponseDTO>> listAvaliacoesByAtividade(
            @PathVariable Long placeId,
            @PathVariable Long atividadeId,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<AvaliacaoResponseDTO> avaliacoes = avaliacaoService.listAvaliacoesByAtividade(atividadeId, pageable);
        return ResponseEntity.ok(avaliacoes);
    }
}
