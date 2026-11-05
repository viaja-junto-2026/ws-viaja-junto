package com.viajajunto.api.core.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadados da documentação gerada pelo springdoc (Swagger UI), que descreve o que a API implementa hoje.
 * O contrato alvo, escrito à mão, fica em docs/viajajunto-openapi.json (ver README).
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI viajaJuntoOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ViajaJunto API")
                        .description("Implementação atual da API. O contrato alvo está em docs/viajajunto-openapi.json.")
                        .version("1.0.0"))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("ID token do Firebase Authentication")))
                // Por padrão toda rota exige o token; as públicas sobrescrevem com @SecurityRequirements vazio.
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
