package com.viajajunto.api.web;

import com.viajajunto.api.core.security.JwtService;
import com.viajajunto.api.modules.auth.entity.UserEntity;
import com.viajajunto.api.modules.auth.repository.UserRepository;
import com.viajajunto.api.modules.review.repository.AvaliacaoRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaginationAndNamingContractTest {

    private static final String MOCK_TOKEN = "valid_mock_token";
    private static final String MOCK_EMAIL = "contract-test@viajajunto.com";

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private JwtService jwtService;

    // Evita depender do DynamoDB/MiniStack rodando durante o teste.
    @MockitoBean
    private AvaliacaoRepository avaliacaoRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;

        if (userRepository.findByEmail(MOCK_EMAIL).isEmpty()) {
            userRepository.save(UserEntity.builder()
                    .nome("Contract Test")
                    .email(MOCK_EMAIL)
                    .senha("irrelevante")
                    .build());
        }

        when(jwtService.extractUsername(MOCK_TOKEN)).thenReturn(MOCK_EMAIL);
        when(jwtService.isTokenValid(eq(MOCK_TOKEN), any())).thenReturn(true);
        when(avaliacaoRepository.findAllByDestinoCatalogoId(any())).thenReturn(List.of());
    }

    private int createTrip(String nome) {
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + MOCK_TOKEN)
                .body(Map.of("nome", nome))
        .when()
                .post("/api/viagens")
        .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    @DisplayName("Deve listar roteiros no path '/trips' com envelope de paginação {data, meta}")
    void shouldListTripsWithStandardPagedEnvelope() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid_mock_token")
                .queryParam("page", 1)
                .queryParam("limit", 10)
        .when()
                .get("/trips")
        .then()
                .statusCode(200)
                .body("data", isA(java.util.List.class))
                .body("meta.page", equalTo(1))
                .body("meta.limit", equalTo(10))
                .body("meta.totalItems", notNullValue())
                .body("meta.totalPages", notNullValue());
    }

    @Test
    @DisplayName("Deve paginar '/trips' de verdade: page=2 e limit=1 devolvem 1 item e meta coerente")
    void shouldPaginateTripsAcrossPages() {
        createTrip("Viagem A");
        createTrip("Viagem B");

        given()
                .header("Authorization", "Bearer " + MOCK_TOKEN)
                .queryParam("page", 2)
                .queryParam("limit", 1)
        .when()
                .get("/trips")
        .then()
                .statusCode(200)
                .body("data", hasSize(1))
                .body("meta.page", equalTo(2))
                .body("meta.limit", equalTo(1))
                .body("meta.totalItems", greaterThanOrEqualTo(2))
                .body("meta.totalPages", greaterThanOrEqualTo(2));
    }

    @Test
    @DisplayName("Deve usar page=1 e limit=20 como padrão em '/trips'")
    void shouldApplyDefaultPagination() {
        given()
                .header("Authorization", "Bearer " + MOCK_TOKEN)
        .when()
                .get("/trips")
        .then()
                .statusCode(200)
                .body("meta.page", equalTo(1))
                .body("meta.limit", equalTo(20));
    }

    @Test
    @DisplayName("Deve listar membros no path '/trips/{tripId}/members' com envelope {data, meta}")
    void shouldListMembersWithPagedEnvelope() {
        int tripId = createTrip("Viagem Membros");

        given()
                .header("Authorization", "Bearer " + MOCK_TOKEN)
                .queryParam("page", 1)
                .queryParam("limit", 10)
        .when()
                .get("/trips/{tripId}/members", tripId)
        .then()
                .statusCode(200)
                .body("data", isA(java.util.List.class))
                .body("meta.page", equalTo(1))
                .body("meta.limit", equalTo(10))
                .body("meta.totalItems", notNullValue())
                .body("meta.totalPages", notNullValue());
    }

    @Test
    @DisplayName("Deve listar atividades no path '/trips/{tripId}/activities' com envelope {data, meta}")
    void shouldListActivitiesWithPagedEnvelope() {
        int tripId = createTrip("Viagem Atividades");

        given()
                .header("Authorization", "Bearer " + MOCK_TOKEN)
                .queryParam("page", 1)
                .queryParam("limit", 10)
        .when()
                .get("/trips/{tripId}/activities", tripId)
        .then()
                .statusCode(200)
                .body("data", isA(java.util.List.class))
                .body("meta.page", equalTo(1))
                .body("meta.limit", equalTo(10))
                .body("meta.totalItems", notNullValue())
                .body("meta.totalPages", notNullValue());
    }

    @Test
    @DisplayName("Deve listar avaliações em '/places/{placeId}/reviews' (rota pública) com envelope {data, meta}")
    void shouldListReviewsWithPagedEnvelopeWithoutAuthentication() {
        given()
                .queryParam("page", 1)
                .queryParam("limit", 10)
        .when()
                .get("/places/{placeId}/reviews", 1)
        .then()
                .statusCode(200)
                .body("data", isA(java.util.List.class))
                .body("meta.page", equalTo(1))
                .body("meta.limit", equalTo(10))
                .body("meta.totalItems", notNullValue())
                .body("meta.totalPages", notNullValue());
    }

    @Test
    @DisplayName("Rotas de listagem protegidas não devem responder 200 sem token")
    void shouldRejectProtectedListingsWithoutToken() {
        given().when().get("/trips").then().statusCode(not(200));
        given().when().get("/trips/1/members").then().statusCode(not(200));
        given().when().get("/trips/1/activities").then().statusCode(not(200));
    }

    @Test
    @DisplayName("Rotas antigas de listagem com prefixo '/api' não devem mais responder 200")
    void shouldNotExposeLegacyListingRoutes() {
        given()
                .header("Authorization", "Bearer " + MOCK_TOKEN)
        .when()
                .get("/api/viagens")
        .then()
                .statusCode(not(200));

        given().when().get("/api/avaliacoes/destino/1").then().statusCode(not(200));
    }
}
