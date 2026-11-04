package com.viajajunto.api.core.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FallbackUtilsTest {

    static class DummyCatalog {
        private final String name;

        DummyCatalog(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    @Test
    @DisplayName("Deve retornar valor do DTO quando ele não for nulo, mesmo com catálogo preenchido")
    void shouldReturnDtoValueWhenNotNull() {
        DummyCatalog catalog = new DummyCatalog("Nome Catálogo");
        String result = FallbackUtils.resolveValue("Nome DTO", catalog, DummyCatalog::getName);

        assertEquals("Nome DTO", result);
    }

    @Test
    @DisplayName("Deve retornar valor do DTO quando ele não for nulo e catálogo for nulo")
    void shouldReturnDtoValueWhenCatalogIsNull() {
        String result = FallbackUtils.resolveValue("Nome DTO", null, DummyCatalog::getName);

        assertEquals("Nome DTO", result);
    }

    @Test
    @DisplayName("Deve extrair valor do catálogo quando DTO for nulo")
    void shouldReturnCatalogValueWhenDtoValueIsNull() {
        DummyCatalog catalog = new DummyCatalog("Nome Catálogo");
        String result = FallbackUtils.resolveValue(null, catalog, DummyCatalog::getName);

        assertEquals("Nome Catálogo", result);
    }

    @Test
    @DisplayName("Deve retornar nulo quando tanto DTO quanto catálogo forem nulos")
    void shouldReturnNullWhenBothAreNull() {
        String result = FallbackUtils.resolveValue(null, null, DummyCatalog::getName);

        assertNull(result);
    }

    @Test
    @DisplayName("Deve retornar nulo quando extrator for nulo e DTO for nulo")
    void shouldReturnNullWhenExtractorIsNull() {
        DummyCatalog catalog = new DummyCatalog("Nome Catálogo");
        String result = FallbackUtils.resolveValue(null, catalog, null);

        assertNull(result);
    }
}

