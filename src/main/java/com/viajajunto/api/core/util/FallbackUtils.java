package com.viajajunto.api.core.util;

import java.util.function.Function;

/**
 * Utilitário para resolução de valores com fallback de entidades de catálogo.
 */
public final class FallbackUtils {

    private FallbackUtils() {
    }

    /**
     * Retorna o valor fornecido pelo DTO se não for nulo; caso contrário, extrai o valor de fallback
     * a partir do objeto de catálogo informado. Se ambos forem nulos, retorna nulo.
     *
     * @param dtoValue          valor prioritário vindo do DTO de requisição
     * @param catalogo          objeto de catálogo de referência (pode ser nulo)
     * @param catalogoExtractor função lambda/method reference que extrai a propriedade correspondente
     * @param <T>               tipo do valor resolvido
     * @param <C>               tipo da entidade de catálogo
     * @return o valor do DTO, ou o extraído do catálogo, ou nulo
     */
    public static <T, C> T resolveValue(T dtoValue, C catalogo, Function<C, T> catalogoExtractor) {
        if (dtoValue != null) {
            return dtoValue;
        }
        return catalogo != null && catalogoExtractor != null ? catalogoExtractor.apply(catalogo) : null;
    }
}

