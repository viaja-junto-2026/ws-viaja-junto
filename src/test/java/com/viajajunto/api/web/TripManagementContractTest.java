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

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Fluxos de viagem pela API: viagem, orçamento, destinos, atividades e membros, com os cenários de
 * permissão e de erro. Não usa o DynamoDB (o repositório de avaliações é simulado).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TripManagementContractTest {

    private static final String OWNER =
            FirebaseTestTokens.forUser("flow-owner-uid", "flow-owner@viajajunto.com");
    private static final String MEMBER =
            FirebaseTestTokens.forUser("flow-member-uid", "flow-member@viajajunto.com");
    private static final String OUTSIDER =
            FirebaseTestTokens.forUser("flow-outsider-uid", "flow-outsider@viajajunto.com");

    @LocalServerPort
    private int port;

    @MockitoBean
    private AvaliacaoRepository avaliacaoRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    private Map<String, Object> createTripResponse(String token, String name) {
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(Map.of("nome", name))
        .when()
                .post("/trips")
        .then()
                .statusCode(201)
                .extract().path("$");
    }

    private int createTrip(String name) {
        return (Integer) createTripResponse(OWNER, name).get("id");
    }

    private int createDestination(int tripId, String name) {
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + OWNER)
                .body(Map.of("nome", name, "pais", "Portugal"))
        .when()
                .post("/trips/{tripId}/destinations", tripId)
        .then()
                .statusCode(201)
                .extract().path("id");
    }

    private Map<String, Object> activity(String name, String start, int minutes, double cost) {
        Map<String, Object> body = new HashMap<>();
        body.put("nome", name);
        body.put("dataHorario", start);
        body.put("duracaoMinutos", minutes);
        body.put("custoPrevisto", cost);
        return body;
    }

    // ---------------------------------------------------------------- viagem

    @Test
    @DisplayName("PUT /trips/{id} atualiza a viagem e o GET devolve os novos dados")
    void shouldUpdateTrip() {
        int tripId = createTrip("Viagem Original");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + OWNER)
                .body(Map.of("nome", "Viagem Renomeada", "descricao", "Nova descrição"))
        .when()
                .put("/trips/{id}", tripId)
        .then()
                .statusCode(200)
                .body("nome", equalTo("Viagem Renomeada"));

        given()
                .header("Authorization", "Bearer " + OWNER)
        .when()
                .get("/trips/{id}", tripId)
        .then()
                .statusCode(200)
                .body("nome", equalTo("Viagem Renomeada"))
                .body("descricao", equalTo("Nova descrição"));
    }

    @Test
    @DisplayName("GET /trips/{id} aceita o ID com prefixo trp_ e responde 404 para ID inválido")
    void shouldResolveTripIdWithPrefixAndRejectInvalidId() {
        int tripId = createTrip("Viagem Prefixo");

        given()
                .header("Authorization", "Bearer " + OWNER)
        .when()
                .get("/trips/trp_{id}", tripId)
        .then()
                .statusCode(200)
                .body("id", equalTo(tripId));

        given()
                .header("Authorization", "Bearer " + OWNER)
        .when()
                .get("/trips/abc")
        .then()
                .statusCode(404)
                .body("code", equalTo("NOT_FOUND"));
    }

    @Test
    @DisplayName("DELETE /trips/{id} remove a viagem e depois ela responde 404")
    void shouldDeleteTrip() {
        int tripId = createTrip("Viagem Para Excluir");

        given()
                .header("Authorization", "Bearer " + OWNER)
        .when()
                .delete("/trips/{id}", tripId)
        .then()
                .statusCode(204);

        given()
                .header("Authorization", "Bearer " + OWNER)
        .when()
                .get("/trips/{id}", tripId)
        .then()
                .statusCode(404)
                .body("code", equalTo("NOT_FOUND"));
    }

    @Test
    @DisplayName("Quem não é membro não acessa nem exclui a viagem (403)")
    void shouldForbidOutsiderOnTrip() {
        int tripId = createTrip("Viagem Privada");

        given()
                .header("Authorization", "Bearer " + OUTSIDER)
        .when()
                .get("/trips/{id}", tripId)
        .then()
                .statusCode(403)
                .body("code", equalTo("FORBIDDEN"));

        given()
                .header("Authorization", "Bearer " + OUTSIDER)
        .when()
                .delete("/trips/{id}", tripId)
        .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("Dados inválidos na criação da viagem respondem 422 com os campos em details")
    void shouldValidateTripCreation() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + OWNER)
                .body(Map.of("nome", ""))
        .when()
                .post("/trips")
        .then()
                .statusCode(422)
                .body("code", equalTo("VALIDATION_ERROR"))
                .body("details.field", notNullValue());
    }

    // ------------------------------------------------------------- orçamento

    @Test
    @DisplayName("PUT /trips/{id}/budget define o orçamento e o resumo reflete o valor")
    void shouldUpdateBudget() {
        int tripId = createTrip("Viagem Orçamento");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + OWNER)
                .body(Map.of("orcamentoTotal", 1000))
        .when()
                .put("/trips/{id}/budget", tripId)
        .then()
                .statusCode(200)
                .body("viagemId", equalTo(tripId));

        double total = given()
                .header("Authorization", "Bearer " + OWNER)
        .when()
                .get("/trips/{id}/budget", tripId)
        .then()
                .statusCode(200)
                .extract().jsonPath().getDouble("orcamentoTotal");

        assertEquals(1000.0, total, 0.001);
    }

    @Test
    @DisplayName("Orçamento negativo é rejeitado com 422")
    void shouldRejectNegativeBudget() {
        int tripId = createTrip("Viagem Orçamento Negativo");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + OWNER)
                .body(Map.of("orcamentoTotal", -5))
        .when()
                .put("/trips/{id}/budget", tripId)
        .then()
                .statusCode(422)
                .body("code", equalTo("VALIDATION_ERROR"));
    }

    // -------------------------------------------------------------- destinos

    @Test
    @DisplayName("Destino da viagem: cria, lista e remove")
    void shouldCreateListAndDeleteDestination() {
        int tripId = createTrip("Viagem Destinos");
        int destinationId = createDestination(tripId, "Lisboa");

        given()
                .header("Authorization", "Bearer " + OWNER)
        .when()
                .get("/trips/{tripId}/destinations", tripId)
        .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].nome", equalTo("Lisboa"))
                .body("[0].ordemVisita", equalTo(1));

        given()
                .header("Authorization", "Bearer " + OWNER)
        .when()
                .delete("/trips/{tripId}/destinations/{id}", tripId, destinationId)
        .then()
                .statusCode(204);

        given()
                .header("Authorization", "Bearer " + OWNER)
        .when()
                .get("/trips/{tripId}/destinations", tripId)
        .then()
                .statusCode(200)
                .body("$", empty());
    }

    // ------------------------------------------------------------ atividades

    @Test
    @DisplayName("Atividades: cria, lista por destino e por viagem (paginado), soma no orçamento e remove")
    void shouldManageActivities() {
        int tripId = createTrip("Viagem Atividades Fluxo");
        int destinationId = createDestination(tripId, "Porto");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + OWNER)
                .body(activity("Passeio de barco", "2026-12-01T10:00:00", 60, 80.0))
        .when()
                .post("/trips/{tripId}/destinations/{destinationId}/activities", tripId, destinationId)
        .then()
                .statusCode(201)
                .body("nome", equalTo("Passeio de barco"));

        int activityId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + OWNER)
                .body(activity("Almoço", "2026-12-01T12:00:00", 60, 20.0))
        .when()
                .post("/trips/{tripId}/destinations/{destinationId}/activities", tripId, destinationId)
        .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .header("Authorization", "Bearer " + OWNER)
        .when()
                .get("/trips/{tripId}/destinations/{destinationId}/activities", tripId, destinationId)
        .then()
                .statusCode(200)
                .body("$", hasSize(2));

        given()
                .header("Authorization", "Bearer " + OWNER)
                .queryParam("limit", 1)
        .when()
                .get("/trips/{tripId}/activities", tripId)
        .then()
                .statusCode(200)
                .body("data", hasSize(1))
                .body("meta.totalItems", equalTo(2))
                .body("meta.totalPages", equalTo(2));

        double planned = given()
                .header("Authorization", "Bearer " + OWNER)
        .when()
                .get("/trips/{id}/budget", tripId)
        .then()
                .statusCode(200)
                .extract().jsonPath().getDouble("totalPlanejado");
        assertEquals(100.0, planned, 0.001);

        given()
                .header("Authorization", "Bearer " + OWNER)
        .when()
                .delete("/trips/{tripId}/activities/{id}", tripId, activityId)
        .then()
                .statusCode(204);
    }

    @Test
    @DisplayName("Atividade com horário sobreposto responde 409 SCHEDULE_CONFLICT")
    void shouldRejectOverlappingActivity() {
        int tripId = createTrip("Viagem Conflito");
        int destinationId = createDestination(tripId, "Faro");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + OWNER)
                .body(activity("Primeira", "2026-12-02T10:00:00", 60, 0.0))
        .when()
                .post("/trips/{tripId}/destinations/{destinationId}/activities", tripId, destinationId)
        .then()
                .statusCode(201);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + OWNER)
                .body(activity("Sobreposta", "2026-12-02T10:30:00", 30, 0.0))
        .when()
                .post("/trips/{tripId}/destinations/{destinationId}/activities", tripId, destinationId)
        .then()
                .statusCode(409)
                .body("code", equalTo("SCHEDULE_CONFLICT"));
    }

    // --------------------------------------------------------------- membros

    @Test
    @DisplayName("Membros: entra com o código de convite, lista, muda a permissão e remove")
    void shouldManageMembers() {
        Map<String, Object> trip = createTripResponse(OWNER, "Viagem em Grupo");
        int tripId = (Integer) trip.get("id");
        String inviteCode = (String) trip.get("codigoConvite");

        int memberId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + MEMBER)
                .body(Map.of("codigoConvite", inviteCode))
        .when()
                .post("/trips/{tripId}/members/join", tripId)
        .then()
                .statusCode(201)
                .body("permissao", equalTo("EDITOR"))
                .extract().path("id");

        given()
                .header("Authorization", "Bearer " + OWNER)
        .when()
                .get("/trips/{tripId}/members", tripId)
        .then()
                .statusCode(200)
                .body("data", hasSize(1))
                .body("meta.totalItems", equalTo(1));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + OWNER)
                .body(Map.of("permissao", "VISUALIZADOR"))
        .when()
                .patch("/trips/{tripId}/members/{memberId}", tripId, memberId)
        .then()
                .statusCode(200)
                .body("permissao", equalTo("VISUALIZADOR"));

        // Visualizador consulta, mas não altera a viagem.
        given()
                .header("Authorization", "Bearer " + MEMBER)
        .when()
                .get("/trips/{id}", tripId)
        .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + MEMBER)
                .body(Map.of("nome", "Tentativa"))
        .when()
                .post("/trips/{tripId}/destinations", tripId)
        .then()
                .statusCode(403)
                .body("code", equalTo("FORBIDDEN"));

        given()
                .header("Authorization", "Bearer " + OWNER)
        .when()
                .delete("/trips/{tripId}/members/{memberId}", tripId, memberId)
        .then()
                .statusCode(204);
    }

    @Test
    @DisplayName("Código de convite inválido responde 400 e quem não é dono não altera permissões")
    void shouldRejectInvalidInviteAndNonOwnerPermissionChange() {
        int tripId = createTrip("Viagem Convite");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + MEMBER)
                .body(Map.of("codigoConvite", "CODIGO-INEXISTENTE"))
        .when()
                .post("/trips/{tripId}/members/join", tripId)
        .then()
                .statusCode(400)
                .body("code", equalTo("BUSINESS_RULE_VIOLATION"));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + OUTSIDER)
                .body(Map.of("permissao", "EDITOR"))
        .when()
                .patch("/trips/{tripId}/members/{memberId}", tripId, 1)
        .then()
                .statusCode(403);
    }
}
