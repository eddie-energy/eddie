package energy.eddie.aiida.streamers;

import com.fasterxml.jackson.databind.ObjectMapper;
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
public class StreamerManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamerManager.class);
    private final ObjectMapper mapper;
    private final Map<String, StreamerSinkContainer> streamers;

    /**
     * The mapper is passed to the {@link AiidaStreamer} instances that which use it to convert POJOs to JSON.
     * As the mapper is shared, make the used implementation is thread-safe and supports sharing.
     */
    @Autowired
    public StreamerManager(ObjectMapper mapper) {
        this.mapper = mapper;

        streamers = new HashMap<>();
    }

    /**
     * Creates a new {@link AiidaStreamer} for the specified permission and stores it internally to enable calls to
     * other methods like {@link #sendNewConnectionStatusMessageForPermission}.
     *
     * @param permission Permission for which an AiidaStreamer should be created.
     * @throws IllegalArgumentException If an AiidaStreamer for the passed permission has already been created.
     */
    public void createNewStreamerForPermission(Permission permission) throws IllegalArgumentException {
        LOGGER.info("Will create a new KafkaStreamer for permission {}", permission.permissionId());

        if (streamers.get(permission.permissionId()) != null)
            throw new IllegalStateException("An AiidaStreamer for permission %s has already been created.".formatted(permission.permissionId()));

        Sinks.Many<ConnectionStatusMessage> statusMessageSink = Sinks.many().unicast().onBackpressureBuffer();
        // TODO get correct flux from aggregator
        Flux<AiidaRecord> recordFlux = Flux.empty();

        var streamer = StreamerFactory.getAiidaStreamer(permission.kafkaStreamingConfig(), permission.connectionId(),
                recordFlux, statusMessageSink.asFlux(), mapper);
        streamer.connect();

        StreamerSinkContainer container = new StreamerSinkContainer(streamer, statusMessageSink);

        streamers.put(permission.permissionId(), container);
    }

    /**
     * Wrapper class for the {@link AiidaStreamer} instance and its associated {@link Sinks.Many} for {@link ConnectionStatusMessage}.
     * Stops streaming for the specified permission.
     * Will also complete the sink for the {@link ConnectionStatusMessage}.
     *
     * @param permissionId ID of permission for which to stop sharing
     * @throws IllegalArgumentException If there is no streamer for the permissionId
     */
    public void stopStreamer(String permissionId) throws IllegalArgumentException {
        var container = getContainer(permissionId);

        container.streamer.close();

        var result = container.statusMessageSink.tryEmitComplete();
        if (result.isFailure())
            LOGGER.warn("Failed to emit complete signal for ConnectionStatusMessage sink of permission {}. Error was: {}", permissionId, result);
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
     * Wrapper class for the {@link AiidaStreamer} instance and its associated {@link Sinks.Many} for {@link ConnectionStatusMessage}.
     * Should be used with a Map that uses the permissionId as key.
     *
     * @param streamer          AiidaStreamer reference.
     * @param statusMessageSink Sink on which status messages should be sent.
     */
    record StreamerSinkContainer(
            AiidaStreamer streamer,
            Sinks.Many<ConnectionStatusMessage> statusMessageSink
    ) {
    }
}
