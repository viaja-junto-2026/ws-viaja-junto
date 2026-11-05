package com.viajajunto.api.web;

import com.viajajunto.api.modules.review.repository.AvaliacaoRepository;
import com.viajajunto.api.modules.trip.service.ViagemService;
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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Erros causados pelo cliente e gerados pelo próprio Spring devem responder 4xx no formato padrão
 * ({code, message, details}), e não 500. Todos os casos falham antes de chegar ao serviço de viagens,
 * que é simulado só para o teste do erro inesperado.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FrameworkErrorsContractTest {

    private static final String TOKEN =
            FirebaseTestTokens.forUser("framework-errors-uid", "framework-errors@viajajunto.com");

    @LocalServerPort
    private int port;

    @MockitoBean
    private ViagemService viagemService;

    // Sem chamadas ao DynamoDB durante o teste.
    @MockitoBean
    private AvaliacaoRepository avaliacaoRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("JSON malformado responde 400 MALFORMED_REQUEST, sem expor o erro interno do parser")
    void shouldReturn400ForMalformedJson() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + TOKEN)
                .body("{ nome: ")
        .when()
                .post("/trips")
        .then()
                .statusCode(400)
                .body("code", equalTo("MALFORMED_REQUEST"))
                .body("message", not(containsString("Jackson")))
                .body("message", not(containsString("Unexpected")));
    }

    @Test
    @DisplayName("Corpo ausente em rota que exige JSON responde 400 MALFORMED_REQUEST")
    void shouldReturn400ForMissingBody() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + TOKEN)
        .when()
                .post("/trips")
        .then()
                .statusCode(400)
                .body("code", equalTo("MALFORMED_REQUEST"));
    }

    @Test
    @DisplayName("Método HTTP não suportado responde 405 com o cabeçalho Allow")
    void shouldReturn405WithAllowHeader() {
        given()
                .header("Authorization", "Bearer " + TOKEN)
        .when()
                .delete("/trips")
        .then()
                .statusCode(405)
                .header("Allow", containsString("GET"))
                .header("Allow", containsString("POST"))
                .body("code", equalTo("METHOD_NOT_ALLOWED"));
    }

    @Test
    @DisplayName("Content-Type não suportado responde 415 UNSUPPORTED_MEDIA_TYPE")
    void shouldReturn415ForUnsupportedContentType() {
        given()
                .contentType(ContentType.TEXT)
                .header("Authorization", "Bearer " + TOKEN)
                .body("nome=viagem")
        .when()
                .post("/trips")
        .then()
                .statusCode(415)
                .body("code", equalTo("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    @DisplayName("Parâmetro com tipo errado (page=abc) responde 400 INVALID_PARAMETER indicando o campo")
    void shouldReturn400ForParameterTypeMismatch() {
        given()
                .header("Authorization", "Bearer " + TOKEN)
                .queryParam("page", "abc")
        .when()
                .get("/trips")
        .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_PARAMETER"))
                .body("details[0].field", equalTo("page"));
    }

    @Test
    @DisplayName("Erro realmente inesperado continua respondendo 500, sem vazar a mensagem interna")
    void shouldKeepReturning500WithoutLeakingInternalMessage() {
        when(viagemService.listUserTrips(any(), any()))
                .thenThrow(new IllegalStateException("detalhe interno secreto"));

        given()
                .header("Authorization", "Bearer " + TOKEN)
        .when()
                .get("/trips")
        .then()
                .statusCode(500)
                .body("code", equalTo("INTERNAL_SERVER_ERROR"))
                .body("message", not(containsString("secreto")));
    }
}
