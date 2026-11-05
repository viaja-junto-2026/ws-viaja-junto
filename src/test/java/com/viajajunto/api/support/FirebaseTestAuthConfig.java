package com.viajajunto.api.support;

import com.viajajunto.api.core.config.FirebaseJwtConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Nos testes, o decodificador confia na chave de {@link FirebaseTestTokens} em vez de buscar as chaves
 * do Firebase na internet, mas aplica as mesmas regras de validação da produção (emissor, audiência,
 * validade e usuário). Por estar em src/test e no pacote da aplicação, é carregada em todos os
 * {@code @SpringBootTest} e não existe fora dos testes.
 */
@Configuration
public class FirebaseTestAuthConfig {

    @Bean
    @Primary
    public JwtDecoder firebaseTestJwtDecoder(@Value("${app.firebase.project-id}") String projectId) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(FirebaseTestTokens.publicKey()).build();
        decoder.setJwtValidator(FirebaseJwtConfig.tokenValidator(projectId));
        return decoder;
    }
}
