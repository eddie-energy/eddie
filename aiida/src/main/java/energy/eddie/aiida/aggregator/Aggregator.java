package energy.eddie.aiida.aggregator;

import energy.eddie.aiida.adapters.datasource.DataSourceAdapter;
import energy.eddie.aiida.adapters.datasource.inbound.InboundAdapter;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.aiida.repositories.AiidaRecordRepository;
import energy.eddie.aiida.repositories.InboundRecordRepository;
import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;
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
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.function.BinaryOperator.maxBy;

@Component
public class Aggregator implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Aggregator.class);
    private static final String HEALTH_REGISTRY_PREFIX = "DATA_SOURCE_";

    private final List<DataSourceAdapter<? extends DataSource>> dataSourceAdapters;
    private final Sinks.Many<AiidaRecord> combinedSink;
    private final AiidaRecordRepository aiidaRecordRepository;
    private final InboundRecordRepository inboundRecordRepository;
    private final HealthContributorRegistry healthContributorRegistry;

    public Aggregator(
            AiidaRecordRepository aiidaRecordRepository,
            InboundRecordRepository inboundRecordRepository,
            HealthContributorRegistry healthContributorRegistry
    ) {
        this.aiidaRecordRepository = aiidaRecordRepository;
        this.inboundRecordRepository = inboundRecordRepository;
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

        healthContributorRegistry.registerContributor(HEALTH_REGISTRY_PREFIX + dataSource.id(), dataSourceAdapter);
        dataSourceAdapters.add(dataSourceAdapter);
        dataSourceAdapter.start()
                         .subscribe(this::publishRecordToCombinedFlux,
                                    throwable -> handleError(throwable, dataSourceAdapter));

        if (dataSourceAdapter instanceof InboundAdapter inboundAdapter) {
            inboundAdapter.inboundRecordFlux()
                          .subscribe(this::saveInboundRecordToDatabase);
        }
    }

    /**
     * Removes a {@link DataSourceAdapter} from this aggregator and will close the datasource.
     *
     * @param dataSourceAdapter The datasource adapter to remove.
     */
    public void removeDataSourceAdapter(DataSourceAdapter<? extends DataSource> dataSourceAdapter) {
        var dataSource = dataSourceAdapter.dataSource();
        LOGGER.info("Will remove datasource {} with ID {} from aggregator", dataSource.name(), dataSource.id());

        healthContributorRegistry.unregisterContributor(HEALTH_REGISTRY_PREFIX + dataSource.id());
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
     * @param allowedAsset             The asset that should be included in the returned Flux.
     * @param permissionExpirationTime Instant when the permission expires.
     * @param transmissionSchedule     The schedule at which the data should be transmitted.
     * @param userId                   The user id of the permission creator.
     * @return A Flux on which will only have AiidaRecords with a code matching one of the codes of the
     * {@code allowedCodes} set, a timestamp that is before {@code permissionExpirationTime} and that are aggregated
     * by the {@code transmissionSchedule}.
     */
    public Flux<AiidaRecord> getFilteredFlux(
            Set<ObisCode> allowedDataTags,
            AiidaAsset allowedAsset,
            Instant permissionExpirationTime,
            CronExpression transmissionSchedule,
            UUID userId,
            UUID dataSourceId
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
                   .filter(aiidaRecord -> isValidAiidaRecord(aiidaRecord,
                                                             allowedAsset,
                                                             permissionExpirationTime,
                                                             userId,
                                                             dataSourceId))
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
        aiidaRecordRepository.save(aiidaRecord);
    }

    private void saveInboundRecordToDatabase(InboundRecord inboundRecord) {
        LOGGER.trace("New raw record saved to the database.");
        inboundRecordRepository.save(inboundRecord);
    }

    private void handleCombinedSinkError(Throwable throwable) {
        LOGGER.error("Error from combindes sink", throwable);
    }

    private synchronized void publishRecordToCombinedFlux(AiidaRecord data) {
        var result = combinedSink.tryEmitNext(data);

        if (result.isFailure()) {
            LOGGER.error("Error while emitting record to combined sink. Error was: {}", result);
        }
    }

    private void handleError(Throwable throwable, DataSourceAdapter<? extends DataSource> dataSourceAdapter) {
        // TODO: GH-1591 do we try to restart the affected datasource or only notify user?
        LOGGER.error("Error from datasource {}", dataSourceAdapter.dataSource().name(), throwable);
    }

    private boolean isValidAiidaRecord(
            AiidaRecord aiidaRecord,
            AiidaAsset allowedAsset,
            Instant expirationTime,
            UUID userId,
            UUID dataSourceId
    ) {
        return isAllowedAsset(aiidaRecord, allowedAsset) &&
               areAiidaRecordValuesValid(aiidaRecord.aiidaRecordValues()) &&
               isBeforeExpiration(aiidaRecord, expirationTime) &&
               doesAiidaRecordBelongToCurrentDataSource(aiidaRecord, dataSourceId) &&
               doesAiidaRecordBelongToCurrentUser(aiidaRecord, userId);
    }

    private boolean isAllowedAsset(AiidaRecord aiidaRecord, AiidaAsset allowedAsset) {
        return aiidaRecord.asset() == allowedAsset;
    }

    private boolean areAiidaRecordValuesValid(List<AiidaRecordValue> aiidaRecordValues) {
        return !aiidaRecordValues.isEmpty();
    }

    private boolean doesAiidaRecordBelongToCurrentDataSource(AiidaRecord aiidaRecord, UUID dataSourceId) {
        return aiidaRecord.dataSourceId().equals(dataSourceId);
    }

    private boolean doesAiidaRecordBelongToCurrentUser(AiidaRecord aiidaRecord, UUID userId) {
        return aiidaRecord.userId().equals(userId);
    }

    private boolean isBeforeExpiration(AiidaRecord aiidaRecord, Instant permissionExpirationTime) {
        return aiidaRecord.timestamp().isBefore(permissionExpirationTime);
    }

    private AiidaRecord filterAllowedDataTags(AiidaRecord aiidaRecord, Set<ObisCode> allowedDataTags) {
        if (!allowedDataTags.isEmpty()) {
            var filteredValues = aiidaRecord.aiidaRecordValues()
                                            .stream()
                                            .filter(value -> allowedDataTags.contains(value.dataTag()))
                                            .toList();
            aiidaRecord.setAiidaRecordValues(filteredValues);
        }

        return aiidaRecord;
    }

    private List<AiidaRecord> aggregateRecords(List<AiidaRecord> aiidaRecords) {
        var aggregatedRecords = aiidaRecords.stream()
                                            .collect(Collectors.toMap(
                                                    AiidaRecord::dataSourceId,
                                                    Function.identity(),
                                                    this::mergeRecords,
                                                    LinkedHashMap::new
                                            ));
        return new ArrayList<>(aggregatedRecords.values());
    }

    private AiidaRecord mergeRecords(AiidaRecord r1, AiidaRecord r2) {
        var latestRecord = r2.timestamp().isAfter(r1.timestamp()) ? r2 : r1;
        Map<String, AiidaRecordValue> mergedValues = new HashMap<>();

        Stream.concat(r1.aiidaRecordValues().stream(), r2.aiidaRecordValues().stream())
              .forEach(val -> mergedValues.merge(
                      val.rawTag(),
                      val,
                      maxBy(comparing(v -> v.aiidaRecord().timestamp()))
              ));

        return new AiidaRecord(
                latestRecord.timestamp(),
                latestRecord.asset(),
                latestRecord.userId(),
                latestRecord.dataSourceId(),
                new ArrayList<>(mergedValues.values())
        );
    }
}
