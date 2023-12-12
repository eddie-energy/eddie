package energy.eddie.regionconnector.aiida.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.aiida.api.AiidaDataSourceInformation;
import energy.eddie.regionconnector.aiida.api.AiidaPermissionRequest;
import energy.eddie.regionconnector.aiida.api.AiidaPermissionRequestRepository;
import energy.eddie.regionconnector.aiida.dtos.TerminationRequest;
import energy.eddie.regionconnector.aiida.services.AiidaRegionConnectorService;
import energy.eddie.regionconnector.aiida.states.AiidaAcceptedPermissionRequestState;
import energy.eddie.regionconnector.aiida.states.AiidaSentToPermissionAdministratorPermissionRequestState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import reactor.test.publisher.TestPublisher;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiidaKafkaTest {
    @Mock
    private AiidaPermissionRequestRepository mockRepository;
    private AiidaKafka kafka;
    private ObjectMapper mapper;
    @Mock
    private KafkaTemplate<String, String> mockTemplate;
    private DataSourceInformation dataSourceInformation;
    private TestPublisher<TerminationRequest> publisher;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        mapper.addMixIn(ConnectionStatusMessage.class, ConnectionStatusMessageMixin.class);

        publisher = TestPublisher.create();
        kafka = new AiidaKafka(mapper, mockRepository, publisher.flux(), mockTemplate);
        dataSourceInformation = new AiidaDataSourceInformation();
    }

    @Test
    void givenAcceptedMessage_listenForConnectionStatusMessages_changesStateAndPersists() throws JsonProcessingException {
        var request = createTestRequest();

        // json that is received from AIIDA
        var message = new ConnectionStatusMessage(request.connectionId(), request.permissionId(), request.dataNeedId(),
                dataSourceInformation, PermissionProcessStatus.ACCEPTED);
        var json = mapper.writeValueAsString(message);

        // make sure the request is in a valid state
        request.changeState(new AiidaSentToPermissionAdministratorPermissionRequestState(request));

        when(mockRepository.findByPermissionId(request.permissionId())).thenReturn(Optional.of(request));

        kafka.listenForConnectionStatusMessages(json);

        verify(mockRepository).save(argThat(arg -> arg.state().status() == PermissionProcessStatus.ACCEPTED));
    }

    @Test
    void givenTerminatedMessage_listenForConnectionStatusMessages_changesStateAndPersists() throws JsonProcessingException {
        var request = createTestRequest();

        var message = new ConnectionStatusMessage(request.connectionId(), request.permissionId(), request.dataNeedId(),
                dataSourceInformation, PermissionProcessStatus.TERMINATED);
        var json = mapper.writeValueAsString(message);

        request.changeState(new AiidaAcceptedPermissionRequestState(request));

        when(mockRepository.findByPermissionId(request.permissionId())).thenReturn(Optional.of(request));

        kafka.listenForConnectionStatusMessages(json);

        verify(mockRepository).save(argThat(arg -> arg.state().status() == PermissionProcessStatus.TERMINATED));
    }

    @Test
    void givenRevokedMessage_listenForConnectionStatusMessages_changesStateAndPersists() throws JsonProcessingException {
        var request = createTestRequest();

        var message = new ConnectionStatusMessage(request.connectionId(), request.permissionId(), request.dataNeedId(),
                dataSourceInformation, PermissionProcessStatus.REVOKED);
        var json = mapper.writeValueAsString(message);

        request.changeState(new AiidaAcceptedPermissionRequestState(request));

        when(mockRepository.findByPermissionId(request.permissionId())).thenReturn(Optional.of(request));

        kafka.listenForConnectionStatusMessages(json);

        verify(mockRepository).save(argThat(arg -> arg.state().status() == PermissionProcessStatus.REVOKED));
    }

    @Test
    void givenTimeLimitMessage_listenForConnectionStatusMessages_changesStateAndPersists() throws JsonProcessingException {
        var request = createTestRequest();

        var message = new ConnectionStatusMessage(request.connectionId(), request.permissionId(), request.dataNeedId(),
                dataSourceInformation, PermissionProcessStatus.TIME_LIMIT);
        var json = mapper.writeValueAsString(message);

        request.changeState(new AiidaAcceptedPermissionRequestState(request));

        when(mockRepository.findByPermissionId(request.permissionId())).thenReturn(Optional.of(request));

        kafka.listenForConnectionStatusMessages(json);

        verify(mockRepository).save(argThat(arg -> arg.state().status() == PermissionProcessStatus.TIME_LIMIT));
    }

    private AiidaPermissionRequest createTestRequest() {
        String connectionId = "TestConnectionId";
        String permissionId = "TestPermissionId";
        String terminationTopic = "TestTopic";
        String dataNeedId = "1";
        Instant start = Instant.now();
        return new AiidaPermissionRequest(permissionId, connectionId, dataNeedId, terminationTopic,
                start, start.plusSeconds(10000), mock(AiidaRegionConnectorService.class));
    }

    @Test
    void givenTerminationRequest_sendsViaKafkaTemplate() {
        var connectionId = "SomeId";
        var terminationTopic = "MyTerminationTopic";

        publisher.assertSubscribers(1);

        publisher.next(new TerminationRequest(connectionId, terminationTopic));

        verify(mockTemplate).send(terminationTopic, connectionId, connectionId);
    }

    // TODO how to handle it, when we get status messages from AIIDA but request is in a different state? i.e. just random messages from aiida
}