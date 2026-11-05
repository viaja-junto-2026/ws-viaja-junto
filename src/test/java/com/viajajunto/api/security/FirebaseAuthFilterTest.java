package com.viajajunto.api.security;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.viajajunto.api.support.FirebaseTestTokens;
import io.restassured.RestAssured;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FirebaseAuthFilterTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    private FirebaseTestTokens.Token novoUsuario() {
        String id = UUID.randomUUID().toString();
        return FirebaseTestTokens.token().uid("filtro-" + id).email("filtro-" + id + "@viajajunto.com");
    }

    private void assertRejected(String token) {
        given()
                .header("Authorization", "Bearer " + token)
        .when()
                .get("/users/me")
        .then()
                .statusCode(401)
                .body("code", equalTo("UNAUTHORIZED"))
                .body("message", notNullValue());
    }

    @Test
    @DisplayName("Deve rejeitar chamadas em rotas protegidas sem o cabeçalho Bearer Token com HTTP 401")
    void shouldRejectProtectedRoutesWithoutBearerToken() {
        given()
        .when()
                .get("/users/me")
        .then()
                .statusCode(401)
                .body("code", equalTo("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Aceita um token Firebase válido")
    void shouldAcceptValidFirebaseToken() {
        given()
                .header("Authorization", "Bearer " + novoUsuario().build())
        .when()
                .get("/users/me")
        .then()
                .statusCode(200)
                .body("id", notNullValue());
    }

    @Test
    @DisplayName("Rejeita token assinado por outra chave")
    void shouldRejectTokenSignedWithAnotherKey() {
        assertRejected(novoUsuario().signedWithOtherKey().build());
    }

    @Test
    @DisplayName("Rejeita token expirado")
    void shouldRejectExpiredToken() {
        assertRejected(novoUsuario().expiresAt(Instant.now().minusSeconds(300)).build());
    }

    @Test
    @DisplayName("Rejeita token emitido para outro projeto Firebase (audiência diferente)")
    void shouldRejectTokenFromAnotherProject() {
        assertRejected(novoUsuario().audience("outro-projeto").build());
    }

    @Test
    @DisplayName("Rejeita token de outro emissor")
    void shouldRejectTokenFromAnotherIssuer() {
        assertRejected(novoUsuario().issuer("https://securetoken.google.com/outro-projeto").build());
    }

    @Test
    @DisplayName("Rejeita token cujo e-mail não foi verificado")
    void shouldRejectUnverifiedEmail() {
        assertRejected(novoUsuario().emailVerified(false).build());
    }

    @Test
    @DisplayName("Rejeita token sem e-mail")
    void shouldRejectTokenWithoutEmail() {
        assertRejected(novoUsuario().withoutEmail().build());
    }

    @Test
    @DisplayName("Rejeita um valor qualquer que não é token")
    void shouldRejectGarbageToken() {
        assertRejected("isto-nao-e-um-jwt");
    }
}
