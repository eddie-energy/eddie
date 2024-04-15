package energy.eddie.regionconnector.aiida.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.api.agnostic.ConnectionStatusMessageMixin;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.aiida.permission.request.AiidaDataSourceInformation;
import energy.eddie.regionconnector.aiida.permission.request.AiidaPermissionRequest;
import energy.eddie.regionconnector.aiida.permission.request.persistence.AiidaPermissionRequestViewRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiidaKafkaTest {
    @Mock
    private KafkaTemplate<String, String> mockTemplate;
    @Mock
    private Outbox mockOutbox;
    @Mock
    private AiidaPermissionRequestViewRepository mockRepository;
    @Mock
    private AiidaPermissionRequest mockRequest;
    private AiidaKafka aiidaKafka;
    private ObjectMapper mapper;
    private DataSourceInformation dataSourceInformation;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        mapper.addMixIn(ConnectionStatusMessage.class, ConnectionStatusMessageMixin.class);

        aiidaKafka = new AiidaKafka(mapper,
                                    mockTemplate,
                                    mockOutbox,
                                    mockRepository);
        dataSourceInformation = new AiidaDataSourceInformation();
    }

    @Test
    void givenTerminationRequest_sendsViaKafkaTemplate() {
        // Given
        String connectionId = "connectionId";
        String terminationTopic = "terminationTopic";
        when(mockRepository.findById(anyString())).thenReturn(Optional.of(mockRequest));
        when(mockRequest.connectionId()).thenReturn(connectionId);
        when(mockRequest.terminationTopic()).thenReturn(terminationTopic);

        // When
        aiidaKafka.sendTerminationRequest("permissionId");

        // Then
        verify(mockTemplate).send(terminationTopic, connectionId, connectionId);
    }

    @ParameterizedTest
    @EnumSource(value = PermissionProcessStatus.class,
            names = {"ACCEPTED", "TERMINATED", "REVOKED", "FULFILLED"},
            mode = EnumSource.Mode.INCLUDE)
    void givenConnectionStatusMessageFromAiida_commitsCorrespondingPermissionEvent(PermissionProcessStatus status) throws JsonProcessingException {
        String connectionId = "TestConnectionId";
        String permissionId = "TestPermissionId";
        String dataNeedId = "1";
        // json that is received from AIIDA
        var message = new ConnectionStatusMessage(connectionId, permissionId, dataNeedId,
                                                  dataSourceInformation, status);
        var json = mapper.writeValueAsString(message);

        // When
        aiidaKafka.listenForConnectionStatusMessages(json);

        // Then
        verify(mockOutbox).commit(argThat(event -> event.status() == status));
    }

    @ParameterizedTest
    @EnumSource(value = PermissionProcessStatus.class,
            names = {"ACCEPTED", "TERMINATED", "REVOKED", "FULFILLED"},
            mode = EnumSource.Mode.EXCLUDE)
    void givenConnectionStatusMessageFromAiidaWithNotHandledStatus_doesNotCommitPermissionEvent(PermissionProcessStatus status) throws JsonProcessingException {
        String connectionId = "TestConnectionId";
        String permissionId = "TestPermissionId";
        String dataNeedId = "1";
        // json that is received from AIIDA
        var message = new ConnectionStatusMessage(connectionId, permissionId, dataNeedId,
                                                  dataSourceInformation, status);
        var json = mapper.writeValueAsString(message);

        // When
        aiidaKafka.listenForConnectionStatusMessages(json);

        // Then
        verifyNoInteractions(mockOutbox);
    }
}
