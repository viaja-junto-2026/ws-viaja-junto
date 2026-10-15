package com.viajajunto.api.modules.activity.repository;

import com.viajajunto.api.modules.activity.entity.AtividadeViagemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AtividadeViagemRepository extends JpaRepository<AtividadeViagemEntity, Long> {

    List<AtividadeViagemEntity> findAllByDestinoViagemIdOrderByDataHorarioAsc(Long destinoViagemId);

    @Query("SELECT a FROM AtividadeViagemEntity a WHERE a.destinoViagem.viagem.id = :viagemId")
    List<AtividadeViagemEntity> findAllByViagemId(@Param("viagemId") Long viagemId);
}
