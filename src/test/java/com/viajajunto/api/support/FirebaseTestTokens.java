package com.viajajunto.api.support;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;

/**
 * Gera tokens no formato do Firebase (ID token) assinados com uma chave de teste, para os testes não
 * dependerem do Firebase nem da internet. O {@code FirebaseTestAuthConfig} registra um decodificador
 * que confia nesta chave e aplica as mesmas regras de validação da produção.
 */
public final class FirebaseTestTokens {

    public static final String PROJECT_ID = "viajajunto-test";

    /** UIDs dos usuários de exemplo do data.sql (perfil dev). */
    public static final String UID_ANA = "dev-firebase-uid-ana";

    private static final String ISSUER_PREFIX = "https://securetoken.google.com/";
    private static final RSAKey KEY = newKey("firebase-test-key");
    private static final RSAKey OTHER_KEY = newKey("outra-chave");

    private FirebaseTestTokens() {
    }

    public static RSAPublicKey publicKey() {
        try {
            return KEY.toRSAPublicKey();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Token válido de um usuário com e-mail verificado. */
    public static String forUser(String uid, String email) {
        return token().uid(uid).email(email).build();
    }

    public static Token token() {
        return new Token();
    }

    private static RSAKey newKey(String id) {
        try {
            return new RSAKeyGenerator(2048).keyID(id).generate();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Monta um token; cada método muda um detalhe para os testes de rejeição. */
    public static final class Token {
        private String uid = "test-uid";
        private String email = "teste@viajajunto.com";
        private boolean emailVerified = true;
        private String name = "Usuário de Teste";
        private String picture;
        private String audience = PROJECT_ID;
        private String issuer = ISSUER_PREFIX + PROJECT_ID;
        private Instant expiresAt = Instant.now().plusSeconds(3600);
        private boolean signWithOtherKey;
        private boolean includeEmail = true;

        public Token uid(String uid) {
            this.uid = uid;
            return this;
        }

        public Token email(String email) {
            this.email = email;
            return this;
        }

        public Token withoutEmail() {
            this.includeEmail = false;
            return this;
        }

        public Token emailVerified(boolean emailVerified) {
            this.emailVerified = emailVerified;
            return this;
        }

        public Token name(String name) {
            this.name = name;
            return this;
        }

        public Token picture(String picture) {
            this.picture = picture;
            return this;
        }

        public Token audience(String audience) {
            this.audience = audience;
            return this;
        }

        public Token issuer(String issuer) {
            this.issuer = issuer;
            return this;
        }

        public Token expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Token signedWithOtherKey() {
            this.signWithOtherKey = true;
            return this;
        }

        public String build() {
            JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                    .subject(uid)
                    .issuer(issuer)
                    .audience(audience)
                    .issueTime(Date.from(Instant.now().minusSeconds(5)))
                    .expirationTime(Date.from(expiresAt))
                    .claim("email_verified", emailVerified);
            if (includeEmail) {
                claims.claim("email", email);
            }
            if (name != null) {
                claims.claim("name", name);
            }
            if (picture != null) {
                claims.claim("picture", picture);
            }

            RSAKey signer = signWithOtherKey ? OTHER_KEY : KEY;
            try {
                SignedJWT jwt = new SignedJWT(
                        new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signer.getKeyID()).build(),
                        claims.build());
                jwt.sign(new RSASSASigner(signer));
                return jwt.serialize();
            } catch (JOSEException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
