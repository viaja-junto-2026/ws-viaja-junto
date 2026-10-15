package com.viajajunto.api.modules.destination.repository;

import com.viajajunto.api.modules.destination.entity.DestinoCatalogoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DestinoCatalogoRepository extends JpaRepository<DestinoCatalogoEntity, Long> {

    @Query("SELECT d FROM DestinoCatalogoEntity d WHERE " +
           "(:termo IS NULL OR LOWER(d.nome) LIKE LOWER(CONCAT('%', :termo, '%')) " +
           "OR LOWER(d.pais) LIKE LOWER(CONCAT('%', :termo, '%')) " +
           "OR LOWER(d.cidade) LIKE LOWER(CONCAT('%', :termo, '%'))) AND " +
           "(:categoria IS NULL OR LOWER(d.categoria) = LOWER(:categoria)) AND " +
           "(:notaMinima IS NULL OR d.avaliacaoMedia >= :notaMinima)")
    Page<DestinoCatalogoEntity> searchDestinos(
            @Param("termo") String termo,
            @Param("categoria") String categoria,
            @Param("notaMinima") Double notaMinima,
            Pageable pageable
    );

    List<DestinoCatalogoEntity> findTop6ByOrderByAvaliacaoMediaDesc();
}
