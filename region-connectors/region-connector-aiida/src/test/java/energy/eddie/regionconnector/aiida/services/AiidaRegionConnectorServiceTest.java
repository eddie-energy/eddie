package energy.eddie.regionconnector.aiida.services;

import energy.eddie.api.agnostic.DataNeed;
import energy.eddie.api.agnostic.DataNeedsService;
import energy.eddie.api.agnostic.DataType;
import energy.eddie.api.agnostic.Granularity;
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
import energy.eddie.regionconnector.shared.exceptions.DataNeedNotFoundException;
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
import java.util.Set;
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
    @Mock
    private DataNeedsService dataNeedsService;
    private Sinks.Many<TerminationRequest> terminationSink;
    private AiidaRegionConnectorService service;

    @BeforeEach
    void setUp() {
        StepVerifier.setDefaultTimeout(Duration.ofSeconds(2));

        var configuration = new PlainAiidaConfiguration(bootstrapServers, dataTopic, statusTopic, terminationPrefix);
        var factory = new AiidaFactory(configuration, dataNeedsService);
        terminationSink = Sinks.many().unicast().onBackpressureBuffer();
        service = new AiidaRegionConnectorService(factory, mockRepository, terminationSink);
    }

    @Test
    void verify_close_emitsCompleteOnConnectionStatusMessageFlux() {
        // Given
        StepVerifier.create(JdkFlowAdapter.flowPublisherToFlux(service.getConnectionStatusMessageStream()))
                // When
                .then(service::close)
                // Then
                .expectComplete()
                .verify(Duration.ofSeconds(2));
    }

    @Test
    void verify_createNewPermission_persistsAndPublishesConnectionStatusMessage() throws StateTransitionException, DataNeedNotFoundException {
        // Given
        String dataNeedId = "1";
        var request = new PermissionRequestForCreation(connectionId, dataNeedId);
        when(dataNeedsService.getDataNeed(dataNeedId)).thenReturn(Optional.of(new TestDataNeed(dataNeedId)));

        StepVerifier stepVerifier = StepVerifier.create(JdkFlowAdapter.flowPublisherToFlux(service.getConnectionStatusMessageStream()))
                .expectNextMatches(msg -> msg.status() == PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR)
                .then(service::close)
                .expectComplete()
                .verifyLater();


        // When
        PermissionDto newPermission = service.createNewPermission(request);

        // Then
        assertEquals(connectionId, newPermission.connectionId());
        assertEquals(dataNeedId, newPermission.dataNeedId());
        assertThat(newPermission.requestedCodes()).hasSameElementsAs(Set.of("1-0:1.8.0", "1-0:1.7.0"));
        assertEquals("Test service name", newPermission.serviceName());
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

    private record TestDataNeed(String dataNeedId) implements DataNeed {
        @Override
        public String id() {
            return dataNeedId;
        }

        @Override
        public String description() {
            return "Test description";
        }

        @Override
        public DataType type() {
            return DataType.AIIDA_NEAR_REALTIME_DATA;
        }

        @Override
        public Granularity granularity() {
            return Granularity.P1M;
        }

        @Override
        public Integer durationStart() {
            return 0;
        }

        @Override
        public Boolean durationOpenEnd() {
            return false;
        }

        @Override
        public Integer durationEnd() {
            return 5;
        }

        @Override
        public Integer transmissionInterval() {
            return 12;
        }

        @Override
        public Set<String> sharedDataIds() {
            return Set.of("1-0:1.8.0", "1-0:1.7.0");
        }

        @Override
        public String serviceName() {
            return "Test service name";
        }
    }
}