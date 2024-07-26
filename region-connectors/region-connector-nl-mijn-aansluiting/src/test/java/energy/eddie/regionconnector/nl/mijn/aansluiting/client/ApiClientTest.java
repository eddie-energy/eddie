package energy.eddie.regionconnector.nl.mijn.aansluiting.client;

import com.fasterxml.jackson.core.type.TypeReference;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.MijnAansluitingResponse;
import energy.eddie.regionconnector.nl.mijn.aansluiting.services.JsonResourceObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiClientTest {

    @Test
    void testFetchConsumptionData_returnsConsumptionData() throws IOException {
        // Given
        var jsonMapper = new JsonResourceObjectMapper<>(new TypeReference<List<MijnAansluitingResponse>>() {});
        var response = jsonMapper.loadRawTestJson("consumption_data.json");
        var expected = jsonMapper.loadTestJson("consumption_data.json");
        var server = new MockWebServer();
        server.enqueue(new MockResponse()
                               .setBody(response)
                               .addHeader("Content-Type", "application/json; charset=utf-8"));
        server.start();
        var singleSync = server.url("/single-sync");
        var apiClient = new ApiClient();

        // When
        var res = apiClient.fetchConsumptionData(singleSync.toString(), "ACCESS TOKEN");


        // Then
        StepVerifier.create(res)
                    .assertNext(resp -> assertEquals(expected.size(), resp.size()))
                    .verifyComplete();
        // Clean-Up
        server.close();
    }

    @Test
    void testFetchConsumptionData_updatesHealth_ifDown() {
        // Given
        var apiClient = new ApiClient();

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
        var jsonMapper = new JsonResourceObjectMapper<>(new TypeReference<List<MijnAansluitingResponse>>() {});
        var response = jsonMapper.loadRawTestJson("consumption_data.json");
        var server = new MockWebServer();
        server.enqueue(new MockResponse()
                               .setBody(response)
                               .addHeader("Content-Type", "application/json; charset=utf-8"));
        server.start();
        var singleSync = server.url("/single-sync");
        var apiClient = new ApiClient();

        // When
        var res = apiClient.fetchConsumptionData(singleSync.toString(), "ACCESS TOKEN");


        // Then
        StepVerifier.create(res)
                    .expectNextCount(1)
                    .verifyComplete();
        assertEquals(Status.UP, apiClient.health().getStatus());

        // Clean-Up
        server.close();
    }
}