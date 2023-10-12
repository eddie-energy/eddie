package energy.eddie.aiida.streamers.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.models.permission.KafkaStreamingConfig;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.streamers.AiidaStreamer;
import energy.eddie.aiida.streamers.ConnectionStatusMessage;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

public class KafkaStreamer extends AiidaStreamer {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaStreamer.class);
    private final ObjectMapper mapper;
    private final Producer<String, String> producer;
    private final Consumer<String, String> consumer;
    private final TaskScheduler scheduler;
    private final Duration terminationRequestPollDuration;
    private final Permission permission;
    @Nullable
    private Disposable recordSubscriptionDisposable;
    @Nullable
    private Disposable statusMessageSubscriptionDisposable;
    private final Runnable terminationRequestPollRunnable;
    @Nullable
    private ScheduledFuture<?> pollFuture;
    private volatile boolean receivedTerminationRequest;

    /**
     * Creates a new streamer that uses the passed {@link KafkaProducer} to send any new record or status message
     * received via the corresponding Flux to the corresponding topics defined by {@code kafkaConfig}.
     * Make sure that the properties for the {@code producer} are set correctly, especially the
     * bootstrap servers need to be set to the value of {@link KafkaStreamingConfig#bootstrapServers()}.
     * <p>
     * Calling {@link #connect()} will start the subscriptions to the Flux and subsequently the sending of records
     * and status messages, as well as starting the listening for termination requests by the EP.
     * The Kafka broker is polled in {@code terminationRequestPollDuration} intervals,
     * whereas one poll may take up to five seconds.
     * </p>
     * Note that {@link KafkaProducer} doesn't provide a method to initiate the connection, but after instantiation,
     * the producer or consumer will already try to connect endlessly.
     * Furthermore, as KafkaProducer does not provide a mechanism to check whether a connection has been successfully
     * initiated (except for WARN level log outputs in the opposite case), after a call to {@link #connect()},
     * any new data from either Flux is passed to the producer for sending.
     * If the messages cannot be sent, eventually the producer's internal buffer will be filled up and the producer
     * will throw an Exception that is logged by this class.
     *
     * @param producer                       Preconfigured {@link KafkaProducer} used to produce the messages.
     * @param consumer                       Preconfigured {@link KafkaConsumer} used to subscribe to the {@code subscriptionTopic} and listen for termination requests.
     * @param recordFlux                     Flux, where records that should be sent are available.
     * @param statusMessageFlux              Flux, where status messages that should be sent are available.
     * @param terminationRequestSink         Sink, to which the permissionId will be published, when the EP requests a termination.
     * @param permission                     Permission to which this KafkaStreamer belongs to.
     * @param mapper                         {@link ObjectMapper} that should be used to convert the records to JSON.
     * @param scheduler                      Scheduler to schedule the periodic polling for EP termination requests.
     * @param terminationRequestPollDuration Duration between the end of one poll interval and the start of the next. Needs to be at least 10 seconds.
     */
    public KafkaStreamer(
            Producer<String, String> producer,
            Consumer<String, String> consumer,
            Flux<AiidaRecord> recordFlux,
            Flux<ConnectionStatusMessage> statusMessageFlux,
            Sinks.One<String> terminationRequestSink,
            Permission permission,
            ObjectMapper mapper,
            TaskScheduler scheduler,
            Duration terminationRequestPollDuration) {
        super(recordFlux, statusMessageFlux, terminationRequestSink);

        if (terminationRequestPollDuration.toSeconds() < 10)
            throw new IllegalArgumentException("terminationRequestPollDuration must be greater or equal to 10 seconds");

        this.producer = producer;
        this.consumer = consumer;
        this.permission = permission;
        this.mapper = mapper;
        this.scheduler = scheduler;
        this.terminationRequestPollDuration = terminationRequestPollDuration;

        this.terminationRequestPollRunnable = () -> {
            try {
                ConsumerRecords<String, String> polled = consumer.poll(Duration.ofSeconds(1));

                consumer.commitAsync();
                // only react to the first message received on the topic
                if (polled.count() > 0)
                    receivedTerminationRequest(polled.iterator().next().value());
            } catch (KafkaException ex) {
                LOGGER.error("Error while polling termination request for permission {}", permission.permissionId(), ex);
            }
        };
    }

    /**
     * Subscribes to both Flux and starts sending new records or status messages to the Kafka cluster.
     */
    @Override
    public void connect() {
        LOGGER.info("Starting subscription to record and statusMessage Flux for permission {}", permission.permissionId());

        recordSubscriptionDisposable = recordFlux.subscribe(this::produceAiidaRecord);
        statusMessageSubscriptionDisposable = statusMessageFlux.subscribe(this::produceStatusMessage);

        consumer.subscribe(List.of(permission.kafkaStreamingConfig().subscribeTopic()));

        pollFuture = scheduler.scheduleAtFixedRate(terminationRequestPollRunnable, terminationRequestPollDuration);
    }

    /**
     * Converts the passed {@code aiidaRecord} to JSON and produces it to the {@code dataTopic} specified by
     * the {@code kafkaConfig}.
     * Any errors that occur while sending are logged, but the aiidaRecord is not marked as <i>failed to send</i> but just dropped.
     *
     * @param aiidaRecord {@link AiidaRecord} to send to the Kafka cluster.
     */
    private void produceAiidaRecord(AiidaRecord aiidaRecord) {
        if (receivedTerminationRequest) {
            LOGGER.debug("Got new aiidaRecord but won't send it as a termination request has been received.");
            return;
        }
        LOGGER.debug("Sending new aiidaRecord: {}", aiidaRecord);
        produceRecord(permission.kafkaStreamingConfig().dataTopic(), aiidaRecord);
    }

    /**
     * Sends the passed {@code statusMessage} to the <i>statusTopic</i> of the streaming target (EP's EDDIE framework).
     * The message is converted to its JSON representation before sending.
     * Any occurring errors are only logged, no retry mechanism or persistence of failed sends is kept.
     *
     * @param statusMessage {@link ConnectionStatusMessage} to send.
     */
    private void produceStatusMessage(ConnectionStatusMessage statusMessage) {
        LOGGER.info("Sending new ConnectionStatusMessage: {}", statusMessage);

        produceRecord(permission.kafkaStreamingConfig().statusTopic(), statusMessage);
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
            ProducerRecord<String, String> kafkaRecord = new ProducerRecord<>(topic, permission.connectionId(), json);

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

    private void receivedTerminationRequest(String connectionId) {
        if (!permission.connectionId().equals(connectionId)) {
            LOGGER.warn("Got request from EP to terminate permission {} but they supplied wrong connectionId. Expected {}, but got {}",
                    permission.permissionId(), permission.connectionId(), connectionId);
            return;
        }

        receivedTerminationRequest = true;

        LOGGER.info("EP requested termination of permission {}", permission.permissionId());

        if (pollFuture != null)
            pollFuture.cancel(false);

        var result = terminationRequestSink.tryEmitValue(permission.permissionId());

        if (result.isFailure())
            LOGGER.error("Failed to emit permissionId {} to terminationRequestSink. Error was {}", permission.permissionId(), result);
    }

    /**
     * Will flush any buffered messages and then stop streaming data via Kafka, but blocks indefinitely until
     * all queued send request complete.
     * Also unsubscribes from any Flux.
     */
    @Override
    public void close() {
        LOGGER.info("Will close KafkaStreamer for permission {}", permission.permissionId());

        try {
            if (recordSubscriptionDisposable != null)
                recordSubscriptionDisposable.dispose();

            if (statusMessageSubscriptionDisposable != null)
                statusMessageSubscriptionDisposable.dispose();

            if (pollFuture != null)
                pollFuture.cancel(true);

            if (!receivedTerminationRequest)
                terminationRequestSink.tryEmitEmpty();

            // don't flush before calling close(), as flush may block for a long time and KafkaProducer still accepts new send requests which we want to avoid
            producer.close();
            LOGGER.info("KafkaStreamer for permission {} successfully shutdown", permission.permissionId());
        } catch (KafkaException e) {
            LOGGER.error("Error while shutting down KafkaStreamer for permission {}", permission.permissionId(), e);
        }
    }
}
