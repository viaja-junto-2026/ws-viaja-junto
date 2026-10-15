package com.viajajunto.api.modules.trip.repository;

import com.viajajunto.api.modules.trip.entity.MembroViagemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MembroViagemRepository extends JpaRepository<MembroViagemEntity, Long> {

    Optional<MembroViagemEntity> findByViagemIdAndUsuarioId(Long viagemId, Long usuarioId);

    List<MembroViagemEntity> findAllByViagemId(Long viagemId);

    boolean existsByViagemIdAndUsuarioId(Long viagemId, Long usuarioId);

    void deleteByViagemIdAndUsuarioId(Long viagemId, Long usuarioId);
}
