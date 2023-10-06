package energy.eddie.aiida.streamers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.aiida.models.permission.KafkaStreamingConfig;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.streamers.kafka.KafkaStreamer;
import org.junit.jupiter.api.Test;
import reactor.test.publisher.TestPublisher;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StreamerFactoryTest {
    @Test
    void givenKafkaConfig_getAiidaStreamer_returnsKafkaStreamerAsExpected() {
        var mapper = new ObjectMapper().registerModule(new JavaTimeModule());

        var connectionId = "MyRandomTestConnectionId";
        var bootstrapServers = "localhost:9092";
        var validDataTopic = "ValidPublishTopic";
        var validStatusTopic = "ValidStatusTopic";
        var validSubscribeTopic = "ValidSubscribeTopic";

        var streamingConfig = new KafkaStreamingConfig(bootstrapServers, validDataTopic, validStatusTopic, validSubscribeTopic);
        TestPublisher<AiidaRecord> recordPublisher = TestPublisher.create();
        TestPublisher<ConnectionStatusMessage> statusMessagePublisher = TestPublisher.create();

        AiidaStreamer streamer = StreamerFactory.getAiidaStreamer(streamingConfig, connectionId, recordPublisher.flux(),
                statusMessagePublisher.flux(), mapper);

        assertTrue(streamer instanceof KafkaStreamer);
    }
}
