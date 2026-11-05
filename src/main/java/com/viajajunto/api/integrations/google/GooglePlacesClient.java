package com.viajajunto.api.integrations.google;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class GooglePlacesClient {

    private final RestTemplate restTemplate;

    // Vem de GOOGLE_PLACES_API_KEY. Vazio = sem integração: a busca devolve resposta vazia.
    @Value("${app.integrations.google.places-api-key:}")
    private String apiKey;

    @Value("${app.integrations.google.places-base-url:https://maps.googleapis.com/maps/api/place}")
    private String baseUrl;

    public PlacesResponseDTO searchPlaces(String query) {
        if (apiKey.isBlank()) {
            log.info("Google Places API Key não configurada. Retornando resposta vazia para: {}", query);
            return PlacesResponseDTO.builder()
                    .status("OK")
                    .results(Collections.emptyList())
                    .build();
        }

        try {
            String url = UriComponentsBuilder.fromUriString(baseUrl)
                    .path("/textsearch/json")
                    .queryParam("query", query)
                    .queryParam("language", "pt-BR")
                    .queryParam("key", apiKey)
                    .build()
                    .toUriString();

            return restTemplate.getForObject(url, PlacesResponseDTO.class);
        } catch (Exception ex) {
            log.error("Erro ao consultar a API do Google Places: {}", ex.getMessage());
            return PlacesResponseDTO.builder()
                    .status("ERROR")
                    .results(Collections.emptyList())
                    .build();
        }
    }
}
