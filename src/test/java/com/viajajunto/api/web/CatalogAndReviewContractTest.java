package com.viajajunto.api.web;

import com.viajajunto.api.modules.activity.entity.CatalogoAtividadeEntity;
import com.viajajunto.api.modules.activity.repository.CatalogoAtividadeRepository;
import com.viajajunto.api.modules.destination.entity.DestinoCatalogoEntity;
import com.viajajunto.api.modules.destination.repository.DestinoCatalogoRepository;
import com.viajajunto.api.modules.review.entity.AvaliacaoEntity;
import com.viajajunto.api.modules.review.repository.AvaliacaoRepository;
import com.viajajunto.api.support.FirebaseTestTokens;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Catálogos de destinos e de atividades (rotas públicas) e criação de avaliações. O catálogo é criado
 * pelos repositórios porque o perfil padrão não carrega os dados de exemplo; o DynamoDB é simulado.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CatalogAndReviewContractTest {

    private static final String USER =
            FirebaseTestTokens.forUser("catalog-user-uid", "catalog-user@viajajunto.com");

    private static Long placeId;
    private static Long activityId;

    @LocalServerPort
    private int port;

    @Autowired
    private DestinoCatalogoRepository destinoCatalogoRepository;

    @Autowired
    private CatalogoAtividadeRepository catalogoAtividadeRepository;

    @MockitoBean
    private AvaliacaoRepository avaliacaoRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;

        if (placeId == null) {
            placeId = destinoCatalogoRepository.save(DestinoCatalogoEntity.builder()
                    .nome("Catalogoville")
                    .pais("Catalogolândia")
                    .codigoPaisIso("CTL")
                    .cidade("Catalogoville")
                    .categoria("cultural")
                    .avaliacaoMedia(4.9)
                    .build()).getId();
            activityId = catalogoAtividadeRepository.save(CatalogoAtividadeEntity.builder()
                    .nome("Tour Catalogado")
                    .tipo("Passeio")
                    .localizacao("Centro")
                    .cidade("Catalogoville")
                    .pais("Catalogolândia")
                    .precoMedio(new BigDecimal("50.00"))
                    .avaliacaoMedia(4.8)
                    .build()).getId();
        }

        when(avaliacaoRepository.save(any(AvaliacaoEntity.class))).thenAnswer(call -> call.getArgument(0));
        when(avaliacaoRepository.findAllByDestinoCatalogoId(any())).thenReturn(List.of());
        when(avaliacaoRepository.findAllByCatalogoAtividadeId(any())).thenReturn(List.of());
        when(avaliacaoRepository.resumirDestino(any())).thenReturn(new AvaliacaoRepository.ResumoNotas(1, 5.0));
        when(avaliacaoRepository.resumirAtividade(any())).thenReturn(new AvaliacaoRepository.ResumoNotas(1, 5.0));
    }

    // ----------------------------------------------------- catálogo de destinos

    @Test
    @DisplayName("GET /places busca no catálogo de destinos sem exigir login")
    void shouldSearchPlacesPublicly() {
        given()
                .queryParam("q", "Catalogoville")
        .when()
                .get("/places")
        .then()
                .statusCode(200)
                .body("content.nome", hasItem("Catalogoville"));
    }

    @Test
    @DisplayName("GET /places/{id} detalha o destino e responde 404 para um ID inexistente")
    void shouldGetPlaceByIdOrReturn404() {
        given()
        .when()
                .get("/places/{id}", placeId)
        .then()
                .statusCode(200)
                .body("nome", equalTo("Catalogoville"))
                .body("codigoPaisIso", equalTo("CTL"));

        given()
        .when()
                .get("/places/{id}", 999999)
        .then()
                .statusCode(404)
                .body("code", equalTo("NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /places/featured lista os destinos mais bem avaliados")
    void shouldListFeaturedPlaces() {
        given()
        .when()
                .get("/places/featured")
        .then()
                .statusCode(200)
                .body("nome", hasItem("Catalogoville"));
    }

    @Test
    @DisplayName("GET /places/visited-countries exige login e devolve 0 para quem ainda não viajou")
    void shouldReturnVisitedCountriesForAuthenticatedUser() {
        given()
        .when()
                .get("/places/visited-countries")
        .then()
                .statusCode(401);

        given()
                .header("Authorization", "Bearer " + USER)
        .when()
                .get("/places/visited-countries")
        .then()
                .statusCode(200)
                .body("totalVisited", equalTo(0));
    }

    // --------------------------------------------------- catálogo de atividades

    @Test
    @DisplayName("GET /activities/catalog busca atividades do catálogo sem exigir login")
    void shouldSearchActivityCatalogPublicly() {
        given()
                .queryParam("q", "Catalogado")
        .when()
                .get("/activities/catalog")
        .then()
                .statusCode(200)
                .body("content.nome", hasItem("Tour Catalogado"));
    }

    @Test
    @DisplayName("GET /activities/catalog/{id} detalha a atividade e responde 404 para ID inexistente")
    void shouldGetCatalogActivityByIdOrReturn404() {
        given()
        .when()
                .get("/activities/catalog/{id}", activityId)
        .then()
                .statusCode(200)
                .body("nome", equalTo("Tour Catalogado"));

        given()
        .when()
                .get("/activities/catalog/{id}", 999999)
        .then()
                .statusCode(404)
                .body("code", equalTo("NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /activities/catalog/featured lista as atividades mais bem avaliadas")
    void shouldListFeaturedCatalogActivities() {
        given()
        .when()
                .get("/activities/catalog/featured")
        .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("nome", hasItem("Tour Catalogado"));
    }

    // -------------------------------------------------------------- avaliações

    @Test
    @DisplayName("POST /places/{id}/reviews cria a avaliação do usuário autenticado")
    void shouldCreateReviewForPlace() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + USER)
                .body(Map.of("rating", 5, "comment", "Lugar incrível"))
        .when()
                .post("/places/{id}/reviews", placeId)
        .then()
                .statusCode(201)
                .body("nota", equalTo(5))
                .body("comentario", equalTo("Lugar incrível"))
                .body("destinoCatalogoId", equalTo(placeId.intValue()));

        // A avaliação atualiza a média e o total do destino no catálogo.
        given()
        .when()
                .get("/places/{id}", placeId)
        .then()
                .statusCode(200)
                .body("avaliacaoMedia", equalTo(5.0f))
                .body("totalAvaliacoes", equalTo(1));
    }

    @Test
    @DisplayName("Avaliação sem login responde 401, com nota inválida 422 e para destino inexistente 404")
    void shouldRejectInvalidReviews() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("rating", 5))
        .when()
                .post("/places/{id}/reviews", placeId)
        .then()
                .statusCode(401);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + USER)
                .body(Map.of("rating", 7))
        .when()
                .post("/places/{id}/reviews", placeId)
        .then()
                .statusCode(422)
                .body("code", equalTo("VALIDATION_ERROR"));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + USER)
                .body(Map.of("rating", 4))
        .when()
                .post("/places/{id}/reviews", 999999)
        .then()
                .statusCode(404)
                .body("code", equalTo("NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /places/{id}/activities/{activityId}/reviews avalia a atividade, sem vincular a nenhum destino")
    void shouldCreateReviewForActivity() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + USER)
                .body(Map.of("rating", 4, "comment", "Passeio ótimo"))
        .when()
                .post("/places/{placeId}/activities/{activityId}/reviews", placeId, activityId)
        .then()
                .statusCode(201)
                .body("nota", equalTo(4))
                .body("catalogoAtividadeId", equalTo(activityId.intValue()))
                .body("destinoCatalogoId", nullValue());
    }

    @Test
    @DisplayName("A avaliação de um destino ignora um catalogoAtividadeId vindo no corpo: o alvo é o do caminho")
    void shouldIgnoreActivityIdInBodyWhenReviewingPlace() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + USER)
                .body(Map.of("rating", 3, "catalogoAtividadeId", activityId))
        .when()
                .post("/places/{id}/reviews", placeId)
        .then()
                .statusCode(201)
                .body("destinoCatalogoId", equalTo(placeId.intValue()))
                .body("catalogoAtividadeId", nullValue());
    }

    @Test
    @DisplayName("Avaliar uma atividade sem login responde 401 e com nota inválida responde 422")
    void shouldRejectInvalidActivityReviews() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("rating", 4))
        .when()
                .post("/places/{placeId}/activities/{activityId}/reviews", placeId, activityId)
        .then()
                .statusCode(401);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + USER)
                .body(Map.of("rating", 9))
        .when()
                .post("/places/{placeId}/activities/{activityId}/reviews", placeId, activityId)
        .then()
                .statusCode(422)
                .body("code", equalTo("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("GET /places/{id}/activities/{activityId}/reviews lista avaliações da atividade sem login")
    void shouldListActivityReviewsPublicly() {
        given()
        .when()
                .get("/places/{placeId}/activities/{activityId}/reviews", placeId, activityId)
        .then()
                .statusCode(200)
                .body("content", hasSize(0));
    }
}
