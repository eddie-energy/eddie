// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.aggregator;

import energy.eddie.aiida.adapters.datasource.DataSourceAdapter;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.aiida.repositories.AiidaRecordRepository;
import energy.eddie.api.agnostic.aiida.AiidaAsset;
import energy.eddie.api.agnostic.aiida.ObisCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.health.registry.HealthContributorRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.function.BinaryOperator.maxBy;

@Component
public class Aggregator extends AbstractAggregator<AiidaRecord> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Aggregator.class);

    private final AiidaRecordRepository aiidaRecordRepository;

    public Aggregator(
            AiidaRecordRepository aiidaRecordRepository,
            HealthContributorRegistry healthContributorRegistry
    ) {
        super(healthContributorRegistry);
        this.aiidaRecordRepository = aiidaRecordRepository;
    }

    @Override
    public void addNewDataSourceAdapter(DataSourceAdapter<? extends DataSource> dataSourceAdapter) {
        super.addNewDataSourceAdapter(dataSourceAdapter);
        dataSourceAdapter.start()
                         .subscribe(this::publishRecordToCombinedFlux,
                                    throwable -> handleError(throwable, dataSourceAdapter));
    }

    @Override
    protected void saveRecordToDatabase(AiidaRecord dataRecord) {
        LOGGER.trace("Saving new AIIDA record to db");
        for (AiidaRecordValue value : dataRecord.aiidaRecordValues()) {
            value.setAiidaRecord(dataRecord);
        }
        aiidaRecordRepository.save(dataRecord);
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
    @SuppressWarnings("FutureReturnValueIgnored")
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
        cronScheduler.schedule(() -> cronSink.tryEmitNext(true), cronTrigger);

        var flux = combinedRecordSink.asFlux().doOnComplete(() -> {
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
                   .flatMapIterable(this::aggregateRecords)
                   .onErrorContinue((err, obj) -> LOGGER.error("Error while filtering records for permission", err));
    }

    private boolean isValidAiidaRecord(
            AiidaRecord aiidaRecord,
            AiidaAsset allowedAsset,
            Instant expirationTime,
            UUID userId,
            UUID dataSourceId
    ) {
        var dataSource = aiidaRecord.dataSource();
        return isAllowedAsset(dataSource, allowedAsset) &&
               areAiidaRecordValuesValid(aiidaRecord.aiidaRecordValues()) &&
               isBeforeExpiration(aiidaRecord, expirationTime) &&
               isSameDataSource(dataSource, dataSourceId) &&
               doesDataSourceBelongToCurrentUser(dataSource, userId);
    }

    private boolean isAllowedAsset(DataSource dataSource, AiidaAsset allowedAsset) {
        return dataSource.asset() == allowedAsset;
    }

    private boolean areAiidaRecordValuesValid(List<AiidaRecordValue> aiidaRecordValues) {
        return !aiidaRecordValues.isEmpty();
    }

    private boolean isSameDataSource(DataSource dataSource, UUID dataSourceId) {
        return dataSource.id().equals(dataSourceId);
    }

    private boolean doesDataSourceBelongToCurrentUser(DataSource dataSource, UUID userId) {
        return dataSource.userId().equals(userId);
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
                                                    r -> r.dataSource().id(),
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
                latestRecord.dataSource(),
                new ArrayList<>(mergedValues.values())
        );
    }
}
