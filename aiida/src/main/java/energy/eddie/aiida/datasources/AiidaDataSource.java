package energy.eddie.aiida.datasources;

import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValidator;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public abstract class AiidaDataSource implements AutoCloseable, HealthIndicator {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiidaDataSource.class);
    protected final Sinks.Many<AiidaRecord> recordSink;
    protected final Sinks.Many<Health> healthSink;
    private final UUID id;
    private final UUID userId;
    private final String name;

    /**
     * Creates a new {@code AiidaDataSource} with the specified display name.
     *
     * @param id     The unique identifier (UUID) of this data source.
     * @param userId The ID of the user who owns this data source.
     * @param name   Display name of this new datasource.
     */
    protected AiidaDataSource(UUID id, UUID userId, String name) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        recordSink = Sinks.many().unicast().onBackpressureBuffer();
        healthSink = Sinks.many().unicast().onBackpressureBuffer();
    }

    /**
     * Starts this datasource and all records will be published on the returned Flux.
     *
     * @return Flux on which all data from this datasource will be published.
     */
    public abstract Flux<AiidaRecord> start();

    /**
     * Emits a new {@code AiidaRecord} with specified aiida record values
     *
     * @param aiidaRecordValues Values for the new aiida record
     */
    public synchronized void emitAiidaRecord(String asset, List<AiidaRecordValue> aiidaRecordValues) {
        Instant timestamp = Instant.now();

        var aiidaRecord = new AiidaRecord(timestamp, asset, userId, id, aiidaRecordValues);
        var invalidTags = AiidaRecordValidator.checkInvalidDataTags(aiidaRecord);

        if (!invalidTags.isEmpty()) {
            LOGGER.debug("Found unknown OBIS-CODES from {}: {}", asset, invalidTags);
        }

        var result = recordSink.tryEmitNext(aiidaRecord);

        if (result.isFailure()) {
            LOGGER.warn("Error while emitting new AiidaRecord {}. Error was {}", aiidaRecord, result);
        }
    }

    /**
     * Closes any open connections and frees resources used by this datasource.
     * Also emits a complete signal on the Flux returned by {@link #start()}.
     */
    @Override
    public abstract void close();

    /**
     * Returns the display name for this datasource.
     *
     * @return Display name of this datasource.
     */
    public String name() {
        return name;
    }

    /**
     * Returns the internal ID of the datasource
     *
     * @return Internal ID of the datasource
     */
    public UUID id() {
        return id;
    }
}
