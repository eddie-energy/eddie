package energy.eddie.regionconnector.fi.fingrid;

import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.shared.security.JwtUtil;
import energy.eddie.regionconnector.shared.timeout.TimeoutConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest()
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({FingridSpringConfigurationTests.TestConfig.class})
class FingridSpringConfigurationTests {
    @SuppressWarnings("unused")
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> postgresqlContainer = new PostgreSQLContainer<>("postgres:15-alpine");

    @MockBean
    @SuppressWarnings("unused")
    private DataNeedsService dataNeedsService;
    @MockBean
    @SuppressWarnings("unused")
    private WebClient webClient;

    @Test
    void contextLoads() {
        // This test checks if the spring context still loads
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public TimeoutConfiguration timeoutConfiguration() {
            return new TimeoutConfiguration(1);
        }

        @Bean
        public JwtUtil jwtUtil() {
            return new JwtUtil("MjVjNmIxNDM1N2I0MWEzNWI0MWMzZmYwMWRmNjA2Yjc", 1);
        }
    }
}
