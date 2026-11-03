package com.viajajunto.api.modules.destination.controller;

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
@RequestMapping({"/places", "/api/destinos/catalogo"})
@RequiredArgsConstructor
public class DestinoCatalogoController {

    private final DestinoCatalogoService destinoCatalogoService;

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

    @GetMapping("/{id}")
    public ResponseEntity<DestinoCatalogoDTO> getDestinoById(@PathVariable Long id) {
        DestinoCatalogoDTO dto = destinoCatalogoService.getDestinoById(id);
        return ResponseEntity.ok(dto);
    }

    @GetMapping({"/destaques", "/featured"})
    public ResponseEntity<List<DestinoCatalogoDTO>> getTopRatedDestinos() {
        List<DestinoCatalogoDTO> destaques = destinoCatalogoService.getTopRatedDestinos();
        return ResponseEntity.ok(destaques);
    }

    @GetMapping({"/paises-visitados", "/visited-countries"})
    public ResponseEntity<VisitedCountryDTO> getVisitedCountries(@AuthenticationPrincipal UserPrincipal principal) {
        VisitedCountryDTO dto = destinoCatalogoService.getUserVisitedCountries(principal.getId());
        return ResponseEntity.ok(dto);
    }
}
