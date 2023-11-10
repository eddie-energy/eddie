package energy.eddie.regionconnector.aiida.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.aiida.AiidaFactory;
import energy.eddie.regionconnector.aiida.api.AiidaPermissionRequestRepository;
import energy.eddie.regionconnector.aiida.config.PlainAiidaConfiguration;
import energy.eddie.regionconnector.aiida.dtos.PermissionDto;
import energy.eddie.regionconnector.aiida.dtos.PermissionRequestForCreation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiidaRegionConnectorServiceTest {
    private final String bootstrapServers = "localhost:9093";
    private final String dataTopic = "dataTopic";
    private final String statusTopic = "statusTopic";
    private final String terminationPrefix = "terminationPrefix";
    @Mock
    private AiidaPermissionRequestRepository mockRepository;
    private AiidaRegionConnectorService service;

    @BeforeEach
    void setUp() {
        StepVerifier.setDefaultTimeout(Duration.ofSeconds(2));

        var configuration = new PlainAiidaConfiguration(bootstrapServers, dataTopic, statusTopic, terminationPrefix);
        var factory = new AiidaFactory(configuration);
        service = new AiidaRegionConnectorService(factory, mockRepository);
    }

    @Test
    void verify_close_emitsCompleteOnConnectionStatusMessageFlux() {
        StepVerifier.create(service.connectionStatusMessageFlux())
                .then(service::close)
                .expectComplete()
                .verify();
    }

    @Test
    void verify_createNewPermission_persistsAndPublishesConnectionStatusMessage() {
        var connectionId = "TestConnectionId";
        var start = Instant.now();
        var expiration = start.plusSeconds(1000);
        var request = new PermissionRequestForCreation(connectionId, "1", start, expiration);

        StepVerifier stepVerifier = StepVerifier.create(service.connectionStatusMessageFlux())
                .expectNextMatches(msg -> msg.status() == PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR)
                .then(service::close)
                .expectComplete()
                .verifyLater();


        PermissionDto newPermission = service.createNewPermission(request);

        assertEquals(connectionId, newPermission.connectionId());
        assertEquals(start, newPermission.startTime());
        assertEquals(expiration, newPermission.expirationTime());
        assertEquals(bootstrapServers, newPermission.kafkaStreamingConfig().bootstrapServers());
        assertEquals(statusTopic, newPermission.kafkaStreamingConfig().statusTopic());
        assertEquals(dataTopic, newPermission.kafkaStreamingConfig().dataTopic());
        assertEquals(terminationPrefix + "_" + connectionId, newPermission.kafkaStreamingConfig().subscribeTopic());

        verify(mockRepository).save(ArgumentMatchers.argThat(arg -> arg.connectionId().equals(connectionId)));
        stepVerifier.verify();
    }
}