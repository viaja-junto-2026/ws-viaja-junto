package com.viajajunto.api.web;

import com.viajajunto.api.support.FirebaseTestTokens;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GlobalExceptionHandlerContractTest {

    // Token Firebase simulado: na primeira requisição a API cria o usuário a partir dele.
    private static final String TOKEN =
            FirebaseTestTokens.forUser("exception-contract-uid", "exception-contract@viajajunto.com");

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("Deve retornar HTTP 422 com schema Error padrão ao falhar na validação do corpo")
    void shouldReturn422WithStandardErrorSchemaOnValidationFailure() {
        String invalidPayload = """
                {
                    "nome": "   "
                }
                """;

        given()
                .header("Authorization", "Bearer " + TOKEN)
                .contentType(ContentType.JSON)
                .body(invalidPayload)
        .when()
                .patch("/users/me")
        .then()
                .statusCode(422)
                .body("code", equalTo("VALIDATION_ERROR"))
                .body("message", notNullValue())
                .body("details", isA(java.util.List.class))
                .body("details.field", hasItem("nome"))
                .body("details.issue", not(hasItem(nullValue())));
    }

    @Test
    @DisplayName("Deve retornar HTTP 404 com o código NOT_FOUND quando o recurso não for localizado")
    void shouldReturn404WithNotFoundCode() {
        given()
                .header("Authorization", "Bearer " + TOKEN)
        .when()
                .get("/trips/trp_inexistente_999")
        .then()
                .statusCode(404)
                .body("code", equalTo("NOT_FOUND"))
                .body("message", notNullValue());
    }
}
