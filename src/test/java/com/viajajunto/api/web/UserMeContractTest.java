package com.viajajunto.api.web;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

import com.viajajunto.api.support.FirebaseTestTokens;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserMeContractTest {

    @LocalServerPort
    private int port;

    private String uid;
    private String email;
    private String token;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        // Um usuário novo por teste: o H2 em memória é compartilhado entre as classes de teste.
        String id = UUID.randomUUID().toString();
        uid = "me-" + id;
        email = "me-" + id + "@viajajunto.com";
        token = FirebaseTestTokens.token().uid(uid).email(email).name("Maria Viajante")
                .picture("https://exemplo.com/maria.png").build();
    }

    private int meId() {
        return given().header("Authorization", "Bearer " + token)
                .when().get("/users/me")
                .then().statusCode(200).extract().path("id");
    }

    @Test
    @DisplayName("GET /users/me cria o usuário no primeiro acesso com os dados do token")
    void shouldCreateUserOnFirstAccess() {
        given()
                .header("Authorization", "Bearer " + token)
        .when()
                .get("/users/me")
        .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("nome", equalTo("Maria Viajante"))
                .body("email", equalTo(email))
                .body("avatarUrl", equalTo("https://exemplo.com/maria.png"))
                .body("dataCriacao", notNullValue());
    }

    @Test
    @DisplayName("O segundo acesso devolve o mesmo usuário, sem criar outro")
    void shouldReturnSameUserOnSecondAccess() {
        int primeiro = meId();
        int segundo = meId();

        org.junit.jupiter.api.Assertions.assertEquals(primeiro, segundo);
    }

    @Test
    @DisplayName("O usuário é encontrado pelo firebaseUid, mesmo que o e-mail do token mude")
    void shouldFindUserByFirebaseUidEvenIfEmailChanges() {
        int original = meId();
        String tokenComOutroEmail = FirebaseTestTokens.token().uid(uid).email("novo-" + email).build();

        int depois = given().header("Authorization", "Bearer " + tokenComOutroEmail)
                .when().get("/users/me")
                .then().statusCode(200)
                .body("email", equalTo(email))
                .extract().path("id");

        org.junit.jupiter.api.Assertions.assertEquals(original, depois);
    }

    @Test
    @DisplayName("PATCH /users/me altera só o que foi enviado")
    void shouldPatchOnlySentFields() {
        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("nome", "  Maria da Silva  "))
        .when()
                .patch("/users/me")
        .then()
                .statusCode(200)
                .body("nome", equalTo("Maria da Silva"))
                .body("avatarUrl", equalTo("https://exemplo.com/maria.png"));

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("avatarUrl", "https://exemplo.com/nova.png"))
        .when()
                .patch("/users/me")
        .then()
                .statusCode(200)
                .body("nome", equalTo("Maria da Silva"))
                .body("avatarUrl", equalTo("https://exemplo.com/nova.png"));

        given()
                .header("Authorization", "Bearer " + token)
        .when()
                .get("/users/me")
        .then()
                .statusCode(200)
                .body("nome", equalTo("Maria da Silva"));
    }

    @Test
    @DisplayName("PATCH com corpo vazio não altera nada")
    void shouldKeepUserWhenPatchBodyIsEmpty() {
        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{}")
        .when()
                .patch("/users/me")
        .then()
                .statusCode(200)
                .body("nome", equalTo("Maria Viajante"));
    }

    @Test
    @DisplayName("PATCH com nome em branco responde 422 com o campo")
    void shouldRejectBlankNameWith422() {
        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("nome", "   "))
        .when()
                .patch("/users/me")
        .then()
                .statusCode(422)
                .body("code", equalTo("VALIDATION_ERROR"))
                .body("details.field", hasItem("nome"));
    }

    @Test
    @DisplayName("PATCH sem token responde 401")
    void shouldRejectPatchWithoutToken() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("nome", "Alguém"))
        .when()
                .patch("/users/me")
        .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("As rotas de login e cadastro locais não existem mais")
    void shouldNotExposeLocalAuthRoutes() {
        given().contentType(ContentType.JSON).body("{}").when().post("/auth/login").then().statusCode(401);
        given().contentType(ContentType.JSON).body("{}").when().post("/auth/register").then().statusCode(401);
        given().contentType(ContentType.JSON).body("{}").when().post("/api/auth/login").then().statusCode(401);
    }
}
