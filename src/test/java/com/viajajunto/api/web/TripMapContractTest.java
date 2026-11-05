package com.viajajunto.api.web;

import com.viajajunto.api.integrations.google.MapsClientPort;
import com.viajajunto.api.integrations.google.PlacesResponseDTO;
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

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TripMapContractTest {

    private static final String TOKEN =
            FirebaseTestTokens.forUser("map-test-uid", "map-test@viajajunto.com");
    private static final String OTHER_USER_TOKEN =
            FirebaseTestTokens.forUser("map-other-uid", "map-other@viajajunto.com");

    @LocalServerPort
    private int port;

    // Sem chamadas reais ao Google nem ao DynamoDB durante o teste.
    @MockitoBean
    private MapsClientPort mapsClient;

    @MockitoBean
    private AvaliacaoRepository avaliacaoRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;

        when(mapsClient.searchPlaces(anyString())).thenReturn(PlacesResponseDTO.builder()
                .status("OK")
                .results(List.of(PlacesResponseDTO.PlaceResult.builder()
                        .geometry(PlacesResponseDTO.Geometry.builder()
                                .location(PlacesResponseDTO.Location.builder().lat(38.7223).lng(-9.1393).build())
                                .build())
                        .build()))
                .build());
    }

    private int createTrip() {
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + TOKEN)
                .body(Map.of("nome", "Viagem Mapa"))
        .when()
                .post("/trips")
        .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    @DisplayName("GET /trips/{tripId}/map devolve os destinos como marcadores com latitude e longitude")
    void shouldReturnMarkersForTripDestinations() {
        int tripId = createTrip();
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + TOKEN)
                .body(Map.of("nome", "Lisboa", "pais", "Portugal"))
        .when()
                .post("/trips/{tripId}/destinations", tripId)
        .then()
                .statusCode(201);

        given()
                .header("Authorization", "Bearer " + TOKEN)
        .when()
                .get("/trips/{tripId}/map", tripId)
        .then()
                .statusCode(200)
                .body("markers", hasSize(1))
                .body("markers[0].type", equalTo("destination"))
                .body("markers[0].label", equalTo("Lisboa"))
                .body("markers[0].latitude", equalTo(38.7223f))
                .body("markers[0].longitude", equalTo(-9.1393f));
    }

    @Test
    @DisplayName("GET /trips/{tripId}/map sem token responde 401")
    void shouldRequireAuthentication() {
        given()
        .when()
                .get("/trips/1/map")
        .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("GET /trips/{tripId}/map de uma viagem de outra pessoa responde 403")
    void shouldForbidTripOfAnotherUser() {
        int tripId = createTrip();

        given()
                .header("Authorization", "Bearer " + OTHER_USER_TOKEN)
        .when()
                .get("/trips/{tripId}/map", tripId)
        .then()
                .statusCode(403);
    }
}
