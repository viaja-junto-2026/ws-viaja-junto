package com.viajajunto.api.core.security;

import com.viajajunto.api.core.exception.ResourceNotFoundException;
import com.viajajunto.api.core.exception.UnauthorizedAccessException;
import com.viajajunto.api.modules.trip.entity.PermissaoMembro;
import com.viajajunto.api.modules.trip.entity.ViagemEntity;
import com.viajajunto.api.modules.trip.repository.MembroViagemRepository;
import com.viajajunto.api.modules.trip.repository.ViagemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service("tripSecurity")
@RequiredArgsConstructor
public class TripSecurityService {

    private final ViagemRepository viagemRepository;
    private final MembroViagemRepository membroViagemRepository;

    public ViagemEntity validateUserCanViewTrip(Long viagemId, Long userId) {
        ViagemEntity viagem = viagemRepository.findById(viagemId)
                .orElseThrow(() -> new ResourceNotFoundException("Viagem não encontrada com o ID: " + viagemId));

        if (viagem.getCriador().getId().equals(userId)) {
            return viagem;
        }

        boolean isMember = membroViagemRepository.existsByViagemIdAndUsuarioId(viagemId, userId);
        if (!isMember) {
            throw new UnauthorizedAccessException("Você não tem permissão para visualizar esta viagem.");
        }

        return viagem;
    }

    public ViagemEntity validateUserCanEditTrip(Long viagemId, Long userId) {
        ViagemEntity viagem = viagemRepository.findById(viagemId)
                .orElseThrow(() -> new ResourceNotFoundException("Viagem não encontrada com o ID: " + viagemId));

        if (viagem.getCriador().getId().equals(userId)) {
            return viagem;
        }

        return membroViagemRepository.findByViagemIdAndUsuarioId(viagemId, userId)
                .filter(m -> m.getPermissao() == PermissaoMembro.EDITOR || m.getPermissao() == PermissaoMembro.CRIADOR)
                .map(m -> viagem)
                .orElseThrow(() -> new UnauthorizedAccessException("Você precisa de permissão de Editor ou Criador para alterar esta viagem."));
    }

    public ViagemEntity validateUserIsOwner(Long viagemId, Long userId) {
        ViagemEntity viagem = viagemRepository.findById(viagemId)
                .orElseThrow(() -> new ResourceNotFoundException("Viagem não encontrada com o ID: " + viagemId));

        if (!viagem.getCriador().getId().equals(userId)) {
            throw new UnauthorizedAccessException("Apenas o criador da viagem tem permissão para realizar esta ação.");
        }

        return viagem;
    }
}
