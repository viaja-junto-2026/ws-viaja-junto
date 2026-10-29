package com.viajajunto.api.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {

    private static final int MAX_LIMIT = 100;

    private List<T> data;
    private PageMeta meta;

    /** Converte o Page do Spring (0-based) para o envelope do contrato (page 1-based). */
    public static <T> PagedResponse<T> of(Page<T> page) {
        PageMeta meta = PageMeta.builder()
                .page(page.getNumber() + 1)
                .limit(page.getSize())
                .totalItems(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
        return new PagedResponse<>(page.getContent(), meta);
    }

    /** Cria o Pageable a partir dos query params do contrato (page 1-based e limit). */
    public static Pageable toPageable(int page, int limit) {
        int safePage = Math.max(page, 1);
        int safeLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
        return PageRequest.of(safePage - 1, safeLimit);
    }
}
