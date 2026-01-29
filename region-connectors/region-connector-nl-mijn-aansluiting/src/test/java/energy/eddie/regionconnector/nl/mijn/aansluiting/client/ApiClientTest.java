// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.nl.mijn.aansluiting.client;

import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.MijnAansluitingResponse;
import energy.eddie.regionconnector.nl.mijn.aansluiting.services.JsonResourceObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;
import tools.jackson.core.type.TypeReference;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiClientTest {

    private static final MockWebServer SERVER = new MockWebServer();

    @BeforeAll
    static void setup() throws IOException {
        SERVER.start();
    }

    @AfterAll
    static void teardown() throws IOException {
        SERVER.shutdown();
    }

    @Test
    void testFetchConsumptionData_returnsConsumptionData() throws IOException {
        // Given
        var jsonMapper = new JsonResourceObjectMapper<>(new TypeReference<List<MijnAansluitingResponse>>() {});
        var response = JsonResourceObjectMapper.loadRawTestJson("consumption_data.json");
        var expected = jsonMapper.loadTestJson("consumption_data.json");
        SERVER.enqueue(new MockResponse()
                               .setBody(response)
                               .addHeader("Content-Type", "application/json; charset=utf-8"));
        var singleSync = SERVER.url("/single-sync");
        var apiClient = new ApiClient(WebClient.builder());

        // When
        var res = apiClient.fetchConsumptionData(singleSync.toString(), "ACCESS TOKEN");


        // Then
        StepVerifier.create(res)
                    .assertNext(resp -> assertEquals(expected.size(), resp.size()))
                    .verifyComplete();
    }

    @Test
    void testFetchConsumptionData_updatesHealth_ifDown() {
        // Given
        var apiClient = new ApiClient(WebClient.builder());

        // When
        var res = apiClient.fetchConsumptionData("https://localhost:9999/", "ACCESS TOKEN");


        // Then
        StepVerifier.create(res)
                    .expectError()
                    .verify();
        assertEquals(Status.DOWN, apiClient.health().getStatus());
    }

    @Test
    void testFetchConsumptionData_updatesHealth_ifUp() throws IOException {
        // Given
        var response = JsonResourceObjectMapper.loadRawTestJson("consumption_data.json");
        SERVER.enqueue(new MockResponse()
                               .setBody(response)
                               .addHeader("Content-Type", "application/json; charset=utf-8"));
        var singleSync = SERVER.url("/single-sync");
        var apiClient = new ApiClient(WebClient.builder());

        // When
        var res = apiClient.fetchConsumptionData(singleSync.toString(), "ACCESS TOKEN");


        // Then
        StepVerifier.create(res)
                    .expectNextCount(1)
                    .verifyComplete();
        assertEquals(Status.UP, apiClient.health().getStatus());
    }
}