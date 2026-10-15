package com.viajajunto.api.modules.review.service;

import com.viajajunto.api.core.exception.BusinessRuleException;
import com.viajajunto.api.core.exception.ResourceNotFoundException;
import com.viajajunto.api.modules.activity.entity.CatalogoAtividadeEntity;
import com.viajajunto.api.modules.activity.repository.CatalogoAtividadeRepository;
import com.viajajunto.api.modules.auth.dto.UserDTO;
import com.viajajunto.api.modules.auth.entity.UserEntity;
import com.viajajunto.api.modules.auth.repository.UserRepository;
import com.viajajunto.api.modules.destination.entity.DestinoCatalogoEntity;
import com.viajajunto.api.modules.destination.repository.DestinoCatalogoRepository;
import com.viajajunto.api.modules.review.dto.AvaliacaoResponseDTO;
import com.viajajunto.api.modules.review.dto.CreateAvaliacaoDTO;
import com.viajajunto.api.modules.review.entity.AvaliacaoEntity;
import com.viajajunto.api.modules.review.repository.AvaliacaoRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AvaliacaoService {

    private final AvaliacaoRepository avaliacaoRepository;
    private final UserRepository userRepository;
    private final DestinoCatalogoRepository destinoCatalogoRepository;
    private final CatalogoAtividadeRepository catalogoAtividadeRepository;

    @Transactional
    public AvaliacaoResponseDTO createAvaliacao(CreateAvaliacaoDTO dto, Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        if (dto.getDestinoCatalogoId() == null && dto.getCatalogoAtividadeId() == null) {
            throw new BusinessRuleException("A avaliação deve estar associada a um Destino ou a uma Atividade.");
        }

        DestinoCatalogoEntity destino = null;

        if (dto.getDestinoCatalogoId() != null) {
            destino = destinoCatalogoRepository.findById(dto.getDestinoCatalogoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Destino não encontrado com ID: " + dto.getDestinoCatalogoId()));
        }

        CatalogoAtividadeEntity atividade = null;
        if (dto.getCatalogoAtividadeId() != null) {
            atividade = catalogoAtividadeRepository.findById(dto.getCatalogoAtividadeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Atividade não encontrada com ID: " + dto.getCatalogoAtividadeId()));
        }

        String targetKey = destino != null ? "DESTINO#" + destino.getId() : "ATIVIDADE#" + atividade.getId();
        String avaliacaoId = UUID.randomUUID().toString();
        String nowIso = Instant.now().toString();

        AvaliacaoEntity avaliacao = AvaliacaoEntity.builder()
                .targetKey(targetKey)
                .id(avaliacaoId)
                .usuarioId(user.getId())
                .usuarioNome(user.getNome())
                .usuarioEmail(user.getEmail())
                .usuarioAvatarUrl(user.getAvatarUrl())
                .destinoCatalogoId(destino != null ? destino.getId() : null)
                .catalogoAtividadeId(atividade != null ? atividade.getId() : null)
                .nota(dto.getNota())
                .comentario(dto.getComentario())
                .dataCriacao(Instant.now().toString())
                .build();

        AvaliacaoEntity saved = avaliacaoRepository.save(avaliacao);

        if (destino != null) {
            Double media = avaliacaoRepository.calculateAverageRatingForDestino(destino.getId());
            destino.setAvaliacaoMedia(media != null ? Math.round(media * 10.0) / 10.0 : 0.0);
            destino.setTotalAvaliacoes(destino.getTotalAvaliacoes() + 1);
            destinoCatalogoRepository.save(destino);
        }

        if (atividade != null) {
            Double media = avaliacaoRepository.calculateAverageRatingForAtividade(atividade.getId());
            atividade.setAvaliacaoMedia(media != null ? Math.round(media * 10.0) / 10.0 : 0.0);
            atividade.setTotalAvaliacoes(atividade.getTotalAvaliacoes() + 1);
            catalogoAtividadeRepository.save(atividade);
        }

        return mapToDTO(saved);
    }

    @Transactional(readOnly = true)
    public Page<AvaliacaoResponseDTO> listAvaliacoesByDestino(Long destinoId, Pageable pageable) {
        List<AvaliacaoEntity> list = avaliacaoRepository.findAllByDestinoCatalogoId(destinoId);
        return paginate(list, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AvaliacaoResponseDTO> listAvaliacoesByAtividade(Long atividadeId, Pageable pageable) {
        List<AvaliacaoEntity> list = avaliacaoRepository.findAllByCatalogoAtividadeId(atividadeId);
        return paginate(list, pageable);
    }

    private Page<AvaliacaoResponseDTO> paginate(List<AvaliacaoEntity> list, Pageable pageable) {
        List<AvaliacaoResponseDTO> dtos = list.stream()
                .sorted(Comparator.comparing(AvaliacaoEntity::getDataCriacao, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), dtos.size());

        if (start > dtos.size()) {
            return new PageImpl<>(List.of(), pageable, dtos.size());
        }

        List<AvaliacaoResponseDTO> subList = dtos.subList(start, end);
        return new PageImpl<>(subList, pageable, dtos.size());
    }

    private AvaliacaoResponseDTO mapToDTO(AvaliacaoEntity entity) {
        LocalDateTime localDateTime = null;
        if (entity.getDataCriacao() != null) {
            try {
                localDateTime = LocalDateTime.ofInstant(Instant.parse(entity.getDataCriacao()), ZoneId.systemDefault());
            } catch (Exception ignored) {
                localDateTime = LocalDateTime.now();
            }
        }

        return AvaliacaoResponseDTO.builder()
                .id(entity.getId())
                .usuario(UserDTO.builder()
                        .id(entity.getUsuarioId())
                        .nome(entity.getUsuarioNome())
                        .email(entity.getUsuarioEmail())
                        .avatarUrl(entity.getUsuarioAvatarUrl())
                        .build())
                .destinoCatalogoId(entity.getDestinoCatalogoId())
                .catalogoAtividadeId(entity.getCatalogoAtividadeId())
                .nota(entity.getNota())
                .comentario(entity.getComentario())
                .dataCriacao(localDateTime)
                .build();
    }
}
