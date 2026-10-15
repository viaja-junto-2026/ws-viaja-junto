package com.viajajunto.api.modules.destination.repository;

import com.viajajunto.api.modules.destination.entity.DestinoViagemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DestinoViagemRepository extends JpaRepository<DestinoViagemEntity, Long> {

    List<DestinoViagemEntity> findAllByViagemIdOrderByOrdemVisitaAsc(Long viagemId);

    @Query("SELECT DISTINCT d.codigoPaisIso FROM DestinoViagemEntity d " +
           "WHERE (d.viagem.criador.id = :userId OR EXISTS (" +
           "   SELECT m FROM MembroViagemEntity m WHERE m.viagem = d.viagem AND m.usuario.id = :userId" +
           ")) AND d.codigoPaisIso IS NOT NULL")
    List<String> findDistinctVisitedCountryCodes(@Param("userId") Long userId);
}
