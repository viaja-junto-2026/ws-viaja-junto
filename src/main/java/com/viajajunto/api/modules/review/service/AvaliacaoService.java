package com.viajajunto.api.modules.review.service;

import com.viajajunto.api.core.exception.BusinessRuleException;
import com.viajajunto.api.core.exception.ResourceNotFoundException;
import com.viajajunto.api.core.exception.ReviewExistsException;
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
import com.viajajunto.api.modules.review.repository.AvaliacaoRepository.ResumoNotas;
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
            List<AvaliacaoEntity> existingReviews = avaliacaoRepository.findAllByDestinoCatalogoId(dto.getDestinoCatalogoId());
            boolean alreadyReviewed = existingReviews.stream().anyMatch(a -> user.getId().equals(a.getUsuarioId()));
            if (alreadyReviewed) {
                throw new ReviewExistsException("Você já avaliou este destino.");
            }
        }

        CatalogoAtividadeEntity atividade = null;
        if (dto.getCatalogoAtividadeId() != null) {
            atividade = catalogoAtividadeRepository.findById(dto.getCatalogoAtividadeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Atividade não encontrada com ID: " + dto.getCatalogoAtividadeId()));
            List<AvaliacaoEntity> existingReviews = avaliacaoRepository.findAllByCatalogoAtividadeId(dto.getCatalogoAtividadeId());
            boolean alreadyReviewed = existingReviews.stream().anyMatch(a -> user.getId().equals(a.getUsuarioId()));
            if (alreadyReviewed) {
                throw new ReviewExistsException("Você já avaliou esta atividade.");
            }
        }

        // Sem destino, a avaliação é de uma atividade, que foi buscada exatamente por este ID (garantido pela
        // checagem no início do método). Usar o ID do pedido evita acessar "atividade", que o analisador não
        // consegue provar que é não nulo.
        String targetKey = destino != null ? "DESTINO#" + destino.getId() : "ATIVIDADE#" + dto.getCatalogoAtividadeId();
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

        // Total e média saem da mesma leitura do DynamoDB, para nunca divergirem.
        if (destino != null) {
            ResumoNotas resumo = avaliacaoRepository.resumirDestino(destino.getId());
            destino.setAvaliacaoMedia(resumo.mediaArredondada());
            destino.setTotalAvaliacoes(resumo.total());
            destinoCatalogoRepository.save(destino);
        }

        if (atividade != null) {
            ResumoNotas resumo = avaliacaoRepository.resumirAtividade(atividade.getId());
            atividade.setAvaliacaoMedia(resumo.mediaArredondada());
            atividade.setTotalAvaliacoes(resumo.total());
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

    /**
     * Pagina em memória: o DynamoDB só avança por cursor (exclusiveStartKey), sem "página N", e a
     * API expõe page/size. Cada listagem lê todas as avaliações do alvo, o que serve ao volume
     * atual mas deve virar paginação por cursor se um destino passar de milhares de avaliações.
     */
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
