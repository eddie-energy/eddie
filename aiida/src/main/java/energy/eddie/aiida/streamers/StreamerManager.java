package energy.eddie.aiida.streamers;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.aggregator.Aggregator;
import energy.eddie.aiida.errors.ConnectionStatusMessageSendFailedException;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.record.AiidaRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.HashMap;
import java.util.Map;

/**
 * The StreamerManager manages the lifecycle of {@link AiidaStreamer}.
 * Other components should rely on the StreamerManager for creating or stopping streamers.
 */
@Component
public class StreamerManager implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamerManager.class);
    private final ObjectMapper mapper;
    private final Map<String, StreamerSinkContainer> streamers;
    private final Aggregator aggregator;
    private final Sinks.Many<String> terminationRequests;

    /**
     * The mapper is passed to the {@link AiidaStreamer} instances that which use it to convert POJOs to JSON.
     * As the mapper is shared, make sure the used implementation is thread-safe and supports sharing.
     */
    @Autowired
    public StreamerManager(ObjectMapper mapper, Aggregator aggregator) {
        this.mapper = mapper;
        this.aggregator = aggregator;

        streamers = new HashMap<>();
        terminationRequests = Sinks.many().unicast().onBackpressureBuffer();
    }

    /**
     * Creates a new {@link AiidaStreamer} for the specified permission and stores it internally to enable calls to
     * other methods like {@link #sendConnectionStatusMessageForPermission}.
     * The created streamer will receive any {@link AiidaRecord} that has a code that is in {@link Permission#requestedCodes()}.
     *
     * @param permission Permission for which an AiidaStreamer should be created.
     * @throws IllegalArgumentException If an AiidaStreamer for the passed permission has already been created.
     */
    public void createNewStreamerForPermission(Permission permission) throws IllegalArgumentException {
        LOGGER.info("Will create a new AiidaStreamer for permission {}", permission.permissionId());

        if (streamers.get(permission.permissionId()) != null)
            throw new IllegalStateException("An AiidaStreamer for permission %s has already been created.".formatted(permission.permissionId()));

        Sinks.Many<ConnectionStatusMessage> statusMessageSink = Sinks.many().unicast().onBackpressureBuffer();
        Flux<AiidaRecord> recordFlux = aggregator.getFilteredFlux(permission.requestedCodes(), permission.expirationTime());
        Sinks.One<String> streamerTerminationRequestSink = Sinks.one();

        streamerTerminationRequestSink.asMono().subscribe(permissionId -> {
            var result = terminationRequests.tryEmitNext(permissionId);
            if (result.isFailure())
                LOGGER.error("Error while emitting termination request for permission {}. Error was: {}", permissionId, result);
        });

        var streamer = StreamerFactory.getAiidaStreamer(permission, recordFlux, statusMessageSink.asFlux(),
                                                        streamerTerminationRequestSink, mapper);
        streamer.connect();

        StreamerSinkContainer container = new StreamerSinkContainer(streamer, statusMessageSink);

        streamers.put(permission.permissionId(), container);
    }

    /**
     * Returns a Flux on which the ID of a permission is published, when the EP requests a termination for this
     * permission.
     * The Flux allows only one subscriber and buffers, ensuring no values get lost.
     *
     * @return Flux of permissionIDs for which the EP requested termination.
     */
    public Flux<String> terminationRequestsFlux() {
        return terminationRequests.asFlux();
    }

    /**
     * Send the specified {@link ConnectionStatusMessage} to the EDDIE framework using the streaming protocol of
     * the permission identified by {@code permissionId}.
     *
     * @param message      ConnectionStatusMessage that should be sent
     * @param permissionId ID of the permission to which the message belongs
     * @throws IllegalArgumentException                   If there is no streamer for the specified permissionId.
     * @throws ConnectionStatusMessageSendFailedException If the AiidaStreamer cannot be notified of the new message to send.
     */
    public void sendConnectionStatusMessageForPermission(ConnectionStatusMessage message, String permissionId)
            throws IllegalArgumentException, ConnectionStatusMessageSendFailedException {
        var container = getContainer(permissionId);

        var result = container.statusMessageSink.tryEmitNext(message);

        if (result == Sinks.EmitResult.FAIL_TERMINATED)
            throw new ConnectionStatusMessageSendFailedException("Cannot emit ConnectionStatusMessage after streamer has been stopped.");

        if (result.isFailure())
            throw new ConnectionStatusMessageSendFailedException("Failed to emit complete signal for ConnectionStatusMessage sink of permission %s. Error was: %s"
                    .formatted(permissionId, result.toString()));
    }

    /**
     * Stops streaming for the specified permission.
     * Will also complete the sink for the {@link ConnectionStatusMessage}.
     *
     * @param permissionId ID of permission for which to stop sharing
     * @throws IllegalArgumentException If there is no streamer for the specified permissionId
     */
    public void stopStreamer(String permissionId) throws IllegalArgumentException {
        var container = getContainer(permissionId);

        var result = container.statusMessageSink.tryEmitComplete();
        if (result.isFailure())
            LOGGER.warn("Failed to emit complete signal for ConnectionStatusMessage sink of permission {}. Error was: {}", permissionId, result);

        container.streamer.close();
    }

    /**
     * Helper method that returns the container of the corresponding permissionId from the map or otherwise throws an exception.
     *
     * @param permissionId permissionId for which to return the container for
     * @return Container for the specified permissionId
     * @throws IllegalArgumentException If there is no container for the permissionId or if the container is null.
     */
    private StreamerSinkContainer getContainer(String permissionId) throws IllegalArgumentException {
        StreamerSinkContainer container = streamers.get(permissionId);

        if (container == null)
            throw new IllegalArgumentException("No streamer for permissionId %s exists.".formatted(permissionId));
        return container;
    }

    /**
     * Closes all streamer to allow for an orderly shutdown. Note that this blocks until all streamers have
     * finished closing, which may be indefinitely in the current implementation.
     */
    @Override
    public void close() {
        LOGGER.info("Closing all {} streamers", streamers.keySet().size());
        for (var entry : streamers.entrySet()) {
            entry.getValue().streamer.close();
        }
    }

    /**
     * Wrapper class for the {@link AiidaStreamer} instance and its associated {@link Sinks.Many} for {@link ConnectionStatusMessage}.
     * Should be used with a Map that uses the permissionId as key.
     *
     * @param streamer          AiidaStreamer reference.
     * @param statusMessageSink Sink on which status messages should be sent.
     */
    record StreamerSinkContainer(
            AiidaStreamer streamer,
            Sinks.Many<ConnectionStatusMessage> statusMessageSink
    ) {}
}
