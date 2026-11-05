package com.viajajunto.api.modules.activity.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import com.viajajunto.api.modules.activity.dto.CatalogoAtividadeDTO;
import com.viajajunto.api.modules.activity.service.CatalogoAtividadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/activities/catalog", "/api/atividades/catalogo"})
@RequiredArgsConstructor
@Tag(name = "Catálogo de atividades")
public class CatalogoAtividadeController {

    private final CatalogoAtividadeService catalogoAtividadeService;

    @Operation(summary = "Buscar atividades do catálogo (público)")
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<Page<CatalogoAtividadeDTO>> searchAtividades(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) Double notaMinima,
            @PageableDefault(size = 12) Pageable pageable
    ) {
        Page<CatalogoAtividadeDTO> page = catalogoAtividadeService.searchAtividades(q, tipo, notaMinima, pageable);
        return ResponseEntity.ok(page);
    }

    @Operation(summary = "Detalhar atividade do catálogo (público)")
    @SecurityRequirements
    @GetMapping("/{id}")
    public ResponseEntity<CatalogoAtividadeDTO> getAtividadeById(@PathVariable Long id) {
        CatalogoAtividadeDTO dto = catalogoAtividadeService.getAtividadeById(id);
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Atividades mais bem avaliadas (público)")
    @SecurityRequirements
    @GetMapping({"/destaques", "/featured"})
    public ResponseEntity<List<CatalogoAtividadeDTO>> getTopRatedAtividades() {
        List<CatalogoAtividadeDTO> destaques = catalogoAtividadeService.getTopRatedAtividades();
        return ResponseEntity.ok(destaques);
    }
}
