package energy.eddie.regionconnector.es.datadis.health;

import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.es.datadis.api.DatadisApiException;
import energy.eddie.regionconnector.shared.security.JwtUtil;
import energy.eddie.regionconnector.shared.timeout.TimeoutConfiguration;
import io.netty.handler.codec.http.HttpResponseStatus;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@TestPropertySource(properties = {
        "region-connector.es.datadis.username=username",
        "region-connector.es.datadis.password=username",
        "region-connector.es.datadis.basepath=https://datadis.es",
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class DatadisApiHealthIndicatorTest {
    @SuppressWarnings("unused")
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> postgresqlContainer = new PostgreSQLContainer<>("postgres:15-alpine");
    private static MockWebServer mockBackEnd;
    private static String path;
    @Autowired
    private HttpClient httpClient;
    @Autowired
    private DatadisApiHealthIndicator healthIndicator;
    @MockBean
    @SuppressWarnings("unused")
    private DataNeedsService dataNeedsService;
    @MockBean
    @SuppressWarnings("unused")
    private JwtUtil jwtUtil;
    @MockBean
    @SuppressWarnings("unused")
    private TimeoutConfiguration timeoutConfiguration;
    @Autowired
    private DatadisApiHealthIndicator datadisApiHealthIndicator;

    @BeforeAll
    static void setUp() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
        path = "http://localhost:" + mockBackEnd.getPort();
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockBackEnd.shutdown();
    }

    @Test
    void onSuccessfulRestRequest_healthIndicatorUp() {
        // Given
        mockBackEnd.enqueue(new MockResponse().setResponseCode(200));
        var resp = httpClient.get()
                             .uri(path)
                             .response();

        // When
        resp.block();

        // Then
        assertEquals(Health.up().build(), healthIndicator.health());
    }

    @SuppressWarnings("ReactiveStreamsUnusedPublisher")
    @Test
    void onServerUnavailable_healthIndicatorDown() {
        // Given
        var resp = httpClient.get()
                             .uri("http://unknown-host")
                             .response();

        // When
        assertThrows(Exception.class, resp::block);

        // Then
        assertEquals(Status.DOWN, healthIndicator.health().getStatus());
    }

    @Test
    void downWith5xxError_healthIndicatorDown() {
        // Given
        var exception = new DatadisApiException("blbl", HttpResponseStatus.INTERNAL_SERVER_ERROR, "");

        // When
        datadisApiHealthIndicator.down(exception);

        // Then
        assertEquals(Status.DOWN, healthIndicator.health().getStatus());
    }

    @Test
    void downWith4xxError_healthIndicatorUnchanged() {
        // Given
        var exception = new DatadisApiException("blbl", HttpResponseStatus.FORBIDDEN, "");
        healthIndicator.up();

        // When
        datadisApiHealthIndicator.down(exception);

        // Then
        assertEquals(Status.UP, healthIndicator.health().getStatus());
    }

    @Test
    void unknownException_healthIndicatorUnchanged() {

        // Given
        var exception = new RuntimeException();
        healthIndicator.up();

        // When
        datadisApiHealthIndicator.down(exception);

        // Then
        assertEquals(Status.UP, healthIndicator.health().getStatus());
    }
}