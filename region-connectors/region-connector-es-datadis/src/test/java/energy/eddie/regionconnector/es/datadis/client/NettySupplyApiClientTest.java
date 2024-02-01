package energy.eddie.regionconnector.es.datadis.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.regionconnector.es.datadis.api.DatadisApiException;
import energy.eddie.regionconnector.es.datadis.api.SupplyApi;
import energy.eddie.regionconnector.es.datadis.dtos.Supply;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.ZONE_ID_SPAIN;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NettySupplyApiClientTest {
    static MockWebServer mockBackEnd;
    private static String basePath;

    @BeforeAll
    static void setUp() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
        basePath = "http://localhost:" + mockBackEnd.getPort();
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockBackEnd.shutdown();
    }

    @Test
    void getSupplies_withWhenReceivingSupplies_returnsSupplies() throws JsonProcessingException {
        SupplyApi uut = new NettySupplyApiClient(
                HttpClient.create(),
                () -> Mono.just("token"),
                basePath);

        var mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        var supply = new Supply("a", "a", "a", "a", "a", "a", LocalDate.now(ZONE_ID_SPAIN), null, 4, "3");

        String body = mapper.writeValueAsString(List.of(supply));

        mockBackEnd.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.OK.value())
                .setBody(body));

        StepVerifier.create(uut.getSupplies("nif", null))
                .assertNext(supplies -> {
                    assertEquals(1, supplies.size());
                    assertEquals(supply, supplies.getFirst());
                })
                .expectComplete()
                .verify(Duration.ofSeconds(2));
    }

    @Test
    void getSupplies_whenReceivingForbidden_producesDatadisApiException() {
        SupplyApi uut = new NettySupplyApiClient(
                HttpClient.create(),
                () -> Mono.just("token"),
                basePath);

        mockBackEnd.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.FORBIDDEN.value()));

        StepVerifier.create(uut.getSupplies("nif", null))
                .expectError(DatadisApiException.class)
                .verify(Duration.ofSeconds(2));
    }
}