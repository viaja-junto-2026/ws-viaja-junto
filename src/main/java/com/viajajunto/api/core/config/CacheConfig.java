package com.viajajunto.api.core.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.viajajunto.api.integrations.google.GooglePlacesClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    // Cache em memória com validade e tamanho máximo, para não crescer sem limite.
    @Bean
    public CacheManager cacheManager(
            @Value("${app.cache.places.ttl:24h}") Duration ttl,
            @Value("${app.cache.places.maximum-size:1000}") long maximumSize
    ) {
        CaffeineCacheManager manager = new CaffeineCacheManager(GooglePlacesClient.PLACES_CACHE);
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(ttl)
                .maximumSize(maximumSize));
        return manager;
    }
}
