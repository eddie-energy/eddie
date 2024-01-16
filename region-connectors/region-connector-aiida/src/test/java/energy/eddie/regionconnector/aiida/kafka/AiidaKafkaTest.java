package energy.eddie.regionconnector.aiida.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.aiida.dtos.TerminationRequest;
import energy.eddie.regionconnector.aiida.permission.request.AiidaDataSourceInformation;
import energy.eddie.regionconnector.aiida.permission.request.AiidaPermissionRequest;
import energy.eddie.regionconnector.aiida.services.AiidaRegionConnectorService;
import energy.eddie.regionconnector.aiida.states.AiidaAcceptedPermissionRequestState;
import energy.eddie.regionconnector.aiida.states.AiidaSentToPermissionAdministratorPermissionRequestState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import reactor.test.publisher.TestPublisher;

import java.time.Instant;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiidaKafkaTest {
    @Mock
    private AiidaRegionConnectorService mockService;
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
        kafka = new AiidaKafka(mapper, mockService, publisher.flux(), mockTemplate);
        dataSourceInformation = new AiidaDataSourceInformation();
    }

    @Test
    void givenAcceptedMessage_listenForConnectionStatusMessages_callsAccept() throws Exception {
        var request = spy(createTestRequest());

        // json that is received from AIIDA
        var message = new ConnectionStatusMessage(request.connectionId(), request.permissionId(), request.dataNeedId(),
                dataSourceInformation, PermissionProcessStatus.ACCEPTED);
        var json = mapper.writeValueAsString(message);

        // make sure the request is in a valid state
        request.changeState(new AiidaSentToPermissionAdministratorPermissionRequestState(request));

        when(mockService.getPermissionRequestById(request.permissionId())).thenReturn(request);

        // When
        kafka.listenForConnectionStatusMessages(json);

        // Then
        verify(mockService).getPermissionRequestById(anyString());
        verify(request).accept();
    }

    @ParameterizedTest
    @EnumSource(
            value = PermissionProcessStatus.class,
            names = {"REVOKED", "TIME_LIMIT", "TERMINATED"},
            mode = EnumSource.Mode.INCLUDE)
    void givenRevokedMessage_listenForConnectionStatusMessages_callsRevoke(PermissionProcessStatus status) throws Exception {
        var request = spy(createTestRequest());

        var message = new ConnectionStatusMessage(request.connectionId(), request.permissionId(), request.dataNeedId(),
                dataSourceInformation, status);
        var json = mapper.writeValueAsString(message);

        request.changeState(new AiidaAcceptedPermissionRequestState(request));

        when(mockService.getPermissionRequestById(request.permissionId())).thenReturn(request);

        // When
        kafka.listenForConnectionStatusMessages(json);

        // Then
        verify(mockService).getPermissionRequestById(anyString());

        switch (status) {
            case REVOKED -> verify(request).revoke();
            case TIME_LIMIT -> verify(request).timeLimit();
            case TERMINATED -> verify(request).terminate();
        }
    }

    @Test
    void givenTerminationRequest_sendsViaKafkaTemplate() {
        // Given
        var connectionId = "SomeId";
        var terminationTopic = "MyTerminationTopic";
        publisher.assertSubscribers(1);

        // When
        publisher.next(new TerminationRequest(connectionId, terminationTopic));

        // Then
        verify(mockTemplate).send(terminationTopic, connectionId, connectionId);
    }

    private AiidaPermissionRequest createTestRequest() {
        String connectionId = "TestConnectionId";
        String permissionId = "TestPermissionId";
        String terminationTopic = "TestTopic";
        String dataNeedId = "1";
        Instant start = Instant.now();
        return new AiidaPermissionRequest(permissionId, connectionId, dataNeedId, terminationTopic,
                start, start.plusSeconds(10000));
    }
    // TODO how to handle it, when we get status messages from AIIDA but request is in a different state? i.e. just random messages from aiida
}