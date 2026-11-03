package com.viajajunto.api.modules.destination.service;

import static com.viajajunto.api.core.util.FallbackUtils.resolveValue;

import com.viajajunto.api.core.exception.BusinessRuleException;
import com.viajajunto.api.core.exception.ResourceNotFoundException;
import com.viajajunto.api.core.security.TripSecurityService;
import com.viajajunto.api.modules.destination.dto.CreateDestinoViagemDTO;
import com.viajajunto.api.modules.destination.dto.DestinoViagemResponseDTO;
import com.viajajunto.api.modules.destination.entity.DestinoCatalogoEntity;
import com.viajajunto.api.modules.destination.entity.DestinoViagemEntity;
import com.viajajunto.api.modules.destination.repository.DestinoCatalogoRepository;
import com.viajajunto.api.modules.destination.repository.DestinoViagemRepository;
import com.viajajunto.api.modules.trip.entity.ViagemEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DestinoViagemService {

    private final DestinoViagemRepository destinoViagemRepository;
    private final DestinoCatalogoRepository destinoCatalogoRepository;
    private final TripSecurityService tripSecurityService;

    @Transactional
    public DestinoViagemResponseDTO addDestinoToViagem(Long viagemId, CreateDestinoViagemDTO dto, Long userId) {
        ViagemEntity viagem = tripSecurityService.validateUserCanEditTrip(viagemId, userId);

        DestinoCatalogoEntity catalogo = null;
        if (dto.getDestinoCatalogoId() != null) {
            catalogo = destinoCatalogoRepository.findById(dto.getDestinoCatalogoId()).orElse(null);
        }

        List<DestinoViagemEntity> existing = destinoViagemRepository.findAllByViagemIdOrderByOrdemVisitaAsc(viagemId);
        int nextOrder = dto.getOrdemVisita() != null ? dto.getOrdemVisita() : existing.size() + 1;

        DestinoViagemEntity destino = DestinoViagemEntity.builder()
                .viagem(viagem)
                .destinoCatalogo(catalogo)
                .nome(dto.getNome())
                .pais(resolveValue(dto.getPais(), catalogo, DestinoCatalogoEntity::getPais))
                .codigoPaisIso(resolveValue(dto.getCodigoPaisIso(), catalogo, DestinoCatalogoEntity::getCodigoPaisIso))
                .localizacao(resolveValue(dto.getLocalizacao(), catalogo, DestinoCatalogoEntity::getCidade))
                .fotoUrl(resolveValue(dto.getFotoUrl(), catalogo, DestinoCatalogoEntity::getFotoUrl))
                .descricao(resolveValue(dto.getDescricao(), catalogo, DestinoCatalogoEntity::getDescricao))
                .categoria(resolveValue(dto.getCategoria(), catalogo, DestinoCatalogoEntity::getCategoria))
                .dataChegada(dto.getDataChegada())
                .dataSaida(dto.getDataSaida())
                .ordemVisita(nextOrder)
                .build();

        DestinoViagemEntity saved = destinoViagemRepository.save(destino);
        return mapToDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<DestinoViagemResponseDTO> listDestinosByViagem(Long viagemId, Long userId) {
        tripSecurityService.validateUserCanViewTrip(viagemId, userId);
        return destinoViagemRepository.findAllByViagemIdOrderByOrdemVisitaAsc(viagemId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void removeDestino(Long viagemId, Long destinoId, Long userId) {
        tripSecurityService.validateUserCanEditTrip(viagemId, userId);

        DestinoViagemEntity destino = destinoViagemRepository.findById(destinoId)
                .orElseThrow(() -> new ResourceNotFoundException("Destino não encontrado na viagem com ID: " + destinoId));

        if (!destino.getViagem().getId().equals(viagemId)) {
            throw new BusinessRuleException("O destino não pertence a esta viagem.");
        }

        destinoViagemRepository.delete(destino);
    }

    private DestinoViagemResponseDTO mapToDTO(DestinoViagemEntity entity) {
        return DestinoViagemResponseDTO.builder()
                .id(entity.getId())
                .viagemId(entity.getViagem().getId())
                .destinoCatalogoId(entity.getDestinoCatalogo() != null ? entity.getDestinoCatalogo().getId() : null)
                .nome(entity.getNome())
                .pais(entity.getPais())
                .codigoPaisIso(entity.getCodigoPaisIso())
                .localizacao(entity.getLocalizacao())
                .fotoUrl(entity.getFotoUrl())
                .descricao(entity.getDescricao())
                .categoria(entity.getCategoria())
                .dataChegada(entity.getDataChegada())
                .dataSaida(entity.getDataSaida())
                .ordemVisita(entity.getOrdemVisita())
                .build();
    }
}
