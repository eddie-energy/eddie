// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.connectionlimit;

import energy.eddie.aiida.aggregator.InboundAggregator;
import energy.eddie.aiida.models.connectionlimit.ConnectionLimit;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.aiida.repositories.ConnectionLimitRepository;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import energy.eddie.cim.v1_12.recmmoe.RECMMOEEnvelope;
import energy.eddie.cim.v1_12.recmmoe.SeriesPeriod;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.scheduler.Schedulers;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ConnectionLimitPersistenceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionLimitPersistenceService.class);

    private final InboundAggregator inboundAggregator;
    private final ConnectionLimitRepository connectionLimitRepository;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public ConnectionLimitPersistenceService(
            InboundAggregator inboundAggregator,
            ConnectionLimitRepository connectionLimitRepository,
            ObjectMapper objectMapper,
            TransactionTemplate transactionTemplate
    ) {
        this.inboundAggregator = inboundAggregator;
        this.connectionLimitRepository = connectionLimitRepository;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
    }

    @PostConstruct
    void subscribeToInboundRecords() {
        inboundAggregator.inboundRecordFlux()
                         .filter(inboundRecord -> inboundRecord.schema() == AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12)
                         .publishOn(Schedulers.boundedElastic())
                         .doOnNext(this::persistConnectionLimits)
                         .onErrorContinue((error, value) -> LOGGER.error(
                                 "Failed to persist connection limits for record {}",
                                 value,
                                 error))
                         .subscribe();
    }

    private void persistConnectionLimits(InboundRecord inboundRecord) {
        var envelope = objectMapper.readValue(inboundRecord.payload(), RECMMOEEnvelope.class);
        var meta = envelope.getMessageDocumentHeader().getMetaInformation();
        var marketDocument = envelope.getMarketDocument();

        var permissionId = UUID.fromString(meta.getRequestPermissionId());
        var meterId = meta.getAsset() != null ? meta.getAsset().getMeterId() : null;
        var mrid = marketDocument.getMRID();

        if (mrid == null || mrid.isBlank()) {
            LOGGER.warn("Rejected connection limit document for permission {}: mRID is missing", permissionId);
            return;
        }

        var creationDateTime = envelope.getMessageDocumentHeader().getCreationDateTime();
        if (creationDateTime == null) {
            LOGGER.warn("Rejected connection limit document for permission {} and mRID {}: createdDateTime is missing",
                        permissionId,
                        mrid);
            return;
        }
        var createdAt = creationDateTime.toInstant();

        int revisionNumber;
        try {
            revisionNumber = Integer.parseInt(marketDocument.getRevisionNumber());
        } catch (NumberFormatException e) {
            LOGGER.warn("Rejected connection limit document for permission {} and mRID {}: invalid revisionNumber '{}'",
                        permissionId,
                        mrid,
                        marketDocument.getRevisionNumber());
            return;
        }

        if (revisionNumber < 1) {
            LOGGER.warn(
                    "Rejected connection limit document for permission {} and mRID {}: revisionNumber must be >= 1, was {}",
                    permissionId,
                    mrid,
                    revisionNumber);
            return;
        }

        var incomingLimits = new ArrayList<ConnectionLimit>();
        for (var timeSeriesSeries : marketDocument.getTimeSeriesSeries()) {
            for (var series : timeSeriesSeries.getSeries()) {
                for (var period : series.getPeriods()) {
                    incomingLimits.addAll(toConnectionLimits(permissionId,
                                                             meterId,
                                                             mrid,
                                                             revisionNumber,
                                                             createdAt,
                                                             period));
                }
            }
        }

        persist(incomingLimits);
    }

    private List<ConnectionLimit> toConnectionLimits(
            UUID permissionId,
            @Nullable String meterId,
            String mrid,
            int revisionNumber,
            Instant createdAt,
            SeriesPeriod period
    ) {
        var periodStart = Instant.parse(period.getTimeInterval().getStart());
        var resolution = Duration.parse(period.getResolution().toString());
        var limits = new ArrayList<ConnectionLimit>();

        for (var point : period.getPoints()) {
            var intervalStart = periodStart.plus(resolution.multipliedBy(point.getPosition() - 1L));
            var intervalEnd = intervalStart.plus(resolution);

            limits.add(new ConnectionLimit(permissionId,
                                           meterId,
                                           intervalStart,
                                           intervalEnd,
                                           point.getMinQuantityQuantity(),
                                           point.getMaxQuantityQuantity(),
                                           mrid,
                                           revisionNumber,
                                           createdAt));
        }

        return limits;
    }

    private void persist(List<ConnectionLimit> incomingLimits) {
        if (incomingLimits.isEmpty()) {
            return;
        }

        var document = incomingLimits.getFirst();
        var latestRevision = connectionLimitRepository.findMaxRevisionNumberByMrid(document.mrid());
        if (latestRevision.isPresent()) {
            if (document.revisionNumber() <= latestRevision.get()) {
                LOGGER.warn(
                        "Rejected connection limit document for permission {} and mRID {}: out-of-order revision {} <= latest revision {}",
                        document.permissionId(),
                        document.mrid(),
                        document.revisionNumber(),
                        latestRevision.get());
                return;
            }

            replaceByMrid(document.mrid(), incomingLimits);
            return;
        }

        upsertByCreatedAt(incomingLimits);
    }

    private void replaceByMrid(String mrid, List<ConnectionLimit> incomingLimits) {
        transactionTemplate.executeWithoutResult(status -> {
            connectionLimitRepository.deleteByMrid(mrid);
            connectionLimitRepository.saveAll(incomingLimits);
        });
    }

    private void upsertByCreatedAt(List<ConnectionLimit> incomingLimits) {
        for (var incoming : incomingLimits) {
            var existing = connectionLimitRepository.findCreatedAtByPermissionMeterAndInterval(incoming.permissionId(),
                                                                                               incoming.meterId(),
                                                                                               incoming.intervalStart(),
                                                                                               incoming.intervalEnd());

            if (existing.isEmpty() || incoming.createdAt().isAfter(existing.get())) {
                connectionLimitRepository.save(incoming);
            } else {
                LOGGER.warn(
                        "Rejected connection limit update for permission {}, meter {}, interval [{} - {}]: createdDateTime {} is not newer than existing createdDateTime {}",
                        incoming.permissionId(),
                        incoming.meterId(),
                        incoming.intervalStart(),
                        incoming.intervalEnd(),
                        incoming.createdAt(),
                        existing.get());
            }
        }
    }
}
