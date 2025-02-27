package energy.eddie.aiida.aggregator;

import energy.eddie.aiida.datasources.AiidaDataSource;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.aiida.repositories.AiidaRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    /**
     * Removes a {@link AiidaDataSource} from this aggregator and will close the datasource.
     *
     * @param dataSource The datasource to remove.
     */
    public void removeAiidaDataSource(AiidaDataSource dataSource) {
        LOGGER.info("Will remove datasource {} with ID {} from aggregator", dataSource.name(), dataSource.id());

        healthContributorRegistry.unregisterContributor(dataSource.id() + "_" + dataSource.name());
        sources.remove(dataSource);
        dataSource.close();
    }

    /**
     * Returns a Flux of {@link AiidaRecord}s that either contains all records or only contains records with a {@link AiidaRecordValue#dataTag()}
     * that is in the set {@code allowedCodes}.
     * All values must have a timestamp before {@code permissionExpirationTime}.
     * Additionally, the records are buffered and aggregated by the {@link CronExpression} {@code transmissionSchedule}.
     * @param allowedDataTags          Tags which should be included in the returned Flux.
     * @param permissionExpirationTime Instant when the permission expires.
     * @param transmissionSchedule     The schedule at which the data should be transmitted.
     * @param userId                   The user id of the permission creator.
     * @return A Flux on which will only have AiidaRecords with a code matching one of the codes of the
     * {@code allowedCodes} set, a timestamp that is before {@code permissionExpirationTime} and that are aggregated
     * by the {@code transmissionSchedule}.
     */
    public Flux<AiidaRecord> getFilteredFlux(
            Set<String> allowedDataTags,
            Instant permissionExpirationTime,
            CronExpression transmissionSchedule,
            UUID userId
    ) {
        var cronSink = Sinks.many().multicast().directAllOrNothing();
        var cronTrigger = new CronTrigger(transmissionSchedule.toString());
        var cronScheduler = new ThreadPoolTaskScheduler();

        cronScheduler.initialize();
        @SuppressWarnings("unused") // Otherwise errorprone shows a warning
        var unused = cronScheduler.schedule(() -> cronSink.tryEmitNext(true), cronTrigger);

        var flux = combinedSink.asFlux()
                               .doOnComplete(() -> {
                                   cronScheduler.stop();
                                   cronSink.tryEmitComplete();
                               });

        return flux.map(AiidaRecord::new)
                   .filter(aiidaRecord -> isValidPermissionRecord(aiidaRecord, permissionExpirationTime, userId))
                   .map(aiidaRecord -> filterAllowedDataTags(aiidaRecord, allowedDataTags))
                   .buffer(cronSink.asFlux())
                   .flatMapIterable(this::aggregateRecords);
    }

    /**
     * Closes all {@link AiidaDataSource}s and emits a complete signal for all the Flux returned by
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

    private void saveRecordToDatabase(AiidaRecord aiidaRecord) {
        LOGGER.trace("Saving new record to db");
        for (AiidaRecordValue value : aiidaRecord.aiidaRecordValue()) {
            value.setAiidaRecord(aiidaRecord);
        }
        repository.save(aiidaRecord);
    }

    private void handleCombinedSinkError(Throwable throwable) {
        LOGGER.error("Got error from combined sink", throwable);
    }

    private synchronized void publishRecordToCombinedFlux(AiidaRecord data) {
        var result = combinedSink.tryEmitNext(data);

        if (result.isFailure()) {
            LOGGER.error("Error while emitting record to combined sink. Error was: {}", result);
        }
    }

    private void handleError(Throwable throwable, AiidaDataSource dataSource) {
        // TODO: GH-1304 do we try to restart the affected datasource or only notify user?
        LOGGER.error("Error from datasource {}", dataSource.name(), throwable);
    }

    private boolean isValidPermissionRecord(AiidaRecord aiidaRecord, Instant expirationTime, UUID userId) {
        return aiidaRecord.userId().equals(userId)
               && !aiidaRecord.aiidaRecordValue().isEmpty()
               && isBeforeExpiration(aiidaRecord, expirationTime);
    }

    private boolean isBeforeExpiration(AiidaRecord aiidaRecord, Instant permissionExpirationTime) {
        return aiidaRecord.timestamp().isBefore(permissionExpirationTime);
    }

    private AiidaRecord filterAllowedDataTags(AiidaRecord aiidaRecord, Set<String> allowedDataTags) {
        if (!allowedDataTags.isEmpty()) {
            var filteredValues = aiidaRecord.aiidaRecordValue()
                                            .stream()
                                            .filter(value -> allowedDataTags.contains(value.dataTag().toString()))
                                            .toList();
            aiidaRecord.setAiidaRecordValues(filteredValues);
        }

        return aiidaRecord;
    }

    private List<AiidaRecord> aggregateRecords(List<AiidaRecord> aiidaRecords) {
        // TODO: GH-1307 Currently only the last record of an asset is kept. This should be changed to a more sophisticated aggregation.
        var aggregatedRecords = aiidaRecords.stream()
                                            .collect(Collectors.toMap(AiidaRecord::dataSourceId,
                                                                      Function.identity(),
                                                                      (existingRecord, newRecord) -> newRecord,
                                                                      LinkedHashMap::new));
        return new ArrayList<>(aggregatedRecords.values());
    }
}
