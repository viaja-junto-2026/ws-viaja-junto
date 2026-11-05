package com.viajajunto.api.web;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;

// Sem perfil ativo (como em produção): a documentação não deve estar disponível.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SwaggerDisabledOutsideDevTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("A spec gerada não deve ser pública fora do perfil dev")
    void shouldNotExposeApiDocsOutsideDevProfile() {
        given()
        .when()
                .get("/v3/api-docs")
        .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("O Swagger UI não deve ser público fora do perfil dev")
    void shouldNotExposeSwaggerUiOutsideDevProfile() {
        given()
                .redirects().follow(false)
        .when()
                .get("/swagger-ui.html")
        .then()
                .statusCode(401);
    }
}
