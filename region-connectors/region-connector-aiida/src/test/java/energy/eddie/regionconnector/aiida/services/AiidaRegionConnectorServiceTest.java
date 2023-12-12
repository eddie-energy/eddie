package energy.eddie.regionconnector.aiida.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.regionconnector.aiida.AiidaFactory;
import energy.eddie.regionconnector.aiida.api.AiidaPermissionRequest;
import energy.eddie.regionconnector.aiida.api.AiidaPermissionRequestRepository;
import energy.eddie.regionconnector.aiida.config.PlainAiidaConfiguration;
import energy.eddie.regionconnector.aiida.dtos.PermissionDto;
import energy.eddie.regionconnector.aiida.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.aiida.dtos.TerminationRequest;
import energy.eddie.regionconnector.aiida.states.AiidaAcceptedPermissionRequestState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiidaRegionConnectorServiceTest {
    private final String bootstrapServers = "localhost:9093";
    private final String dataTopic = "dataTopic";
    private final String statusTopic = "statusTopic";
    private final String terminationPrefix = "terminationPrefix";
    private final String connectionId = "TestConnectionId";
    @Mock
    private AiidaPermissionRequestRepository mockRepository;
    private Sinks.Many<TerminationRequest> terminationSink;
    private AiidaRegionConnectorService service;

    @BeforeEach
    void setUp() {
        StepVerifier.setDefaultTimeout(Duration.ofSeconds(2));

        var configuration = new PlainAiidaConfiguration(bootstrapServers, dataTopic, statusTopic, terminationPrefix);
        var factory = new AiidaFactory(configuration);
        terminationSink = Sinks.many().unicast().onBackpressureBuffer();
        service = new AiidaRegionConnectorService(factory, mockRepository, terminationSink);
    }

    @Test
    void verify_close_emitsCompleteOnConnectionStatusMessageFlux() {
        StepVerifier.create(JdkFlowAdapter.flowPublisherToFlux(service.getConnectionStatusMessageStream()))
                .then(service::close)
                .expectComplete()
                .verify();
    }

    @Test
    void verify_createNewPermission_persistsAndPublishesConnectionStatusMessage() throws StateTransitionException {
        String dataNeedId = "1";
        var request = new PermissionRequestForCreation(connectionId, dataNeedId);

        StepVerifier stepVerifier = StepVerifier.create(JdkFlowAdapter.flowPublisherToFlux(service.getConnectionStatusMessageStream()))
                .expectNextMatches(msg -> msg.status() == PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR)
                .then(service::close)
                .expectComplete()
                .verifyLater();


        PermissionDto newPermission = service.createNewPermission(request);

        assertEquals(connectionId, newPermission.connectionId());
        assertEquals(dataNeedId, newPermission.dataNeedId());
        assertEquals(bootstrapServers, newPermission.kafkaStreamingConfig().bootstrapServers());
        assertEquals(statusTopic, newPermission.kafkaStreamingConfig().statusTopic());
        assertEquals(dataTopic, newPermission.kafkaStreamingConfig().dataTopic());
        assertThat(newPermission.kafkaStreamingConfig().subscribeTopic()).startsWith(terminationPrefix + "_");

        verify(mockRepository).save(ArgumentMatchers.argThat(arg -> arg.connectionId().equals(connectionId)));
        stepVerifier.verify();
    }

    @Test
    void givenPermissionId_terminateRequest_publishesTerminationRequestOnFluxAndUpdatesDB() throws StateTransitionException {
        var permissionId = UUID.randomUUID().toString();
        var start = Instant.now();
        var expiration = start.plusSeconds(1000);
        AiidaPermissionRequest request = new AiidaPermissionRequest(permissionId, connectionId,
                "dataNeedId", "SomeTopic", start, expiration, service);
        request.changeState(new AiidaAcceptedPermissionRequestState(request));

        when(mockRepository.findByPermissionId(permissionId)).thenReturn(Optional.of(request));

        service.terminatePermission(permissionId);

        StepVerifier stepVerifier = StepVerifier.create(terminationSink.asFlux())
                .expectNextMatches(next -> next.connectionId().equals(connectionId))
                .then(service::close)
                .expectComplete()
                .verifyLater();

        verify(mockRepository).findByPermissionId(permissionId);
        verify(mockRepository).save(argThat(argument -> argument.state().status() == PermissionProcessStatus.TERMINATED));
        stepVerifier.verify();
    }

    @Test
    void givenNonExistingPermissionId_terminateRequest_doesNotPublish() throws StateTransitionException {
        // Given
        var permissionId = "NonExisting";
        when(mockRepository.findByPermissionId(permissionId)).thenReturn(Optional.empty());

        // When
        service.terminatePermission(permissionId);

        // Then
        StepVerifier.create(terminationSink.asFlux())
                .then(service::close)
                .expectComplete()
                .verify();

        verify(mockRepository).findByPermissionId(permissionId);
        verifyNoMoreInteractions(mockRepository);
    }
}