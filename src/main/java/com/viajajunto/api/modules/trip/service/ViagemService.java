package com.viajajunto.api.modules.trip.service;

import com.viajajunto.api.core.exception.BusinessRuleException;
import com.viajajunto.api.core.exception.ResourceNotFoundException;
import com.viajajunto.api.core.security.TripSecurityService;
import com.viajajunto.api.modules.auth.dto.UserDTO;
import com.viajajunto.api.modules.auth.entity.UserEntity;
import com.viajajunto.api.modules.auth.repository.UserRepository;
import com.viajajunto.api.modules.budget.entity.OrcamentoEntity;
import com.viajajunto.api.modules.budget.repository.OrcamentoRepository;
import com.viajajunto.api.modules.trip.dto.*;
import com.viajajunto.api.modules.trip.entity.MembroViagemEntity;
import com.viajajunto.api.modules.trip.entity.PermissaoMembro;
import com.viajajunto.api.modules.trip.entity.StatusViagem;
import com.viajajunto.api.modules.trip.entity.ViagemEntity;
import com.viajajunto.api.modules.trip.repository.MembroViagemRepository;
import com.viajajunto.api.modules.trip.repository.ViagemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ViagemService {

    private final ViagemRepository viagemRepository;
    private final MembroViagemRepository membroViagemRepository;
    private final UserRepository userRepository;
    private final OrcamentoRepository orcamentoRepository;
    private final TripSecurityService tripSecurityService;

    private static final String INVITE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public ViagemResponseDTO createViagem(CreateViagemDTO dto, Long userId) {
        UserEntity criador = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        if (dto.getDataInicio() != null && dto.getDataFim() != null && dto.getDataFim().isBefore(dto.getDataInicio())) {
            throw new BusinessRuleException("A data final da viagem não pode ser anterior à data de início.");
        }

        String codigoConvite = generateUniqueInviteCode();

        ViagemEntity viagem = ViagemEntity.builder()
                .nome(dto.getNome())
                .descricao(dto.getDescricao())
                .dataInicio(dto.getDataInicio())
                .dataFim(dto.getDataFim())
                .status(dto.getStatus() != null ? dto.getStatus() : StatusViagem.EM_PLANEJAMENTO)
                .codigoConvite(codigoConvite)
                .criador(criador)
                .build();

        ViagemEntity savedViagem = viagemRepository.save(viagem);

        // Inicializa orçamento da viagem
        BigDecimal orcamentoInicial = dto.getOrcamentoTotal() != null ? dto.getOrcamentoTotal() : BigDecimal.ZERO;
        OrcamentoEntity orcamento = OrcamentoEntity.builder()
                .viagem(savedViagem)
                .orcamentoTotal(orcamentoInicial)
                .build();
        orcamentoRepository.save(orcamento);

        return mapToResponse(savedViagem, PermissaoMembro.CRIADOR, orcamentoInicial);
    }

    @Transactional(readOnly = true)
    public List<ViagemResponseDTO> listUserTrips(Long userId) {
        List<ViagemEntity> viagens = viagemRepository.findAllByUserAccess(userId);
        return viagens.stream()
                .map(v -> {
                    PermissaoMembro role = determineUserRole(v, userId);
                    BigDecimal orcamentoTotal = orcamentoRepository.findByViagemId(v.getId())
                            .map(OrcamentoEntity::getOrcamentoTotal)
                            .orElse(BigDecimal.ZERO);
                    return mapToResponse(v, role, orcamentoTotal);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ViagemResponseDTO getViagemById(Long viagemId, Long userId) {
        ViagemEntity viagem = tripSecurityService.validateUserCanViewTrip(viagemId, userId);
        PermissaoMembro role = determineUserRole(viagem, userId);
        BigDecimal orcamentoTotal = orcamentoRepository.findByViagemId(viagem.getId())
                .map(OrcamentoEntity::getOrcamentoTotal)
                .orElse(BigDecimal.ZERO);

        return mapToResponse(viagem, role, orcamentoTotal);
    }

    @Transactional
    public ViagemResponseDTO updateViagem(Long viagemId, UpdateViagemDTO dto, Long userId) {
        ViagemEntity viagem = tripSecurityService.validateUserCanEditTrip(viagemId, userId);

        if (dto.getDataInicio() != null && dto.getDataFim() != null && dto.getDataFim().isBefore(dto.getDataInicio())) {
            throw new BusinessRuleException("A data final da viagem não pode ser anterior à data de início.");
        }

        if (dto.getNome() != null && !dto.getNome().isBlank()) {
            viagem.setNome(dto.getNome());
        }
        if (dto.getDescricao() != null) {
            viagem.setDescricao(dto.getDescricao());
        }
        if (dto.getDataInicio() != null) {
            viagem.setDataInicio(dto.getDataInicio());
        }
        if (dto.getDataFim() != null) {
            viagem.setDataFim(dto.getDataFim());
        }
        if (dto.getStatus() != null) {
            viagem.setStatus(dto.getStatus());
        }

        ViagemEntity updated = viagemRepository.save(viagem);
        PermissaoMembro role = determineUserRole(updated, userId);
        BigDecimal orcamentoTotal = orcamentoRepository.findByViagemId(updated.getId())
                .map(OrcamentoEntity::getOrcamentoTotal)
                .orElse(BigDecimal.ZERO);

        return mapToResponse(updated, role, orcamentoTotal);
    }

    @Transactional
    public void deleteViagem(Long viagemId, Long userId) {
        ViagemEntity viagem = tripSecurityService.validateUserIsOwner(viagemId, userId);
        orcamentoRepository.deleteByViagemId(viagemId);
        viagemRepository.delete(viagem);
    }

    private PermissaoMembro determineUserRole(ViagemEntity viagem, Long userId) {
        if (viagem.getCriador().getId().equals(userId)) {
            return PermissaoMembro.CRIADOR;
        }
        return membroViagemRepository.findByViagemIdAndUsuarioId(viagem.getId(), userId)
                .map(MembroViagemEntity::getPermissao)
                .orElse(PermissaoMembro.VISUALIZADOR);
    }

    private ViagemResponseDTO mapToResponse(ViagemEntity viagem, PermissaoMembro role, BigDecimal orcamentoTotal) {
        List<MembroViagemEntity> membros = membroViagemRepository.findAllByViagemId(viagem.getId());
        List<MembroResponseDTO> membrosDTO = membros.stream()
                .map(m -> MembroResponseDTO.builder()
                        .id(m.getId())
                        .usuario(UserDTO.builder()
                                .id(m.getUsuario().getId())
                                .nome(m.getUsuario().getNome())
                                .email(m.getUsuario().getEmail())
                                .avatarUrl(m.getUsuario().getAvatarUrl())
                                .build())
                        .permissao(m.getPermissao())
                        .dataEntrada(m.getDataEntrada())
                        .build())
                .collect(Collectors.toList());

        return ViagemResponseDTO.builder()
                .id(viagem.getId())
                .nome(viagem.getNome())
                .descricao(viagem.getDescricao())
                .dataInicio(viagem.getDataInicio())
                .dataFim(viagem.getDataFim())
                .status(viagem.getStatus())
                .codigoConvite(viagem.getCodigoConvite())
                .criador(UserDTO.builder()
                        .id(viagem.getCriador().getId())
                        .nome(viagem.getCriador().getNome())
                        .email(viagem.getCriador().getEmail())
                        .avatarUrl(viagem.getCriador().getAvatarUrl())
                        .build())
                .permissaoUsuarioAutenticado(role)
                .membros(membrosDTO)
                .orcamentoTotal(orcamentoTotal)
                .dataCriacao(viagem.getDataCriacao())
                .dataAtualizacao(viagem.getDataAtualizacao())
                .build();
    }

    private String generateUniqueInviteCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(8);
            for (int i = 0; i < 8; i++) {
                sb.append(INVITE_CHARS.charAt(RANDOM.nextInt(INVITE_CHARS.length())));
            }
            code = sb.toString();
        } while (viagemRepository.findByCodigoConvite(code).isPresent());
        return code;
    }
}
