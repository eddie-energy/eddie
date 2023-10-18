package energy.eddie.aiida.streamers;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.streamers.kafka.KafkaFactory;
import energy.eddie.aiida.streamers.kafka.KafkaStreamer;
import org.springframework.scheduling.TaskScheduler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;

public class StreamerFactory {
    private StreamerFactory() {
    }

    /**
     * Creates a new {@link AiidaStreamer} applying the specified streamingConfig.
     * Currently only {@link KafkaStreamer} is supported.
     * Further implementations differentiate which AiidaStreamer to create by checking the streamingConfig.
     *
     * @param permission                     Permission for which to create the AiidaStreamer.
     * @param recordFlux                     Flux on which the records that should be sent are published.
     * @param statusMessageFlux              Flux on which status messages that should be sent are published.
     * @param terminationRequestSink         Sink, to which the permissionId will be published, when the EP requests a termination.
     * @param mapper                         {@link ObjectMapper} that should be used to convert the records to JSON.
     * @param scheduler                      Scheduler to schedule the periodic polling for EP termination requests.
     * @param terminationRequestPollDuration Duration between the end of one poll interval and the start of the next. Needs to be at least 10 seconds.
     * @return A KafkaStreamer with the configuration applied and default values for the Kafka properties as specified in {@link KafkaFactory}.
     */
    protected static AiidaStreamer getAiidaStreamer(
            Permission permission,
            Flux<AiidaRecord> recordFlux,
            Flux<ConnectionStatusMessage> statusMessageFlux,
            Sinks.One<String> terminationRequestSink,
            ObjectMapper mapper,
            TaskScheduler scheduler,
            Duration terminationRequestPollDuration) {
        var producer = KafkaFactory.getKafkaProducer(permission.kafkaStreamingConfig(), permission.connectionId());
        var consumer = KafkaFactory.getKafkaConsumer(permission.kafkaStreamingConfig(), permission.connectionId());

        return new KafkaStreamer(
                producer,
                consumer,
                recordFlux,
                statusMessageFlux,
                terminationRequestSink,
                permission,
                mapper,
                scheduler,
                terminationRequestPollDuration);
    }
}
