package energy.eddie.aiida.streamers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.aiida.errors.ConnectionStatusMessageSendFailedException;
import energy.eddie.aiida.models.permission.KafkaStreamingConfig;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import energy.eddie.aiida.streamers.kafka.KafkaFactory;
import energy.eddie.aiida.streamers.kafka.KafkaStreamer;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StreamerManagerTest {
    private Permission permission;
    private StreamerManager manager;
    private String connectionId;

    @BeforeEach
    void setUp() {
        manager = new StreamerManager(new ObjectMapper().registerModule(new JavaTimeModule()));

        var permissionId = "72831e2c-a01c-41b8-9db6-3f51670df7a5";
        var grant = Instant.parse("2023-08-01T10:00:00.00Z");
        var start = grant.plusSeconds(100_000);
        var expiration = start.plusSeconds(800_000);
        var serviceName = "My NewAIIDA Test Service";
        connectionId = "NewAiidaRandomConnectionId";

        var codes = Set.of("1.8.0", "2.8.0");
        var bootstrapServers = "localhost:9092";
        var validDataTopic = "ValidPublishTopic";
        var validStatusTopic = "ValidStatusTopic";
        var validSubscribeTopic = "ValidSubscribeTopic";

        var streamingConfig = new KafkaStreamingConfig(bootstrapServers, validDataTopic, validStatusTopic, validSubscribeTopic);
        permission = new Permission(serviceName, start, expiration, grant, connectionId, codes, streamingConfig);
        // set permissionId via reflections to mimic database
        ReflectionTestUtils.setField(permission, "permissionId", permissionId);
    }

    @Test
    void verify_createNewStreamerForPermission_createsKafkaStreamerAndCallsConnect() {
        try (MockedStatic<StreamerFactory> mockStatic = mockStatic(StreamerFactory.class)) {
            var streamerMock = mock(KafkaStreamer.class);
            mockStatic.when(() -> StreamerFactory.getAiidaStreamer(any(), anyString(), any(), any(), any())).thenReturn(streamerMock);

            manager.createNewStreamerForPermission(permission);

            mockStatic.verify(() -> StreamerFactory.getAiidaStreamer(any(), anyString(), any(), any(), any()));
            verify(streamerMock).connect();
        }
    }

    @Test
    void givenSamePermissionTwice_createNewStreamerForPermission_throws() {
        // first time should result in valid creation
        assertDoesNotThrow(() -> manager.createNewStreamerForPermission(permission));

        var thrown = assertThrows(IllegalStateException.class, () -> manager.createNewStreamerForPermission(permission));

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
            var mockStreamer = mock(KafkaStreamer.class);
            mockStatic.when(() -> StreamerFactory.getAiidaStreamer(any(), eq(connectionId), any(), any(), any())).thenReturn(mockStreamer);

            // need to create streamer before stopping
            manager.createNewStreamerForPermission(permission);

            manager.stopStreamer(permission.permissionId());
            verify(mockStreamer).close();
        }
    }

    @Test
    void givenInvalidPermissionId_sendNewConnectionStatusMessageForPermission_throws() {
        var mockMessage = mock(ConnectionStatusMessage.class);

        assertThrows(IllegalArgumentException.class, () ->
                manager.sendConnectionStatusMessageForPermission(mockMessage, "InvalidId"));
    }

    /**
     * Tests that a ConnectionStatusMessage is sent via Kafka by using a Kafka MockProducer and checking if the
     * Mock Producer received the correct JSON which should be sent to the cluster.
     */
    @Test
    void givenValidPermissionId_sendNewConnectionStatusMessageForPermission_callsSendMessage() throws ConnectionStatusMessageSendFailedException {
        var acceptedMessageTimestamp = Instant.parse("2023-09-11T22:00:00.00Z");
        var acceptedMessage = new ConnectionStatusMessage(permission.connectionId(), acceptedMessageTimestamp, PermissionStatus.ACCEPTED);
        String acceptedMessageJson = "{\"connectionId\":\"NewAiidaRandomConnectionId\",\"timestamp\":1694469600.000000000,\"status\":\"ACCEPTED\"}";

        try (MockedStatic<KafkaFactory> mockKafkaFactory = mockStatic(KafkaFactory.class)) {
            var mockProducer = new MockProducer<>(false, new StringSerializer(), new StringSerializer());
            mockKafkaFactory.when(() -> KafkaFactory.getKafkaProducer(any(), anyString())).thenReturn(mockProducer);

            manager.createNewStreamerForPermission(permission);

            manager.sendConnectionStatusMessageForPermission(acceptedMessage, permission.permissionId());

            assertEquals(1, mockProducer.history().size());

            assertEquals(acceptedMessageJson, mockProducer.history().get(0).value());

            // clean up
            manager.stopStreamer(permission.permissionId());
        }
    }

    @Test
    void givenConnectionStatusMessage_afterStreamerHasBeenClosed_sendNewConnectionStatusMessageForPermission_throws() {
        var acceptedMessageTimestamp = Instant.parse("2023-09-11T22:00:00.00Z");
        var acceptedMessage = new ConnectionStatusMessage(permission.connectionId(), acceptedMessageTimestamp, PermissionStatus.ACCEPTED);

        try (MockedStatic<StreamerFactory> mockStatic = mockStatic(StreamerFactory.class)) {
            var streamerMock = mock(KafkaStreamer.class);
            mockStatic.when(() -> StreamerFactory.getAiidaStreamer(any(), anyString(), any(), any(), any())).thenReturn(streamerMock);

            manager.createNewStreamerForPermission(permission);

            String permissionId = permission.permissionId();
            assertDoesNotThrow(() -> manager.sendConnectionStatusMessageForPermission(acceptedMessage, permissionId));

            manager.stopStreamer(permissionId);

            var thrown = assertThrows(ConnectionStatusMessageSendFailedException.class, () ->
                    manager.sendConnectionStatusMessageForPermission(acceptedMessage, permissionId));

            assertEquals("Cannot emit ConnectionStatusMessage after streamer has been stopped.", thrown.getMessage());
        }
    }
}