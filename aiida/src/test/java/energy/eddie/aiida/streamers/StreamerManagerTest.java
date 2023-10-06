package energy.eddie.aiida.streamers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.aiida.models.permission.KafkaStreamingConfig;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.streamers.kafka.KafkaStreamer;
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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StreamerManagerTest {
    private Permission permission;
    private StreamerManager manager;

    @BeforeEach
    void setUp() {
        manager = new StreamerManager(new ObjectMapper().registerModule(new JavaTimeModule()));

        var permissionId = "72831e2c-a01c-41b8-9db6-3f51670df7a5";
        var grant = Instant.parse("2023-08-01T10:00:00.00Z");
        var start = grant.plusSeconds(100_000);
        var expiration = start.plusSeconds(800_000);
        var serviceName = "My NewAIIDA Test Service";
        var connectionId = "NewAiidaRandomConnectionId";

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
    void createNewStreamerForPermission_createsKafkaStreamerAndCallsConnect() {
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
}