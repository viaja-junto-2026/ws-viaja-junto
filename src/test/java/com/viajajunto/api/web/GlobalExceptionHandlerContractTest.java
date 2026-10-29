package com.viajajunto.api.web;

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

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("Deve retornar HTTP 422 com schema Error padrão ao falhar na validação do registro de usuário")
    void shouldReturn422WithStandardErrorSchemaOnValidationFailure() {
        String invalidPayload = """
                {
                    "name": "",
                    "email": "email-invalido",
                    "password": "123"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(invalidPayload)
        .when()
                .post("/auth/register")
        .then()
                .statusCode(422)
                .body("code", equalTo("VALIDATION_ERROR"))
                .body("message", notNullValue())
                .body("details", isA(java.util.List.class))
                .body("details.field", hasItems("email", "password"))
                .body("details.issue", not(hasItem(nullValue())));
    }

    @Test
    @DisplayName("Deve retornar HTTP 404 com o código NOT_FOUND quando o recurso não for localizado")
    void shouldReturn404WithNotFoundCode() {
        given()
                .header("Authorization", "Bearer token_mock")
        .when()
                .get("/trips/trp_inexistente_999")
        .then()
                .statusCode(404)
                .body("code", equalTo("NOT_FOUND"))
                .body("message", notNullValue());
    }
}
