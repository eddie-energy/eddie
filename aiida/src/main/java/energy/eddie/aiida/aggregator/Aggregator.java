package energy.eddie.aiida.aggregator;

import energy.eddie.aiida.datasources.AiidaDataSource;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.repositories.AiidaRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class Aggregator implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Aggregator.class);
    private final List<AiidaDataSource> sources;
    private final Sinks.Many<AiidaRecord> combinedSink;
    private final AiidaRecordRepository repository;
    private final HealthContributorRegistry healthContributorRegistry;

    public Aggregator(AiidaRecordRepository repository, HealthContributorRegistry healthContributorRegistry) {
        this.repository = repository;
        this.healthContributorRegistry = healthContributorRegistry;

        sources = new ArrayList<>();
        combinedSink = Sinks.many().multicast().directAllOrNothing();

        combinedSink.asFlux()
                    .publishOn(Schedulers.boundedElastic())
                    .doOnNext(this::saveRecordToDatabase)
                    .doOnError(this::handleCombinedSinkError)
                    .subscribe();
    }

    private void saveRecordToDatabase(AiidaRecord aiidaRecord) {
        LOGGER.trace("Saving new record to db");
        repository.save(aiidaRecord);
    }

    private void handleCombinedSinkError(Throwable throwable) {
        LOGGER.error("Got error from combined sink", throwable);
    }

    /**
     * Adds a new {@link AiidaDataSource} to this aggregator and will subscribe to the Flux returned by
     * {@link AiidaDataSource#start()}.
     *
     * @param dataSource The new datasource to add. No check is made if this is a duplicate.
     */
    public void addNewAiidaDataSource(AiidaDataSource dataSource) {
        LOGGER.info("Will add datasource {} with ID {} to aggregator", dataSource.name(), dataSource.id());

        healthContributorRegistry.registerContributor(dataSource.id() + "_" + dataSource.name(), dataSource);
        sources.add(dataSource);
        dataSource.start()
                  .subscribe(this::publishRecordToCombinedFlux, throwable -> handleError(throwable, dataSource));
    }

    private void publishRecordToCombinedFlux(AiidaRecord data) {
        var result = combinedSink.tryEmitNext(data);

        if (result.isFailure())
            LOGGER.error("Error while emitting record to combined sink. Error was: {}", result);
    }

    private void handleError(Throwable throwable, AiidaDataSource dataSource) {
        // TODO: do we try to restart the affected datasource or only notify user?
        LOGGER.error("Error from datasource {}", dataSource.name(), throwable);
    }

    /**
     * Returns a filtered Flux of {@link AiidaRecord}s that only contains records with a {@link AiidaRecord#code()} that
     * is in the set {@code allowedCodes} and that have a timestamp before {@code permissionExpirationTime}.
     *
     * @param allowedCodes             Codes which should be included in the returned Flux.
     * @param permissionExpirationTime Instant when the permission expires.
     * @return A Flux on which will only have AiidaRecords with a code matching one of the codes of the
     * {@code allowedCodes} set and a timestamp that is before {@code permissionExpirationTime}.
     */
    public Flux<AiidaRecord> getFilteredFlux(Set<String> allowedCodes, Instant permissionExpirationTime) {
        return combinedSink.asFlux().filter(aiidaRecord -> allowedCodes.contains(aiidaRecord.code())
                                                           && aiidaRecord.timestamp()
                                                                         .isBefore(permissionExpirationTime));
    }

    /**
     * Closes all {@link AiidaDataSource}s and emits a complete signal for all the Flux returned by
     * {@link #getFilteredFlux(Set, Instant)}.
     */
    @Override
    public void close() {
        // ignore if complete signal can't be emitted successfully
        combinedSink.tryEmitComplete();

        LOGGER.info("Closing all {} datasources", sources.size());
        for (AiidaDataSource source : sources) {
            source.close();
        }
    }
}
