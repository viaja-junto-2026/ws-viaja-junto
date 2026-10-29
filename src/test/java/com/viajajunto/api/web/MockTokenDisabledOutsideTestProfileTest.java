package com.viajajunto.api.web;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.not;

// Sem o perfil "test", o atalho "token_mock" não pode autenticar ninguém.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MockTokenDisabledOutsideTestProfileTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("Não deve autenticar com 'token_mock' quando o perfil 'test' não está ativo")
    void shouldNotAuthenticateWithMockTokenOutsideTestProfile() {
        given()
                .header("Authorization", "Bearer token_mock")
        .when()
                .get("/trips")
        .then()
                .statusCode(not(200));
    }
}
