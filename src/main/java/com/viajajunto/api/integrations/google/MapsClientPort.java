package com.viajajunto.api.integrations.google;

/**
 * Porta para o serviço de mapas/lugares. O restante da aplicação depende desta interface e não do Google,
 * o que permite trocar o provedor (ou simulá-lo nos testes) sem alterar os serviços de negócio.
 */
public interface MapsClientPort {

    /** Busca lugares por texto (ex.: "Torre Eiffel, Paris"). Nunca lança exceção: falhas voltam com status "ERROR". */
    PlacesResponseDTO searchPlaces(String query);
}
