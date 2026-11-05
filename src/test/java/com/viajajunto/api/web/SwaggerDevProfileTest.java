package com.viajajunto.api.web;

import com.viajajunto.api.support.FirebaseTestTokens;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

// Banco H2 próprio para não colidir com os outros contextos de teste (ver SecurityDevProfileTest).
@ActiveProfiles("dev")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:viajajunto-swagger-dev-test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                "app.firebase.project-id=" + FirebaseTestTokens.PROJECT_ID
        }
)
class SwaggerDevProfileTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("No perfil dev a spec gerada é pública e lista as rotas implementadas")
    void shouldExposeGeneratedSpecInDevProfile() {
        given()
        .when()
                .get("/v3/api-docs")
        .then()
                .statusCode(200)
                .body("openapi", notNullValue())
                .body("info.title", equalTo("ViajaJunto API"))
                .body("paths", hasKey("/trips"))
                .body("paths", hasKey("/trips/{tripId}/map"))
                .body("paths", hasKey("/users/me"))
                .body("paths", hasKey("/places/{placeId}/reviews"));
    }

    @Test
    @DisplayName("Nenhuma rota legada /api/... aparece na documentação (elas foram removidas)")
    void shouldHideLegacyApiPrefixRoutes() {
        given()
        .when()
                .get("/v3/api-docs")
        .then()
                .statusCode(200)
                .body("paths.keySet().findAll { it.startsWith('/api/') }", is(empty()));
    }

    @Test
    @DisplayName("A spec declara o esquema Bearer e marca as rotas públicas sem exigência de token")
    void shouldDescribeBearerSecurityAndPublicRoutes() {
        given()
        .when()
                .get("/v3/api-docs")
        .then()
                .statusCode(200)
                .body("components.securitySchemes", hasKey("bearerAuth"))
                .body("components.securitySchemes.bearerAuth.scheme", equalTo("bearer"))
                // por padrão toda rota exige o esquema bearerAuth (exigência global da spec)
                .body("security.collect { it.keySet() }.flatten()", hasItem("bearerAuth"))
                // rota protegida herda a exigência global; rota pública a sobrescreve com lista vazia
                .body("paths['/trips'].get.security", is(nullValue()))
                .body("paths['/places'].get.security", is(empty()));
    }

    @Test
    @DisplayName("O Swagger UI abre no perfil dev")
    void shouldServeSwaggerUiInDevProfile() {
        given()
                .redirects().follow(false)
        .when()
                .get("/swagger-ui.html")
        .then()
                .statusCode(anyOf(is(200), is(302)));
    }

    @Test
    @DisplayName("Uma rota protegida continua exigindo token mesmo no perfil dev")
    void shouldKeepProtectedRoutesProtected() {
        given()
        .when()
                .get("/trips")
        .then()
                .statusCode(401)
                .body("code", equalTo("UNAUTHORIZED"));
    }
}
