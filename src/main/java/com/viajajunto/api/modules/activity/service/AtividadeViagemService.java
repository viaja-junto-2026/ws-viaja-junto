package com.viajajunto.api.modules.activity.service;

import com.viajajunto.api.core.exception.BusinessRuleException;
import com.viajajunto.api.core.exception.ResourceNotFoundException;
import com.viajajunto.api.core.security.TripSecurityService;
import com.viajajunto.api.modules.activity.dto.AtividadeResponseDTO;
import com.viajajunto.api.modules.activity.dto.CreateAtividadeDTO;
import com.viajajunto.api.modules.activity.entity.AtividadeViagemEntity;
import com.viajajunto.api.modules.activity.entity.CatalogoAtividadeEntity;
import com.viajajunto.api.modules.activity.entity.StatusAtividade;
import com.viajajunto.api.modules.activity.repository.AtividadeViagemRepository;
import com.viajajunto.api.modules.activity.repository.CatalogoAtividadeRepository;
import com.viajajunto.api.modules.destination.entity.DestinoViagemEntity;
import com.viajajunto.api.modules.destination.repository.DestinoViagemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AtividadeViagemService {

    private final AtividadeViagemRepository atividadeViagemRepository;
    private final DestinoViagemRepository destinoViagemRepository;
    private final CatalogoAtividadeRepository catalogoAtividadeRepository;
    private final TripSecurityService tripSecurityService;

    @Transactional
    public AtividadeResponseDTO addAtividade(Long viagemId, Long destinoId, CreateAtividadeDTO dto, Long userId) {
        tripSecurityService.validateUserCanEditTrip(viagemId, userId);

        DestinoViagemEntity destino = destinoViagemRepository.findById(destinoId)
                .orElseThrow(() -> new ResourceNotFoundException("Destino não encontrado na viagem com ID: " + destinoId));

        if (!destino.getViagem().getId().equals(viagemId)) {
            throw new BusinessRuleException("O destino não pertence à viagem indicada.");
        }

        CatalogoAtividadeEntity catalogo = null;
        if (dto.getCatalogoAtividadeId() != null) {
            catalogo = catalogoAtividadeRepository.findById(dto.getCatalogoAtividadeId()).orElse(null);
        }

        AtividadeViagemEntity atividade = AtividadeViagemEntity.builder()
                .destinoViagem(destino)
                .catalogoAtividade(catalogo)
                .nome(dto.getNome())
                .tipo(dto.getTipo() != null ? dto.getTipo() : (catalogo != null ? catalogo.getTipo() : null))
                .local(dto.getLocal() != null ? dto.getLocal() : (catalogo != null ? catalogo.getLocalizacao() : null))
                .fotoUrl(dto.getFotoUrl() != null ? dto.getFotoUrl() : (catalogo != null ? catalogo.getFotoUrl() : null))
                .descricao(dto.getDescricao() != null ? dto.getDescricao() : (catalogo != null ? catalogo.getDescricao() : null))
                .dataHorario(dto.getDataHorario())
                .duracaoMinutos(dto.getDuracaoMinutos())
                .custoPrevisto(dto.getCustoPrevisto() != null ? dto.getCustoPrevisto() : BigDecimal.ZERO)
                .status(dto.getStatus() != null ? dto.getStatus() : StatusAtividade.PENDENTE)
                .build();

        AtividadeViagemEntity saved = atividadeViagemRepository.save(atividade);
        return mapToDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<AtividadeResponseDTO> listAtividadesByDestino(Long viagemId, Long destinoId, Long userId) {
        tripSecurityService.validateUserCanViewTrip(viagemId, userId);

        DestinoViagemEntity destino = destinoViagemRepository.findById(destinoId)
                .orElseThrow(() -> new ResourceNotFoundException("Destino não encontrado na viagem com ID: " + destinoId));

        if (!destino.getViagem().getId().equals(viagemId)) {
            throw new BusinessRuleException("O destino não pertence à viagem indicada.");
        }

        return atividadeViagemRepository.findAllByDestinoViagemIdOrderByDataHorarioAsc(destinoId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void removeAtividade(Long viagemId, Long destinoId, Long atividadeId, Long userId) {
        tripSecurityService.validateUserCanEditTrip(viagemId, userId);

        AtividadeViagemEntity atividade = atividadeViagemRepository.findById(atividadeId)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade não encontrada com ID: " + atividadeId));

        if (!atividade.getDestinoViagem().getId().equals(destinoId) ||
            !atividade.getDestinoViagem().getViagem().getId().equals(viagemId)) {
            throw new BusinessRuleException("A atividade não pertence ao destino/viagem especificados.");
        }

        atividadeViagemRepository.delete(atividade);
    }

    private AtividadeResponseDTO mapToDTO(AtividadeViagemEntity entity) {
        return AtividadeResponseDTO.builder()
                .id(entity.getId())
                .destinoViagemId(entity.getDestinoViagem().getId())
                .catalogoAtividadeId(entity.getCatalogoAtividade() != null ? entity.getCatalogoAtividade().getId() : null)
                .nome(entity.getNome())
                .tipo(entity.getTipo())
                .local(entity.getLocal())
                .fotoUrl(entity.getFotoUrl())
                .descricao(entity.getDescricao())
                .dataHorario(entity.getDataHorario())
                .duracaoMinutos(entity.getDuracaoMinutos())
                .custoPrevisto(entity.getCustoPrevisto())
                .status(entity.getStatus())
                .build();
    }
}
