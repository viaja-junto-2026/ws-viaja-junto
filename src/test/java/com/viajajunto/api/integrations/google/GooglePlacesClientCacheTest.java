package com.viajajunto.api.integrations.google;

import com.viajajunto.api.core.config.CacheConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Contexto mínimo: só o cache e o cliente (o RestTemplate é simulado, sem chamar o Google de verdade).
@SpringBootTest(
        classes = {CacheConfig.class, GooglePlacesClient.class},
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "app.integrations.google.places-api-key=chave-de-teste"
)
class GooglePlacesClientCacheTest {

    @MockitoBean
    private RestTemplate restTemplate;

    @Autowired
    private MapsClientPort mapsClient;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        reset(restTemplate);
        cacheManager.getCache(GooglePlacesClient.PLACES_CACHE).clear();
    }

    private PlacesResponseDTO okResponse() {
        return PlacesResponseDTO.builder().status("OK").results(List.of()).build();
    }

    @Test
    @DisplayName("A mesma busca feita duas vezes consulta o Google uma única vez")
    void shouldCallGoogleOnlyOnceForRepeatedQuery() {
        when(restTemplate.getForObject(anyString(), eq(PlacesResponseDTO.class))).thenReturn(okResponse());

        PlacesResponseDTO first = mapsClient.searchPlaces("Torre Eiffel, Paris");
        PlacesResponseDTO second = mapsClient.searchPlaces("Torre Eiffel, Paris");

        assertSame(first, second);
        verify(restTemplate, times(1)).getForObject(anyString(), eq(PlacesResponseDTO.class));
    }

    @Test
    @DisplayName("Maiúsculas e espaços nas pontas não geram uma nova consulta")
    void shouldNormalizeCacheKey() {
        when(restTemplate.getForObject(anyString(), eq(PlacesResponseDTO.class))).thenReturn(okResponse());

        mapsClient.searchPlaces("Lisboa");
        mapsClient.searchPlaces("  LISBOA ");

        verify(restTemplate, times(1)).getForObject(anyString(), eq(PlacesResponseDTO.class));
    }

    @Test
    @DisplayName("Buscas diferentes não compartilham o mesmo resultado em cache")
    void shouldNotMixDifferentQueries() {
        when(restTemplate.getForObject(anyString(), eq(PlacesResponseDTO.class))).thenReturn(okResponse());

        mapsClient.searchPlaces("Lisboa");
        mapsClient.searchPlaces("Porto");

        verify(restTemplate, times(2)).getForObject(anyString(), eq(PlacesResponseDTO.class));
    }

    @Test
    @DisplayName("Falha ao consultar o Google devolve status ERROR e não fica gravada no cache")
    void shouldNotCacheErrors() {
        when(restTemplate.getForObject(anyString(), eq(PlacesResponseDTO.class)))
                .thenThrow(new RestClientException("Google fora do ar"))
                .thenReturn(okResponse());

        PlacesResponseDTO failed = mapsClient.searchPlaces("Roma");
        PlacesResponseDTO recovered = mapsClient.searchPlaces("Roma");

        assertEquals("ERROR", failed.getStatus());
        assertNotNull(recovered);
        assertEquals("OK", recovered.getStatus());
        verify(restTemplate, times(2)).getForObject(anyString(), eq(PlacesResponseDTO.class));
    }
}
