package energy.eddie.aiida.streamers;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.aggregator.Aggregator;
import energy.eddie.aiida.config.AiidaConfiguration;
import energy.eddie.aiida.dtos.ConnectionStatusMessage;
import energy.eddie.aiida.errors.ConnectionStatusMessageSendFailedException;
import energy.eddie.aiida.models.permission.MqttStreamingConfig;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import energy.eddie.aiida.repositories.FailedToSendRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StreamerManagerTest {
    @Mock
    private Aggregator aggregatorMock;
    @Mock
    private FailedToSendRepository mockRepository;
    private Permission permission;
    private StreamerManager manager;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new AiidaConfiguration().objectMapper();
        manager = new StreamerManager(mapper, aggregatorMock, mockRepository);

        var permissionId = "72831e2c-a01c-41b8-9db6-3f51670df7a5";
        var dataNeedId = "dataNeedId";
        var grant = Instant.parse("2023-08-01T10:00:00.00Z");
        var start = grant.plusSeconds(100_000);
        var expiration = start.plusSeconds(800_000);
        var serviceName = "My NewAIIDA Test Service";
        var connectionId = "NewAiidaRandomConnectionId";
        var codes = Set.of("1.8.0", "2.8.0");

        permission = new Permission(permissionId,
                                    serviceName,
                                    dataNeedId,
                                    start,
                                    expiration,
                                    grant,
                                    connectionId,
                                    codes,
                                    MqttStreamingConfig.getFixedConfig(permissionId));
    }

    @Test
    void givenSamePermissionTwice_createNewStreamerForPermission_throws() {
        when(aggregatorMock.getFilteredFlux(any(), any())).thenReturn(Flux.empty());

        // first time should result in valid creation
        assertDoesNotThrow(() -> manager.createNewStreamerForPermission(permission));

        var thrown = assertThrows(IllegalStateException.class,
                                  () -> manager.createNewStreamerForPermission(permission));

        assertThat(thrown.getMessage(), startsWith("An AiidaStreamer for permission "));
    }

    @Test
    void givenInvalidPermissionId_stopStreamer_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                manager.stopStreamer("InvalidPermissionId"));
    }

    @Test
    void givenValidPermissionId_stopStreamer_callsClose() {
        try (MockedStatic<StreamerFactory> mockStatic = mockStatic(StreamerFactory.class)) {
            // Given
            var mockStreamer = mock(AiidaStreamer.class);
            mockStatic.when(() -> StreamerFactory.getAiidaStreamer(any(), any(), any(), any(), any(), any()))
                      .thenReturn(mockStreamer);
            when(aggregatorMock.getFilteredFlux(any(), any())).thenReturn(Flux.empty());

            // need to create streamer before stopping
            manager.createNewStreamerForPermission(permission);

            // When
            manager.stopStreamer(permission.permissionId());

            // Then
            verify(mockStreamer).close();
        }
    }

    @Test
    void givenInvalidPermissionId_sendNewConnectionStatusMessageForPermission_throws() {
        var mockMessage = mock(ConnectionStatusMessage.class);

        assertThrows(IllegalArgumentException.class, () ->
                manager.sendConnectionStatusMessageForPermission(mockMessage, "InvalidId"));
    }

    @Test
    void givenConnectionStatusMessage_afterStreamerHasBeenClosed_sendNewConnectionStatusMessageForPermission_throws() {
        var acceptedMessageTimestamp = Instant.parse("2023-09-11T22:00:00.00Z");
        var acceptedMessage = new ConnectionStatusMessage(permission.connectionId(),
                                                          permission.dataNeedId(),
                                                          acceptedMessageTimestamp,
                                                          PermissionStatus.ACCEPTED,
                                                          permission.permissionId());

        try (MockedStatic<StreamerFactory> mockStatic = mockStatic(StreamerFactory.class)) {
            var streamerMock = mock(AiidaStreamer.class);
            mockStatic.when(() -> StreamerFactory.getAiidaStreamer(any(), any(), any(), any(), any(), any()))
                      .thenReturn(streamerMock);
            when(aggregatorMock.getFilteredFlux(any(), any())).thenReturn(Flux.empty());

            manager.createNewStreamerForPermission(permission);

            String permissionId = permission.permissionId();
            assertDoesNotThrow(() -> manager.sendConnectionStatusMessageForPermission(acceptedMessage, permissionId));

            manager.stopStreamer(permissionId);

            var thrown = assertThrows(ConnectionStatusMessageSendFailedException.class, () ->
                    manager.sendConnectionStatusMessageForPermission(acceptedMessage, permissionId));

            assertEquals("Cannot emit ConnectionStatusMessage after streamer has been stopped.", thrown.getMessage());
        }
    }

    @Test
    void verify_close_closesAllStreamers() {
        try (MockedStatic<StreamerFactory> mockStatic = mockStatic(StreamerFactory.class)) {
            var mockStreamer = mock(AiidaStreamer.class);
            mockStatic.when(() -> StreamerFactory.getAiidaStreamer(any(), any(), any(), any(), any(), any()))
                      .thenReturn(mockStreamer);
            when(aggregatorMock.getFilteredFlux(any(), any())).thenReturn(Flux.empty());

            manager.createNewStreamerForPermission(permission);

            manager.close();
            verify(mockStreamer).close();
        }
    }
}
