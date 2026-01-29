// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.client;

import energy.eddie.regionconnector.es.datadis.api.DatadisApiException;
import energy.eddie.regionconnector.es.datadis.api.SupplyApi;
import energy.eddie.regionconnector.es.datadis.config.DatadisConfiguration;
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
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NettySupplyApiClientTest {
    private static MockWebServer mockBackEnd;
    private static DatadisConfiguration config;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeAll
    static void setUp() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
        String basePath = "http://localhost:" + mockBackEnd.getPort();
        config = new DatadisConfiguration("username", "password", basePath);
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockBackEnd.shutdown();
    }

    @Test
    void getSupplies_withWhenReceivingSupplies_returnsSupplies() {
        SupplyApi uut = new NettySupplyApiClient(
                HttpClient.create(),
                mapper,
                () -> Mono.just("token"),
                config
        );

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
                mapper,
                () -> Mono.just("token"),
                config
        );

        mockBackEnd.enqueue(new MockResponse()
                                    .setResponseCode(HttpStatus.FORBIDDEN.value()));

        StepVerifier.create(uut.getSupplies("nif", null))
                    .expectError(DatadisApiException.class)
                    .verify(Duration.ofSeconds(2));
    }
}