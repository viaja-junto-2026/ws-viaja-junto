package com.viajajunto.api.modules.activity.service;

import com.viajajunto.api.core.exception.ResourceNotFoundException;
import com.viajajunto.api.modules.activity.dto.CatalogoAtividadeDTO;
import com.viajajunto.api.modules.activity.entity.CatalogoAtividadeEntity;
import com.viajajunto.api.modules.activity.repository.CatalogoAtividadeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogoAtividadeService {

    private final CatalogoAtividadeRepository catalogoAtividadeRepository;

    public static final String nome = "abc";


    @Transactional(readOnly = true)
    public Page<CatalogoAtividadeDTO> searchAtividades(String query, String tipo, Double notaMinima, Pageable pageable) {
        return catalogoAtividadeRepository.searchAtividades(query, tipo, notaMinima, pageable)
                .map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public CatalogoAtividadeDTO getAtividadeById(Long id) {
        CatalogoAtividadeEntity atividade = catalogoAtividadeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade não encontrada no catálogo com ID: " + id));
        return mapToDTO(atividade);
    }

    @Transactional(readOnly = true)
    public List<CatalogoAtividadeDTO> getTopRatedAtividades() {
        return catalogoAtividadeRepository.findTop6ByOrderByAvaliacaoMediaDesc()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public CatalogoAtividadeDTO mapToDTO(CatalogoAtividadeEntity entity) {
        return CatalogoAtividadeDTO.builder()
                .id(entity.getId())
                .nome(entity.getNome())
                .tipo(entity.getTipo())
                .localizacao(entity.getLocalizacao())
                .cidade(entity.getCidade())
                .pais(entity.getPais())
                .descricao(entity.getDescricao())
                .fotoUrl(entity.getFotoUrl())
                .precoMedio(entity.getPrecoMedio())
                .avaliacaoMedia(entity.getAvaliacaoMedia())
                .totalAvaliacoes(entity.getTotalAvaliacoes())
                .build();
    }
}
