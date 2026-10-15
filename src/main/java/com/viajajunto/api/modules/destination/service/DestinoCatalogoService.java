package com.viajajunto.api.modules.destination.service;

import com.viajajunto.api.core.exception.ResourceNotFoundException;
import com.viajajunto.api.modules.destination.dto.DestinoCatalogoDTO;
import com.viajajunto.api.modules.destination.dto.VisitedCountryDTO;
import com.viajajunto.api.modules.destination.entity.DestinoCatalogoEntity;
import com.viajajunto.api.modules.destination.repository.DestinoCatalogoRepository;
import com.viajajunto.api.modules.destination.repository.DestinoViagemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DestinoCatalogoService {

    private final DestinoCatalogoRepository destinoCatalogoRepository;
    private final DestinoViagemRepository destinoViagemRepository;

    @Transactional(readOnly = true)
    public Page<DestinoCatalogoDTO> searchDestinos(String query, String categoria, Double notaMinima, Pageable pageable) {
        return destinoCatalogoRepository.searchDestinos(query, categoria, notaMinima, pageable)
                .map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public DestinoCatalogoDTO getDestinoById(Long id) {
        DestinoCatalogoEntity destino = destinoCatalogoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Destino não encontrado no catálogo com ID: " + id));
        return mapToDTO(destino);
    }

    @Transactional(readOnly = true)
    public List<DestinoCatalogoDTO> getTopRatedDestinos() {
        return destinoCatalogoRepository.findTop6ByOrderByAvaliacaoMediaDesc()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VisitedCountryDTO getUserVisitedCountries(Long userId) {
        List<String> visitedCodes = destinoViagemRepository.findDistinctVisitedCountryCodes(userId);
        return VisitedCountryDTO.builder()
                .visitedIsoCodes(visitedCodes)
                .totalVisited(visitedCodes.size())
                .build();
    }

    public DestinoCatalogoDTO mapToDTO(DestinoCatalogoEntity entity) {
        return DestinoCatalogoDTO.builder()
                .id(entity.getId())
                .nome(entity.getNome())
                .pais(entity.getPais())
                .codigoPaisIso(entity.getCodigoPaisIso())
                .cidade(entity.getCidade())
                .estado(entity.getEstado())
                .descricao(entity.getDescricao())
                .categoria(entity.getCategoria())
                .fotoUrl(entity.getFotoUrl())
                .avaliacaoMedia(entity.getAvaliacaoMedia())
                .totalAvaliacoes(entity.getTotalAvaliacoes())
                .build();
    }
}
