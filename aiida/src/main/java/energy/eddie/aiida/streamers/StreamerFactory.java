package energy.eddie.aiida.streamers;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.models.permission.KafkaStreamingConfig;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.streamers.kafka.KafkaFactory;
import energy.eddie.aiida.streamers.kafka.KafkaStreamer;
import reactor.core.publisher.Flux;

public class StreamerFactory {
    private StreamerFactory() {
    }

    /**
     * Creates a new AiidaStreamer applying the specified streamingConfig.
     * Currently only {@link energy.eddie.aiida.streamers.kafka.KafkaStreamer} is supported.
     * Further implementations differentiate which AiidaStreamer to create by checking the streamingConfig.
     *
     * @param streamingConfig Object holding necessary configuration for the AiidaStreamer.
     * @return A KafkaStreamer with the configuration applied and default values for the Kafka properties as specified in {@link KafkaFactory}.
     */
    protected static AiidaStreamer getAiidaStreamer(
            KafkaStreamingConfig streamingConfig,
            String connectionId,
            Flux<AiidaRecord> recordFlux,
            Flux<ConnectionStatusMessage> statusMessageFlux,
            ObjectMapper mapper) {
        var producer = KafkaFactory.getKafkaProducer(streamingConfig, connectionId);

        return new KafkaStreamer(producer, recordFlux, statusMessageFlux, connectionId, streamingConfig, mapper);
    }
}
