package energy.eddie.aiida.aggregator;

import energy.eddie.aiida.datasources.DataSourceAdapter;
import energy.eddie.aiida.models.datasource.DataSource;
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
    private final List<DataSourceAdapter<? extends DataSource>> dataSourceAdapters;
    private final Sinks.Many<AiidaRecord> combinedSink;
    private final AiidaRecordRepository repository;
    private final HealthContributorRegistry healthContributorRegistry;

    public Aggregator(AiidaRecordRepository repository, HealthContributorRegistry healthContributorRegistry) {
        this.repository = repository;
        this.healthContributorRegistry = healthContributorRegistry;

        dataSourceAdapters = new ArrayList<>();
        combinedSink = Sinks.many().multicast().directAllOrNothing();

        combinedSink.asFlux()
                    .publishOn(Schedulers.boundedElastic())
                    .doOnNext(this::saveRecordToDatabase)
                    .doOnError(this::handleCombinedSinkError)
                    .subscribe();
    }

    /**
     * Adds a new {@link DataSourceAdapter} to this aggregator and will subscribe to the Flux returned by
     * {@link DataSourceAdapter#start()}.
     *
     * @param dataSourceAdapter The new data source adapter to add. No check is made if this is a duplicate.
     */
    public void addNewDataSourceAdapter(DataSourceAdapter<? extends DataSource> dataSourceAdapter) {
        var dataSource = dataSourceAdapter.dataSource();
        LOGGER.info("Will add datasource {} with ID {} to aggregator", dataSource.name(), dataSource.id());

        healthContributorRegistry.registerContributor(dataSource.id() + "_" + dataSource.name(), dataSourceAdapter);
        dataSourceAdapters.add(dataSourceAdapter);
        dataSourceAdapter.start()
                  .subscribe(this::publishRecordToCombinedFlux, throwable -> handleError(throwable, dataSourceAdapter));
    }

    /**
     * Removes a {@link DataSourceAdapter} from this aggregator and will close the datasource.
     *
     * @param dataSourceAdapter The datasource adapter to remove.
     */
    public void removeDataSourceAdapter(DataSourceAdapter<? extends DataSource> dataSourceAdapter) {
        var dataSource = dataSourceAdapter.dataSource();
        LOGGER.info("Will remove datasource {} with ID {} from aggregator", dataSource.name(), dataSource.id());

        healthContributorRegistry.unregisterContributor(dataSource.id() + "_" + dataSource.name());
        dataSourceAdapters.remove(dataSourceAdapter);
        dataSourceAdapter.close();
    }

    /**
     * Returns a Flux of {@link AiidaRecord}s that either contains all records or only contains records with a {@link AiidaRecordValue#dataTag()}
     * that is in the set {@code allowedCodes}.
     * All values must have a timestamp before {@code permissionExpirationTime}.
     * Additionally, the records are buffered and aggregated by the {@link CronExpression} {@code transmissionSchedule}.
     *
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

        var flux = combinedSink.asFlux().doOnComplete(() -> {
            cronScheduler.stop();
            cronSink.tryEmitComplete();
        });

        return flux.map(AiidaRecord::new)
                   .filter(aiidaRecord -> isValidAiidaRecord(aiidaRecord, permissionExpirationTime, userId))
                   .map(aiidaRecord -> filterAllowedDataTags(aiidaRecord, allowedDataTags))
                   .buffer(cronSink.asFlux())
                   .flatMapIterable(this::aggregateRecords);
    }

    /**
     * Closes all {@link DataSourceAdapter}s and emits a complete signal for all the Flux returned by
     */
    @Override
    public void close() {
        // ignore if complete signal can't be emitted successfully
        combinedSink.tryEmitComplete();

        LOGGER.info("Closing all {} datasources", dataSourceAdapters.size());
        for (var dataSourceAdapter : dataSourceAdapters) {
            dataSourceAdapter.close();
        }
    }

    private void saveRecordToDatabase(AiidaRecord aiidaRecord) {
        LOGGER.trace("Saving new record to db");
        for (AiidaRecordValue value : aiidaRecord.aiidaRecordValues()) {
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

    private void handleError(Throwable throwable, DataSourceAdapter<? extends DataSource> dataSourceAdapter) {
        // TODO: GH-1304 do we try to restart the affected datasource or only notify user?
        LOGGER.error("Error from datasource {}", dataSourceAdapter.dataSource().name(), throwable);
    }

    private boolean isValidAiidaRecord(AiidaRecord aiidaRecord, Instant expirationTime, UUID userId) {
        return areAiidaRecordValuesValid(aiidaRecord.aiidaRecordValues()) &&
               isBeforeExpiration(aiidaRecord, expirationTime) &&
               doesAiidaRecordBelongToCurrentUser(aiidaRecord, userId);
    }

    private boolean areAiidaRecordValuesValid(List<AiidaRecordValue> aiidaRecordValues) {
        return !aiidaRecordValues.isEmpty();
    }

    private boolean doesAiidaRecordBelongToCurrentUser(AiidaRecord aiidaRecord, UUID userId) {
        return aiidaRecord.userId().equals(userId);
    }

    private boolean isBeforeExpiration(AiidaRecord aiidaRecord, Instant permissionExpirationTime) {
        return aiidaRecord.timestamp().isBefore(permissionExpirationTime);
    }

    private AiidaRecord filterAllowedDataTags(AiidaRecord aiidaRecord, Set<String> allowedDataTags) {
        if (!allowedDataTags.isEmpty()) {
            var filteredValues = aiidaRecord.aiidaRecordValues()
                                            .stream()
                                            .filter(value -> allowedDataTags.contains(value.dataTag().toString()))
                                            .toList();
            aiidaRecord.setAiidaRecordValues(filteredValues);
        }

        return aiidaRecord;
    }

    private List<AiidaRecord> aggregateRecords(List<AiidaRecord> aiidaRecords) {
        // TODO: GH-1307 Currently only the last record of a data source is kept. This should be changed to a more sophisticated aggregation.
        var aggregatedRecords = aiidaRecords.stream()
                                            .collect(Collectors.toMap(AiidaRecord::dataSourceId,
                                                                      Function.identity(),
                                                                      (existingRecord, newRecord) -> newRecord,
                                                                      LinkedHashMap::new));
        return new ArrayList<>(aggregatedRecords.values());
    }
}
