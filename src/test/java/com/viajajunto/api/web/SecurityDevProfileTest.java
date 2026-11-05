package com.viajajunto.api.web;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

// O perfil dev carrega o data.sql (dados de exemplo). O H2 em memória é compartilhado entre os
// contextos de teste da mesma JVM, então este teste usa um banco só dele para não colidir com
// os registros criados pelos outros.
@ActiveProfiles("dev")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:viajajunto-dev-profile-test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
)
class SecurityDevProfileTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("H2 console deve estar acessível apenas no perfil dev")
    void shouldExposeH2ConsoleInDevProfile() {
        given()
                .redirects().follow(false)
        .when()
                .get("/h2-console/")
        .then()
                .statusCode(anyOf(is(200), is(302)));
    }

    @Test
    @DisplayName("CORS: origem local configurada no perfil dev deve ser aceita no preflight")
    void shouldAcceptCorsPreflightFromConfiguredDevOrigin() {
        given()
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET")
        .when()
                .options("/trips")
        .then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", equalTo("http://localhost:3000"));
    }

    @Test
    @DisplayName("CORS: origem fora da lista deve ser rejeitada mesmo no perfil dev")
    void shouldRejectCorsPreflightFromUnknownOriginInDevProfile() {
        given()
                .header("Origin", "http://origem-desconhecida.example")
                .header("Access-Control-Request-Method", "GET")
        .when()
                .options("/trips")
        .then()
                .statusCode(403);
    }
}
