package energy.eddie.aiida.streamers;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.aggregator.Aggregator;
import energy.eddie.aiida.dtos.ConnectionStatusMessage;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.repositories.FailedToSendRepository;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * The StreamerManager manages the lifecycle of {@link AiidaStreamer}. Other components should rely on the
 * StreamerManager for creating or stopping streamers.
 */
@Component
public class StreamerManager implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamerManager.class);
    private final ObjectMapper mapper;
    private final Map<UUID, AiidaStreamer> streamers;
    private final Aggregator aggregator;
    private final Sinks.Many<UUID> terminationRequests;
    private final FailedToSendRepository failedToSendRepository;

    /**
     * The mapper is passed to the {@link AiidaStreamer} instances that which use it to convert POJOs to JSON. As the
     * mapper is shared, make sure the used implementation is thread-safe and supports sharing.
     */
    @Autowired
    public StreamerManager(ObjectMapper mapper, Aggregator aggregator, FailedToSendRepository failedToSendRepository) {
        this.mapper = mapper;
        this.aggregator = aggregator;
        this.failedToSendRepository = failedToSendRepository;

        streamers = new HashMap<>();
        terminationRequests = Sinks.many().unicast().onBackpressureBuffer();
    }

    /**
     * Creates a new {@link AiidaStreamer} for the specified permission and stores it internally. The created streamer
     * will receive any matching {@link AiidaRecord} as requested by the data need of the permission.
     *
     * @param permission Permission for which an AiidaStreamer should be created.
     * @throws IllegalArgumentException If an AiidaStreamer for the passed permission has already been created.
     */
    public void createNewStreamer(Permission permission) throws IllegalArgumentException, MqttException {
        LOGGER.info("Will create a new AiidaStreamer for permission {}", permission.permissionId());
        var id = permission.permissionId();

        if (streamers.get(id) != null) {
            throw new IllegalStateException(
                    "An AiidaStreamer for EDDIE framework '%s' with permission '%s' has already been created.".formatted(
                            permission.eddieId(),
                            permission.permissionId()));
        }

        var dataNeed = Objects.requireNonNull(permission.dataNeed());
        var allowedDataTags = Objects.requireNonNull(dataNeed.dataTags());
        var transmissionSchedule = Objects.requireNonNull(dataNeed.transmissionSchedule());
        var permissionExpirationTime = Objects.requireNonNull(permission.expirationTime());
        var userId = Objects.requireNonNull(permission.userId());

        Flux<AiidaRecord> recordFlux = aggregator.getFilteredFlux(allowedDataTags,
                                                                  permissionExpirationTime,
                                                                  transmissionSchedule,
                                                                  userId);
        Sinks.One<UUID> streamerTerminationRequestSink = Sinks.one();

        streamerTerminationRequestSink.asMono().subscribe(permissionId -> {
            var result = terminationRequests.tryEmitNext(permissionId);
            if (result.isFailure()) LOGGER.error(
                    "Error while emitting termination request for permission {}. Error was: {}",
                    permissionId,
                    result);
        });

        var streamer = StreamerFactory.getAiidaStreamer(permission,
                                                        recordFlux,
                                                        streamerTerminationRequestSink,
                                                        mapper,
                                                        failedToSendRepository);
        streamer.connect();
        streamers.put(id, streamer);
    }

    /**
     * Returns a Flux on which the ID of a permission is published, when the EP requests a termination for this
     * permission. The Flux allows only one subscriber and buffers, ensuring no values get lost.
     *
     * @return Flux of permissionIDs for which the EP requested termination.
     */
    public Flux<UUID> terminationRequestsFlux() {
        return terminationRequests.asFlux();
    }

    /**
     * Sends the passed status message in a blocking manner and then terminally stops the streamer.
     *
     * @param message Message to send before stopping the streamer. The status should be one of
     *                {@link PermissionStatus#TERMINATED}, {@link PermissionStatus#REVOKED} or
     *                {@link PermissionStatus#FULFILLED}.
     */
    public void stopStreamer(ConnectionStatusMessage message) {
        var id = message.permissionId();
        AiidaStreamer streamer = streamers.get(id);

        if (streamer == null)
            throw new IllegalArgumentException("No streamer for permissionId '%s' exists.".formatted(id));

        streamer.closeTerminally(message);
    }

    /**
     * Closes all streamer to allow for an orderly shutdown. Note that this blocks until all streamers have finished
     * closing, which may be indefinitely in the current implementation.
     */
    @Override
    public void close() {
        LOGGER.info("Closing all {} streamers", streamers.size());
        for (var entry : streamers.entrySet()) {
            entry.getValue().close();
        }

        terminationRequests.tryEmitComplete();
    }
}
