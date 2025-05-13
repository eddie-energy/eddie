package energy.eddie.regionconnector.be.fluvius;

import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.v0_82.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.shared.timeout.CommonTimeoutService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SuppressWarnings("SpringBootApplicationProperties")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {"region-connector.be.fluvius.redirect-uri=http://localhost:8080"})
@Testcontainers
@Import(FluviusSpringConfigTests.TestConfig.class)
class FluviusSpringConfigTests {
    @SuppressWarnings("unused")
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> postgresqlContainer = new PostgreSQLContainer<>("postgres:15-alpine");
    @MockitoBean
    @SuppressWarnings("unused")
    private WebClient webClient;
    @MockitoBean
    @SuppressWarnings("unused")
    private DataNeedsService dataNeedsService;
    @MockitoBean
    @SuppressWarnings("unused")
    private CommonTimeoutService timeoutService;

    @Test
    void contextLoads() {
        // Check if spring context loads
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public CommonInformationModelConfiguration cimConfig() {
            return new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.BELGIUM_NATIONAL_CODING_SCHEME,
                                                                "epId");
        }
    }
}
