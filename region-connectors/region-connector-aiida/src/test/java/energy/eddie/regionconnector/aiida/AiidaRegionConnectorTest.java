package energy.eddie.regionconnector.aiida;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.HealthState;
import energy.eddie.cim.v0_82.cmd.ConsentMarketDocument;
import energy.eddie.regionconnector.aiida.kafka.AiidaKafka;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiidaRegionConnectorTest {
    private final String expectedRcId = "aiida";
    @Mock
    private AiidaKafka aiidaKafka;
    private AiidaRegionConnector connector;
    private final Sinks.Many<ConnectionStatusMessage> statusSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<ConsentMarketDocument> documentSink = Sinks.many().multicast().onBackpressureBuffer();

    @BeforeEach
    void setUp() {
        connector = new AiidaRegionConnector(aiidaKafka, statusSink, documentSink);
    }

    @Test
    void getMetadata_MdaCodeIsAiida() {
        assertEquals(expectedRcId, connector.getMetadata().id());
    }

    @Test
    void verify_terminate_callsAiidaKafka() {
        var permissionId = UUID.randomUUID().toString();
        connector.terminatePermission(permissionId);

        verify(aiidaKafka).sendTerminationRequest(permissionId);
    }

    @Test
    void verify_healthStateIsAlwaysHealthy() {
        HealthState healthState = connector.health().get(expectedRcId);

        assertNotNull(healthState);
        assertEquals(HealthState.UP, healthState);
    }

    @Test
    void close_emitsCompleteOnStatusMessageFlux() {
        StepVerifier.create(connector.getConnectionStatusMessageStream())
                    .then(() -> connector.close())
                    .expectComplete()
                    .verify(Duration.ofSeconds(2));
    }

    @Test
    void close_emitsCompleteOnConsentDocumentFlux() {
        StepVerifier.create(connector.getConsentMarketDocumentStream())
                    .then(() -> connector.close())
                    .expectComplete()
                    .verify(Duration.ofSeconds(2));
    }
}
