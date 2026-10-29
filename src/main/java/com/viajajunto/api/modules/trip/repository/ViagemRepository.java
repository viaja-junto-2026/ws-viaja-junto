package com.viajajunto.api.modules.trip.repository;

import com.viajajunto.api.modules.trip.entity.ViagemEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ViagemRepository extends JpaRepository<ViagemEntity, Long> {

    Optional<ViagemEntity> findByCodigoConvite(String codigoConvite);

    @Query(value = "SELECT DISTINCT v FROM ViagemEntity v LEFT JOIN v.membros m " +
                   "WHERE v.criador.id = :userId OR m.usuario.id = :userId " +
                   "ORDER BY v.dataCriacao DESC",
           countQuery = "SELECT COUNT(DISTINCT v) FROM ViagemEntity v LEFT JOIN v.membros m " +
                        "WHERE v.criador.id = :userId OR m.usuario.id = :userId")
    Page<ViagemEntity> findAllByUserAccess(@Param("userId") Long userId, Pageable pageable);
}
