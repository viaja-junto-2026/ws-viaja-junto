package com.viajajunto.api.modules.budget.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.viajajunto.api.core.security.UserPrincipal;
import com.viajajunto.api.modules.budget.dto.OrcamentoResponseDTO;
import com.viajajunto.api.modules.budget.dto.UpdateOrcamentoDTO;
import com.viajajunto.api.modules.budget.service.OrcamentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/trips/{viagemId}/budget", "/api/viagens/{viagemId}/orcamento"})
@RequiredArgsConstructor
@Tag(name = "Orçamento")
public class OrcamentoController {

    private final OrcamentoService orcamentoService;

    @Operation(summary = "Resumo do orçamento da viagem")
    @GetMapping
    public ResponseEntity<OrcamentoResponseDTO> getResumoOrcamento(
            @PathVariable Long viagemId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        OrcamentoResponseDTO resumo = orcamentoService.getResumoOrcamento(viagemId, principal.getId());
        return ResponseEntity.ok(resumo);
    }

    @Operation(summary = "Atualizar o orçamento total da viagem")
    @PutMapping
    public ResponseEntity<OrcamentoResponseDTO> updateOrcamento(
            @PathVariable Long viagemId,
            @Valid @RequestBody UpdateOrcamentoDTO dto,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        OrcamentoResponseDTO resumo = orcamentoService.updateOrcamentoTotal(viagemId, dto, principal.getId());
        return ResponseEntity.ok(resumo);
    }
}
