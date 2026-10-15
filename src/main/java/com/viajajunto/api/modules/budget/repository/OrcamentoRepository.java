package com.viajajunto.api.modules.budget.repository;

import com.viajajunto.api.modules.budget.entity.OrcamentoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrcamentoRepository extends JpaRepository<OrcamentoEntity, Long> {
    Optional<OrcamentoEntity> findByViagemId(Long viagemId);
    void deleteByViagemId(Long viagemId);
}
