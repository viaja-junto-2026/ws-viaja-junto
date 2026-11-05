package com.viajajunto.api.web;

import com.viajajunto.api.modules.review.repository.AvaliacaoRepository;
import com.viajajunto.api.support.FirebaseTestTokens;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Executa, com os mesmos corpos e a mesma ordem, o roteiro do "Passo 8" do README (viagem, destino,
 * atividades e orçamento). Se o comportamento da API mudar, este teste falha e avisa que o README precisa
 * ser atualizado. A parte de avaliações (DynamoDB) é coberta pelo CatalogAndReviewContractTest.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReadmeWalkthroughContractTest {

    private static final String TOKEN =
            FirebaseTestTokens.forUser("readme-walkthrough-uid", "readme-walkthrough@viajajunto.com");

    // Corpos copiados do README (Passo 8).
    private static final String BODY_TRIP = """
            {
              "nome": "Viagem de teste",
              "descricao": "Testando o Swagger",
              "dataInicio": "2026-12-01",
              "dataFim": "2026-12-10",
              "orcamentoTotal": 5000
            }
            """;
    private static final String BODY_DESTINATION = """
            { "nome": "Lisboa", "pais": "Portugal" }
            """;
    private static final String BODY_ACTIVITY_BOAT = """
            {
              "nome": "Passeio de barco",
              "tipo": "Passeio",
              "dataHorario": "2026-12-02T10:00:00",
              "duracaoMinutos": 60,
              "custoPrevisto": 600
            }
            """;
    private static final String BODY_ACTIVITY_DINNER = """
            {
              "nome": "Jantar tradicional",
              "tipo": "Gastronomia",
              "dataHorario": "2026-12-02T20:00:00",
              "duracaoMinutos": 90,
              "custoPrevisto": 400
            }
            """;
    private static final String BODY_ACTIVITY_OVERLAP = """
            {
              "nome": "Atividade sobreposta",
              "dataHorario": "2026-12-02T10:30:00",
              "duracaoMinutos": 30,
              "custoPrevisto": 10
            }
            """;

    @LocalServerPort
    private int port;

    @MockitoBean
    private AvaliacaoRepository avaliacaoRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    private io.restassured.specification.RequestSpecification authenticated() {
        return given().contentType(ContentType.JSON).header("Authorization", "Bearer " + TOKEN);
    }

    @Test
    @DisplayName("O roteiro do README funciona do jeito descrito (passos 1 a 8 e os erros de demonstração)")
    void shouldFollowTheReadmeWalkthrough() {
        // Passo 1: GET /users/me cria e devolve o usuário do token
        authenticated().when().get("/users/me").then().statusCode(200);

        // Passo 2: criar a viagem
        int tripId = authenticated().body(BODY_TRIP).when().post("/trips")
                .then().statusCode(201).extract().path("id");

        // Passo 3: a viagem aparece na listagem paginada
        authenticated().when().get("/trips")
                .then().statusCode(200).body("data.id", org.hamcrest.Matchers.hasItem(tripId));

        // Passo 4: criar o destino
        int destinationId = authenticated().body(BODY_DESTINATION)
                .when().post("/trips/{tripId}/destinations", tripId)
                .then().statusCode(201).extract().path("id");

        // Passo 5: criar as duas atividades
        authenticated().body(BODY_ACTIVITY_BOAT)
                .when().post("/trips/{tripId}/destinations/{destinationId}/activities", tripId, destinationId)
                .then().statusCode(201);
        authenticated().body(BODY_ACTIVITY_DINNER)
                .when().post("/trips/{tripId}/destinations/{destinationId}/activities", tripId, destinationId)
                .then().statusCode(201);

        // Passo 6: o resumo do orçamento
        var budget = authenticated().when().get("/trips/{tripId}/budget", tripId)
                .then().statusCode(200).extract().jsonPath();
        assertEquals(1000.0, budget.getDouble("totalPlanejado"), 0.001);
        assertEquals(4000.0, budget.getDouble("saldoDisponivel"), 0.001);
        assertEquals(20.0, budget.getDouble("percentualConsumidoTotal"), 0.001);
        assertEquals(2, budget.getList("gastosPorCategoria").size());

        // Passo 7: alterar o orçamento
        authenticated().body("{ \"orcamentoTotal\": 8000 }")
                .when().put("/trips/{tripId}/budget", tripId)
                .then().statusCode(200);
        assertEquals(8000.0,
                authenticated().when().get("/trips/{tripId}/budget", tripId)
                        .then().extract().jsonPath().getDouble("orcamentoTotal"), 0.001);

        // Passo 8: as duas atividades na listagem paginada
        authenticated().when().get("/trips/{tripId}/activities", tripId)
                .then().statusCode(200).body("data", hasSize(2)).body("meta.totalItems", equalTo(2));

        // Erros de demonstração
        authenticated().body(BODY_ACTIVITY_OVERLAP)
                .when().post("/trips/{tripId}/destinations/{destinationId}/activities", tripId, destinationId)
                .then().statusCode(409).body("code", equalTo("SCHEDULE_CONFLICT"));
        authenticated().body("{ \"nome\": \"\" }").when().post("/trips")
                .then().statusCode(422).body("code", equalTo("VALIDATION_ERROR"));
        authenticated().body("{ \"nome\": ").when().post("/trips")
                .then().statusCode(400).body("code", equalTo("MALFORMED_REQUEST"));
        authenticated().when().get("/trips/99999/budget")
                .then().statusCode(404).body("code", equalTo("NOT_FOUND"));
        given().when().get("/trips").then().statusCode(401).body("code", equalTo("UNAUTHORIZED"));
    }
}
