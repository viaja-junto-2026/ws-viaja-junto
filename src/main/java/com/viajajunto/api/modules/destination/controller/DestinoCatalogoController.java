package com.viajajunto.api.modules.destination.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import com.viajajunto.api.core.security.UserPrincipal;
import com.viajajunto.api.modules.destination.dto.DestinoCatalogoDTO;
import com.viajajunto.api.modules.destination.dto.VisitedCountryDTO;
import com.viajajunto.api.modules.destination.service.DestinoCatalogoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/places")
@RequiredArgsConstructor
@Tag(name = "Destinos do catálogo")
public class DestinoCatalogoController {

    private final DestinoCatalogoService destinoCatalogoService;

    @Operation(summary = "Buscar destinos da comunidade (público)")
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<Page<DestinoCatalogoDTO>> searchDestinos(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) Double notaMinima,
            @PageableDefault(size = 12) Pageable pageable
    ) {
        Page<DestinoCatalogoDTO> page = destinoCatalogoService.searchDestinos(q, categoria, notaMinima, pageable);
        return ResponseEntity.ok(page);
    }

    @Operation(summary = "Detalhar destino do catálogo (público)")
    @SecurityRequirements
    @GetMapping("/{id}")
    public ResponseEntity<DestinoCatalogoDTO> getDestinoById(@PathVariable Long id) {
        DestinoCatalogoDTO dto = destinoCatalogoService.getDestinoById(id);
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Destinos mais bem avaliados (público)")
    @SecurityRequirements
    @GetMapping({"/destaques", "/featured"})
    public ResponseEntity<List<DestinoCatalogoDTO>> getTopRatedDestinos() {
        List<DestinoCatalogoDTO> destaques = destinoCatalogoService.getTopRatedDestinos();
        return ResponseEntity.ok(destaques);
    }

    @Operation(summary = "Países visitados pelo usuário")
    @GetMapping({"/paises-visitados", "/visited-countries"})
    public ResponseEntity<VisitedCountryDTO> getVisitedCountries(@AuthenticationPrincipal UserPrincipal principal) {
        VisitedCountryDTO dto = destinoCatalogoService.getUserVisitedCountries(principal.getId());
        return ResponseEntity.ok(dto);
    }
}
