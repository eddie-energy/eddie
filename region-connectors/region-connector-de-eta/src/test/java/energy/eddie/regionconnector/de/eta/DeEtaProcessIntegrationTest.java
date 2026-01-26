package energy.eddie.regionconnector.de.eta;

import com.github.tomakehurst.wiremock.client.WireMock;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequestRepository;
import energy.eddie.regionconnector.de.eta.permission.request.events.AcceptedEvent;
import energy.eddie.regionconnector.de.eta.permission.request.events.CreatedEvent;
import energy.eddie.regionconnector.de.eta.permission.request.events.ValidatedEvent;
import energy.eddie.regionconnector.de.eta.providers.cim.v104.DeValidatedHistoricalDataMarketDocumentProvider;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import static org.mockito.Mockito.atLeastOnce;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.time.ZoneId;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "REGION_CONNECTOR_DE_ETA_BASEPATH=http://localhost:9090",
                "REGION_CONNECTOR_DE_ETA_USERNAME=test-user",
                "REGION_CONNECTOR_DE_ETA_PASSWORD=test-pass",
                "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
                "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect"
        }
)
@Testcontainers
@AutoConfigureWireMock(port = 9090)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(DeEtaProcessIntegrationTest.TestConfig.class)
class DeEtaProcessIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public static BeanPostProcessor entityManagerFactoryInterfaceConfigurer() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessBeforeInitialization(Object bean, String beanName) {
                    if (bean instanceof LocalContainerEntityManagerFactoryBean factoryBean) {
                        factoryBean.setEntityManagerFactoryInterface(jakarta.persistence.EntityManagerFactory.class);
                    }
                    return bean;
                }
            };
        }

        @Bean
        @Primary
        public energy.eddie.api.cim.config.CommonInformationModelConfiguration commonInformationModelConfiguration() {
            var config = Mockito.mock(energy.eddie.api.cim.config.CommonInformationModelConfiguration.class, Mockito.RETURNS_DEEP_STUBS);
            org.mockito.BDDMockito.given(config.eligiblePartyNationalCodingScheme().value()).willReturn("A10");
            return config;
        }
    }

    @Autowired private Outbox outbox;
    @Autowired private DePermissionRequestRepository repository;

    @MockitoSpyBean
    private DeValidatedHistoricalDataMarketDocumentProvider publisher;

    @MockitoBean
    private DataNeedsService dataNeedsService;

    @BeforeEach
    void setup() {
        WireMock.reset();
        repository.deleteAll();
    }

    @Test
    void shouldFetchAndPublishDataOnAcceptedEvent() {
        String permissionId = UUID.randomUUID().toString();
        String meterId = "malo-123";

        ValidatedHistoricalDataDataNeed realDataNeed = Mockito.mock(ValidatedHistoricalDataDataNeed.class);
        given(realDataNeed.energyType()).willReturn(EnergyType.ELECTRICITY);
        given(realDataNeed.type()).willReturn(ValidatedHistoricalDataDataNeed.DISCRIMINATOR_VALUE);

        given(dataNeedsService.getById(anyString())).willReturn(realDataNeed);

        String dateStr = LocalDate.now(ZoneId.of("UTC")).minusDays(1).format(DateTimeFormatter.ISO_DATE);
        stubEtaApi(meterId, dateStr, 123.45);

        outbox.commit(new CreatedEvent(permissionId, "need-1", "conn-1", meterId));
        outbox.commit(new ValidatedEvent(
                permissionId,
                LocalDate.now(ZoneId.of("UTC")).minusDays(10),
                LocalDate.now(ZoneId.of("UTC")).minusDays(1),
                Granularity.PT15M
        ));

        await().atMost(Duration.ofSeconds(2)).untilAsserted(() ->
                assertThat(repository.findByPermissionId(permissionId).get().start()).isNotNull()
        );

        outbox.commit(new AcceptedEvent(permissionId));

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            DePermissionRequest req = repository.findByPermissionId(permissionId).get();
            assertThat(req.latestReading()).isPresent();
        });

        verify(publisher, atLeastOnce()).emitDocument(any());
        WireMock.verify(getRequestedFor(urlPathMatching("/api/meters/historical")));
    }

    private void stubEtaApi(String meterId, String dateString, double value) {
        String timestamp = dateString + "T00:00:00Z";
        WireMock.stubFor(get(urlPathMatching("/api/meters/historical"))
                .withQueryParam("meteringPointId", equalTo(meterId))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(String.format("""
                            {
                                "meteringPointId": "%s",
                                "readings": [
                                    { "timestamp": "%s", "value": %s, "unit": "KWH", "status": "A04" }
                                ]
                            }
                        """, meterId, timestamp, value))));
    }
}