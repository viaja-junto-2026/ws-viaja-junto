package com.viajajunto.api.modules.activity.repository;

import com.viajajunto.api.modules.activity.entity.CatalogoAtividadeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CatalogoAtividadeRepository extends JpaRepository<CatalogoAtividadeEntity, Long> {

    @Query("SELECT a FROM CatalogoAtividadeEntity a WHERE " +
           "(:termo IS NULL OR LOWER(a.nome) LIKE LOWER(CONCAT('%', :termo, '%')) " +
           "OR LOWER(a.cidade) LIKE LOWER(CONCAT('%', :termo, '%')) " +
           "OR LOWER(a.localizacao) LIKE LOWER(CONCAT('%', :termo, '%'))) AND " +
           "(:tipo IS NULL OR LOWER(a.tipo) = LOWER(:tipo)) AND " +
           "(:notaMinima IS NULL OR a.avaliacaoMedia >= :notaMinima)")
    Page<CatalogoAtividadeEntity> searchAtividades(
            @Param("termo") String termo,
            @Param("tipo") String tipo,
            @Param("notaMinima") Double notaMinima,
            Pageable pageable
    );

    List<CatalogoAtividadeEntity> findTop6ByOrderByAvaliacaoMediaDesc();
}
