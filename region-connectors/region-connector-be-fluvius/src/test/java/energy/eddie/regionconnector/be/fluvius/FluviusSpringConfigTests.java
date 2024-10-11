package energy.eddie.regionconnector.be.fluvius;

import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.shared.timeout.CommonTimeoutService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
class FluviusSpringConfigTests {
    @SuppressWarnings("unused")
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> postgresqlContainer = new PostgreSQLContainer<>("postgres:15-alpine");
    @MockBean
    @SuppressWarnings("unused")
    private WebClient webClient;
    @MockBean
    @SuppressWarnings("unused")
    private DataNeedsService dataNeedsService;
    @MockBean
    @SuppressWarnings("unused")
    private CommonTimeoutService timeoutService;

    @Test
    void contextLoads() {
        // Check if spring context loads
    }
}
