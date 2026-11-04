package com.viajajunto.api.web;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

// Sem perfil ativo (configuração segura por padrão, como em produção).
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityDefaultProfileTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("Rota protegida sem token deve responder 401 no formato padrão de erro")
    void shouldReturn401WithStandardErrorWhenTokenIsMissing() {
        given()
        .when()
                .get("/trips")
        .then()
                .statusCode(401)
                .body("code", equalTo("UNAUTHORIZED"))
                .body("message", notNullValue());
    }

    @Test
    @DisplayName("Rota protegida com token inválido deve responder 401")
    void shouldReturn401WhenTokenIsInvalid() {
        given()
                .header("Authorization", "Bearer token.invalido.qualquer")
        .when()
                .get("/trips")
        .then()
                .statusCode(401)
                .body("code", equalTo("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("H2 console não deve ficar acessível fora do perfil dev")
    void shouldNotExposeH2ConsoleOutsideDevProfile() {
        given()
                .redirects().follow(false)
        .when()
                .get("/h2-console/")
        .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("Endpoints sensíveis do actuator não devem ser públicos")
    void shouldNotExposeActuatorEndpointsPublicly() {
        given()
        .when()
                .get("/actuator/env")
        .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("CORS: origem não configurada deve ser rejeitada")
    void shouldRejectCorsPreflightFromUnknownOrigin() {
        given()
                .header("Origin", "http://origem-desconhecida.example")
                .header("Access-Control-Request-Method", "GET")
        .when()
                .options("/trips")
        .then()
                .statusCode(403);
    }
}
