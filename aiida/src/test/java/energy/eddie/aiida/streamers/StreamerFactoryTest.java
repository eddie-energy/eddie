package energy.eddie.aiida.streamers;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.models.permission.KafkaStreamingConfig;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.streamers.kafka.KafkaStreamer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;
import reactor.core.publisher.Sinks;
import reactor.test.publisher.TestPublisher;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class StreamerFactoryTest {
    // use annotation instead of mock(...) method to get typed mock
    @Mock
    Sinks.One<String> mockSink;

    @Test
    void givenKafkaConfig_getAiidaStreamer_returnsKafkaStreamerAsExpected() {
        var start = Instant.now();
        var permission = getTestPermission(start);

        TestPublisher<AiidaRecord> recordPublisher = TestPublisher.create();
        TestPublisher<ConnectionStatusMessage> statusMessagePublisher = TestPublisher.create();

        AiidaStreamer streamer = StreamerFactory.getAiidaStreamer(
                permission,
                recordPublisher.flux(),
                statusMessagePublisher.flux(),
                mockSink,
                mock(ObjectMapper.class),
                mock(TaskScheduler.class),
                Duration.ofSeconds(10));

        assertTrue(streamer instanceof KafkaStreamer);
    }

    private Permission getTestPermission(Instant start) {
        var expiration = start.plusSeconds(1000);
        var connectionId = "MyRandomTestConnectionId";
        var bootstrapServers = "localhost:9092";
        var validDataTopic = "ValidPublishTopic";
        var validStatusTopic = "ValidStatusTopic";
        var validSubscribeTopic = "ValidSubscribeTopic";
        var streamingConfig = new KafkaStreamingConfig(bootstrapServers, validDataTopic, validStatusTopic, validSubscribeTopic);
        return new Permission("SomeServiceName", start, expiration, start, connectionId, Set.of("1.8.0"), streamingConfig);
    }
}
