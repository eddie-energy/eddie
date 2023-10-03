package energy.eddie.aiida.streamers.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.models.permission.KafkaStreamingConfig;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.streamers.AiidaStreamer;
import energy.eddie.aiida.streamers.ConnectionStatusMessage;
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
    private Disposable recordSubscriptionDisposable;
    @Nullable
    private Disposable statusMessageSubscriptionDisposable;

    /**
     * Creates a new streamer that uses the passed {@link KafkaProducer} to send any new record or status message
     * received via the corresponding Flux to the corresponding topics defined by {@code kafkaConfig}.
     * Make sure that the properties for the {@code producer} are set correctly, especially the
     * bootstrap servers need to be set to the value of {@link KafkaStreamingConfig#bootstrapServers()}.
     * Calling {@link #connect()} will start the subscriptions to the Flux and subsequently the sending of records and status messages.
     * Note that {@link KafkaProducer} doesn't provide a method to initiate the connection, but after instantiation,
     * the producer will try to connect endlessly.
     * Furthermore, as KafkaProducer does not provide a mechanism to check whether a connection has been successfully
     * initiated (except for WARN level log outputs in the opposite case), after a call to {@link #connect()},
     * any new data from either Flux is passed to the producer for sending.
     * If the messages cannot be sent, eventually the producer's internal buffer will be filled up and the producer
     * will throw an Exception that is logged by this class.
     *
     * @param producer          Preconfigured {@link KafkaProducer} used to produce the messages.
     * @param recordFlux        Flux on which the records that should be sent are available.
     * @param statusMessageFlux Flux on which status messages that should be sent are available.
     * @param connectionId      ID that will be sent along with each message and also used as key for messages.
     * @param kafkaConfig       Configuration object with necessary information about the Kafka cluster to connect to.
     * @param mapper            {@link ObjectMapper} that should be used to convert the records to JSON.
     */
    public KafkaStreamer(
            Producer<String, String> producer,
            Flux<AiidaRecord> recordFlux,
            Flux<ConnectionStatusMessage> statusMessageFlux,
            String connectionId,
            KafkaStreamingConfig kafkaConfig,
            ObjectMapper mapper) {
        super(recordFlux, statusMessageFlux);

        this.producer = producer;
        this.connectionId = connectionId;
        this.kafkaConfig = kafkaConfig;
        this.mapper = mapper;
    }

    /**
     * Subscribes to both Flux and starts sending new records or status messages to the Kafka cluster.
     */
    @Override
    public void connect() {
        LOGGER.info("Starting subscription to record and statusMessage Flux for connectionId {}", connectionId);

        recordSubscriptionDisposable = recordFlux.subscribe(this::produceAiidaRecord);

        statusMessageSubscriptionDisposable = statusMessageFlux.subscribe(this::produceStatusMessage);
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
        produceRecord(kafkaConfig.dataTopic(), aiidaRecord);
    }

    /**
     * Sends the passed {@code statusMessage} to the <i>statusTopic</i> of the streaming target (EP's EDDIE framework).
     * The message is converted to its JSON representation before sending.
     * Any occurring errors are only logged, no retry mechanism or persistence of failed sends is kept.
     *
     * @param statusMessage {@link ConnectionStatusMessage} to send
     */
    private void produceStatusMessage(ConnectionStatusMessage statusMessage) {
        LOGGER.info("Sending new ConnectionStatusMessage: {}", statusMessage);

        produceRecord(kafkaConfig.statusTopic(), statusMessage);
    }

    /**
     * Helper method that converts {@code data} to its JSON representation and then sends a ProducerRecord to {@code topic}.
     * Uses {@code connectionId} as key for the ProducerRecord.
     * Any occurring errors are only logged, no retry mechanism or persistence of failed sends is kept.
     *
     * @param topic To which topic the data should be produced.
     * @param data  Data that should be sent.
     */
    private void produceRecord(String topic, Object data) {
        String json;
        try {
            json = mapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            LOGGER.warn("Error while converting data {} to JSON", data, e);
            return;
        }

        try {
            ProducerRecord<String, String> kafkaRecord = new ProducerRecord<>(topic, connectionId, json);

            // FutureReturnValueIgnored because callback is used
            producer.send(kafkaRecord, (metadata, exception) -> {
                // callback is executed in IO thread of producer, so delegate expensive work to other threads
                if (exception == null)
                    LOGGER.info("Successfully produced data {} to topic {}, metadata: {}", data, topic, metadata);
                else
                    LOGGER.error("Failed to send data {}", data, exception);
            });
        } catch (IllegalStateException | KafkaException e) {
            // duplicate error handling as the .send() call can already throw an exception, but in the callback,
            // an exception may also be present
            LOGGER.error("Error while sending data {}", data, e);
        }
    }

    /**
     * Will flush any buffered messages and then stop streaming data via Kafka, but blocks indefinitely until
     * all queued send request complete.
     * Also unsubscribes from any Flux.
     */
    @Override
    public void close() {
        LOGGER.info("Will close KafkaStreamer for connectionId {}", connectionId);

        try {
            if (recordSubscriptionDisposable != null)
                recordSubscriptionDisposable.dispose();

            if (statusMessageSubscriptionDisposable != null)
                statusMessageSubscriptionDisposable.dispose();

            // don't flush beforehand, as flush may block for a long time but still accept new send requests which we want to avoid
            producer.close();
            LOGGER.info("KafkaStreamer for connectionId {} successfully shutdown", connectionId);
        } catch (KafkaException e) {
            LOGGER.error("Error while shutting down KafkaStreamer for connectionId {}", connectionId, e);
        }
    }
}
