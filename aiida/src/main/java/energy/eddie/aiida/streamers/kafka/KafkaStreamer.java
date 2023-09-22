package energy.eddie.aiida.streamers.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.models.permission.KafkaStreamingConfig;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.streamers.AiidaStreamer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import javax.annotation.Nullable;

public class KafkaStreamer extends AiidaStreamer {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaStreamer.class);
    private final KafkaStreamingConfig kafkaConfig;
    private final String connectionId;
    private final ObjectMapper mapper;
    private final Producer<String, String> producer;
    @Nullable
    private Disposable subscriptionDisposable;

    /**
     * Creates a new streamer that uses the passed {@link KafkaProducer} to send any new record received via the Flux
     * to the topic broker defined by {@link KafkaStreamingConfig#dataTopic()}.
     * Make sure that the properties for the {@code producer} are set correctly, especially the
     * bootstrap servers need to be set to the value of {@link KafkaStreamingConfig#bootstrapServers()}.
     * Calling {@link #connect()} will start the subscription to the Flux and subsequently the sending of records.
     * Note that {@link KafkaProducer} doesn't provide a method to initiate the connection, but after instantiation,
     * the producer will try to connect endlessly.
     * Furthermore, as KafkaProducer does not provide a mechanism to check whether a connection has been successfully
     * initiated (except for WARN level log outputs in the opposite case), after a call to {@link #connect()},
     * any new records from the {@code recordFlux} are passed to the producer for sending.
     * If the records cannot be sent, eventually the producer's internal buffer will be filled up and the producer
     * will throw an Exception that is logged by this class.
     *
     * @param producer     Preconfigured {@link KafkaProducer} used to produce the messages.
     * @param recordFlux   (Hot) flux on which the records that should be sent are available.
     * @param connectionId ID that will be sent along with each message and also used as key for messages.
     * @param kafkaConfig  Configuration object with necessary information about the Kafka cluster to connect to.
     * @param mapper       {@link ObjectMapper} that should be used to convert the records to JSON.
     */
    public KafkaStreamer(
            Producer<String, String> producer,
            Flux<AiidaRecord> recordFlux,
            String connectionId,
            KafkaStreamingConfig kafkaConfig,
            ObjectMapper mapper) {
        super(recordFlux);

        this.producer = producer;
        this.connectionId = connectionId;
        this.kafkaConfig = kafkaConfig;
        this.mapper = mapper;
    }

    /**
     * Subscribes to the {@code recordFlux} and starts sending records to the Kafka cluster.
     */
    @Override
    public void connect() {
        LOGGER.info("Starting subscription to AiidaRecord flux for connectionId {}", connectionId);

        subscriptionDisposable = recordFlux.log()
                .subscribe(this::produceAiidaRecord);
    }

    /**
     * Converts the passed {@code aiidaRecord} to JSON and produces it to the {@code dataTopic} specified by
     * the {@code kafkaConfig}.
     * Any errors that occur while sending are logged, but the aiidaRecord is not marked as <i>failed to send</i> but just dropped.
     *
     * @param aiidaRecord {@link AiidaRecord} to send to the Kafka cluster.
     */
    private void produceAiidaRecord(AiidaRecord aiidaRecord) {
        LOGGER.info("Sending new aiidaRecord: {}", aiidaRecord);

        String json;
        try {
            json = mapper.writeValueAsString(aiidaRecord);
        } catch (JsonProcessingException e) {
            LOGGER.warn("Error while converting aiidaRecord {} to JSON", aiidaRecord, e);
            return;
        }

        try {
            ProducerRecord<String, String> kafkaRecord = new ProducerRecord<>(kafkaConfig.dataTopic(), connectionId, json);

            // FutureReturnValueIgnored because callback is used
            producer.send(kafkaRecord, (metadata, exception) -> {
                // callback is executed in IO thread of producer, so delegate expensive work to other threads
                if (exception == null)
                    LOGGER.info("Successfully sent aiidaRecord {}, metadata: {}", aiidaRecord, metadata);
                else
                    LOGGER.error("Failed to send aiidaRecord {}", aiidaRecord, exception);
            });
        } catch (IllegalStateException | KafkaException e) {
            // duplicate error handling as the .send() call can already throw an exception, but in the callback,
            // an exception may also be present
            LOGGER.error("Error while sending aiidaRecord {}", aiidaRecord, e);
        }
    }

    /**
     * Will flush any buffered messages and then stop streaming data via Kafka, but blocks indefinitely until
     * all queued send request complete.
     * Also unsubscribes from the {@code recordFlux}.
     */
    @Override
    public void shutdown() {
        LOGGER.info("Will shutdown KafkaStreamer");

        try {
            if (subscriptionDisposable != null)
                subscriptionDisposable.dispose();

            // don't flush beforehand, as flush may block for a long time but still accept new send requests which we want to avoid
            producer.close();
            LOGGER.info("KafkaStreamer for connectionId {} successfully shutdown", connectionId);
        } catch (KafkaException e) {
            LOGGER.error("Error while shutting down KafkaStreamer for connectionId {}", connectionId, e);
        }
    }
}
