package energy.eddie.regionconnector.aiida;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.regionconnector.aiida.services.AiidaPermissionService;
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
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiidaRegionConnectorTest {
    @Mock
    private AiidaPermissionService mockService;
    private AiidaRegionConnector connector;
    private final Sinks.Many<ConnectionStatusMessage> statusSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<PermissionEnvelope> documentSink = Sinks.many().multicast().onBackpressureBuffer();

    @BeforeEach
    void setUp() {
        connector = new AiidaRegionConnector(statusSink, documentSink, mockService);
    }

    @Test
    void getMetadata_MdaCodeIsAiida() {
        assertEquals("aiida", connector.getMetadata().id());
    }

    @Test
    void verify_terminate_callsMqtt() {
        // Given
        var permissionId = UUID.randomUUID().toString();

        // When
        connector.terminatePermission(permissionId);

        // Then
        verify(mockService).terminatePermission(permissionId);
    }

    @Test
    void close_emitsCompleteOnStatusMessageFlux() {
        StepVerifier.create(connector.getConnectionStatusMessageStream())
                    .then(() -> connector.close())
                    .expectComplete()
                    .verify(Duration.ofSeconds(2));
    }

    @Test
    void close_emitsCompleteOnPermissionDocumentFlux() {
        StepVerifier.create(connector.getPermissionMarketDocumentStream())
                    .then(() -> connector.close())
                    .expectComplete()
                    .verify(Duration.ofSeconds(2));
    }
}
