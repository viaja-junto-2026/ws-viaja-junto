package com.viajajunto.api.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.util.StringUtils;

/**
 * Valida os tokens (ID tokens) emitidos pelo Firebase Authentication.
 *
 * <p>A assinatura é verificada com as chaves públicas do Firebase, baixadas só no primeiro uso (a
 * API sobe sem internet). Além da assinatura e das datas, o token precisa ser do nosso projeto:
 * emissor {@code https://securetoken.google.com/<project-id>} e audiência igual ao
 * {@code project-id}. Sem isso, qualquer token Firebase de outro projeto seria aceito.
 */
@Configuration
public class FirebaseJwtConfig {

    public static final String ISSUER_PREFIX = "https://securetoken.google.com/";

    private static final String JWK_SET_URI =
            "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com";

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${app.firebase.project-id}") String projectId,
            @Value("${app.firebase.jwk-set-uri:" + JWK_SET_URI + "}") String jwkSetUri
    ) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(tokenValidator(projectId));
        return decoder;
    }

    /** Regras que todo token precisa cumprir. Público para o decodificador dos testes usar as mesmas. */
    public static OAuth2TokenValidator<Jwt> tokenValidator(String projectId) {
        if (!StringUtils.hasText(projectId)) {
            throw new IllegalStateException("app.firebase.project-id (FIREBASE_PROJECT_ID) é obrigatório.");
        }

        OAuth2TokenValidator<Jwt> emissorEDatas = JwtValidators.createDefaultWithIssuer(ISSUER_PREFIX + projectId);

        OAuth2TokenValidator<Jwt> audiencia = jwt -> jwt.getAudience() != null && jwt.getAudience().contains(projectId)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "O token não pertence a este projeto Firebase.", null));

        OAuth2TokenValidator<Jwt> usuario = jwt -> StringUtils.hasText(jwt.getSubject())
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "O token não identifica o usuário.", null));

        return new DelegatingOAuth2TokenValidator<>(emissorEDatas, audiencia, usuario);
    }
}
