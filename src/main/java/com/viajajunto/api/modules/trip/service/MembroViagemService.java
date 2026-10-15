package com.viajajunto.api.modules.trip.service;

import com.viajajunto.api.core.exception.BusinessRuleException;
import com.viajajunto.api.core.exception.ResourceNotFoundException;
import com.viajajunto.api.core.security.TripSecurityService;
import com.viajajunto.api.modules.auth.dto.UserDTO;
import com.viajajunto.api.modules.auth.entity.UserEntity;
import com.viajajunto.api.modules.auth.repository.UserRepository;
import com.viajajunto.api.modules.trip.dto.AddMembroDTO;
import com.viajajunto.api.modules.trip.dto.MembroResponseDTO;
import com.viajajunto.api.modules.trip.dto.UpdatePermissaoDTO;
import com.viajajunto.api.modules.trip.entity.MembroViagemEntity;
import com.viajajunto.api.modules.trip.entity.PermissaoMembro;
import com.viajajunto.api.modules.trip.entity.ViagemEntity;
import com.viajajunto.api.modules.trip.repository.MembroViagemRepository;
import com.viajajunto.api.modules.trip.repository.ViagemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MembroViagemService {

    private final ViagemRepository viagemRepository;
    private final MembroViagemRepository membroViagemRepository;
    private final UserRepository userRepository;
    private final TripSecurityService tripSecurityService;

    @Transactional
    public MembroResponseDTO joinViagemByCode(AddMembroDTO dto, Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        ViagemEntity viagem = viagemRepository.findByCodigoConvite(dto.getCodigoConvite().trim().toUpperCase())
                .orElseThrow(() -> new BusinessRuleException("Código de convite inválido ou expirado."));

        if (viagem.getCriador().getId().equals(userId)) {
            throw new BusinessRuleException("Você já é o criador desta viagem.");
        }

        if (membroViagemRepository.existsByViagemIdAndUsuarioId(viagem.getId(), userId)) {
            throw new BusinessRuleException("Você já é membro desta viagem.");
        }

        PermissaoMembro permissao = dto.getPermissao() != null ? dto.getPermissao() : PermissaoMembro.EDITOR;

        MembroViagemEntity membro = MembroViagemEntity.builder()
                .viagem(viagem)
                .usuario(user)
                .permissao(permissao)
                .build();

        MembroViagemEntity saved = membroViagemRepository.save(membro);
        return mapToDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<MembroResponseDTO> listMembros(Long viagemId, Long userId) {
        tripSecurityService.validateUserCanViewTrip(viagemId, userId);
        return membroViagemRepository.findAllByViagemId(viagemId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public MembroResponseDTO updatePermissao(Long viagemId, Long membroId, UpdatePermissaoDTO dto, Long userId) {
        // Apenas o criador pode alterar permissões (RN04)
        tripSecurityService.validateUserIsOwner(viagemId, userId);

        MembroViagemEntity membro = membroViagemRepository.findById(membroId)
                .orElseThrow(() -> new ResourceNotFoundException("Membro não encontrado com ID: " + membroId));

        if (!membro.getViagem().getId().equals(viagemId)) {
            throw new BusinessRuleException("O membro não pertence a esta viagem.");
        }

        membro.setPermissao(dto.getPermissao());
        MembroViagemEntity updated = membroViagemRepository.save(membro);
        return mapToDTO(updated);
    }

    @Transactional
    public void removeMembro(Long viagemId, Long membroId, Long userId) {
        // Apenas o criador pode remover membros (RN04)
        tripSecurityService.validateUserIsOwner(viagemId, userId);

        MembroViagemEntity membro = membroViagemRepository.findById(membroId)
                .orElseThrow(() -> new ResourceNotFoundException("Membro não encontrado com ID: " + membroId));

        if (!membro.getViagem().getId().equals(viagemId)) {
            throw new BusinessRuleException("O membro não pertence a esta viagem.");
        }

        membroViagemRepository.delete(membro);
    }

    private MembroResponseDTO mapToDTO(MembroViagemEntity membro) {
        return MembroResponseDTO.builder()
                .id(membro.getId())
                .usuario(UserDTO.builder()
                        .id(membro.getUsuario().getId())
                        .nome(membro.getUsuario().getNome())
                        .email(membro.getUsuario().getEmail())
                        .avatarUrl(membro.getUsuario().getAvatarUrl())
                        .build())
                .permissao(membro.getPermissao())
                .dataEntrada(membro.getDataEntrada())
                .build();
    }
}
