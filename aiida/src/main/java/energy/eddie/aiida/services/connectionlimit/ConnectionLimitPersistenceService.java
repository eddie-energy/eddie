// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.connectionlimit;

import energy.eddie.aiida.aggregator.InboundAggregator;
import energy.eddie.aiida.models.connectionlimit.ConnectionLimit;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.aiida.repositories.ConnectionLimitRepository;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import energy.eddie.cim.v1_12.recmmoe.RECMMOEEnvelope;
import energy.eddie.cim.v1_12.recmmoe.Series;
import energy.eddie.cim.v1_12.recmmoe.SeriesPeriod;
import energy.eddie.cim.v1_12.recmmoe.TimeSeriesSeries;
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
import java.util.Objects;
import java.util.UUID;

@Service
public class ConnectionLimitPersistenceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionLimitPersistenceService.class);

    private final InboundAggregator inboundAggregator;
    private final ConnectionLimitRepository connectionLimitRepository;
    private final PermissionRepository permissionRepository;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public ConnectionLimitPersistenceService(
            InboundAggregator inboundAggregator,
            ConnectionLimitRepository connectionLimitRepository,
            PermissionRepository permissionRepository,
            ObjectMapper objectMapper,
            TransactionTemplate transactionTemplate
    ) {
        this.inboundAggregator = inboundAggregator;
        this.connectionLimitRepository = connectionLimitRepository;
        this.permissionRepository = permissionRepository;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
    }

    @PostConstruct
    void subscribeToInboundRecords() {
        inboundAggregator.inboundRecordFlux()
                         .filter(inboundRecord -> inboundRecord.schema() == AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12)
                         .publishOn(Schedulers.boundedElastic())
                         .doOnNext(this::handleInboundRecord)
                         .onErrorContinue((error, value) -> LOGGER.error(
                                 "Failed to persist connection limits for record {}",
                                 value,
                                 error))
                         .subscribe();
    }

    private void handleInboundRecord(InboundRecord inboundRecord) {
        try {
            persistConnectionLimits(inboundRecord);
        } catch (InvalidConnectionLimitDocumentException e) {
            LOGGER.warn(e.getMessage());
        } catch (IllegalArgumentException e) {
            LOGGER.error("Failed to persist connection limit from inbound record {}: {}",
                         inboundRecord,
                         e.getMessage());
        }
    }

    private void persistConnectionLimits(InboundRecord inboundRecord) throws InvalidConnectionLimitDocumentException, IllegalArgumentException {
        var envelope = objectMapper.readValue(inboundRecord.payload(), RECMMOEEnvelope.class);

        var document = parseEnvelope(envelope);
        var permission = permissionRepository.findInboundByDataSourceId(inboundRecord.dataSource().id())
                                             .orElseThrow(() -> new IllegalArgumentException(
                                                     "Data source did not reference a valid permission."));

        var permissionId = permission.id();
        if (!Objects.equals(permissionId, document.permissionId())) {
            throw new InvalidConnectionLimitDocumentException("document permission id did not match record",
                                                              permissionId,
                                                              document.mrid());
        }

        var permissionMeterId = permission.meterId();
        var documentMeterId = document.meterId();

        if (documentMeterId != null && permissionMeterId != null && !documentMeterId.equals(permissionMeterId)) {
            throw new InvalidConnectionLimitDocumentException("document meter id did not match permission meter id",
                                                              permissionId,
                                                              document.mrid());
        }

        var meterId = Objects.requireNonNullElse(document.meterId(), permission.meterId());

        var incomingLimits = new ArrayList<ConnectionLimit>();
        for (var period : document.periods()) {
            incomingLimits.addAll(toConnectionLimits(document.permissionId(),
                                                     meterId,
                                                     document.mrid(),
                                                     document.revisionNumber(),
                                                     document.createdAt(),
                                                     period));
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

    private ConnectionLimitDocument parseEnvelope(RECMMOEEnvelope envelope) throws InvalidConnectionLimitDocumentException {
        var meta = envelope.getMessageDocumentHeader().getMetaInformation();
        var marketDocument = envelope.getMarketDocument();

        var permissionId = UUID.fromString(meta.getRequestPermissionId());
        var meterId = meta.getAsset() == null ? null : meta.getAsset().getMeterId();
        if (meterId != null && meterId.isBlank()) {
            meterId = null;
        }

        var mrid = marketDocument.getMRID();
        if (mrid == null || mrid.isBlank()) {
            throw new InvalidConnectionLimitDocumentException("MRID is missing", permissionId);
        }

        var creationDateTime = envelope.getMessageDocumentHeader().getCreationDateTime();
        if (creationDateTime == null) {
            throw new InvalidConnectionLimitDocumentException("createdDateTime is missing", permissionId, mrid);
        }
        var createdAt = creationDateTime.toInstant();

        int revisionNumber;
        try {
            revisionNumber = Integer.parseInt(marketDocument.getRevisionNumber());
        } catch (NumberFormatException e) {
            throw new InvalidConnectionLimitDocumentException("Invalid revisionNumber %s".formatted(marketDocument.getRevisionNumber()),
                                                              permissionId,
                                                              mrid);
        }

        if (revisionNumber < 1) {
            throw new InvalidConnectionLimitDocumentException("revisionNumber must be >= 1, was %s".formatted(
                    revisionNumber), permissionId, mrid);
        }

        var periods = new ArrayList<SeriesPeriod>();
        for (TimeSeriesSeries timeSeriesSeries : marketDocument.getTimeSeriesSeries()) {
            for (Series series : timeSeriesSeries.getSeries()) {
                periods.addAll(series.getPeriods());
            }
        }

        return new ConnectionLimitDocument(permissionId, meterId, mrid, revisionNumber, createdAt, periods);
    }

    private record ConnectionLimitDocument(UUID permissionId, @Nullable String meterId, String mrid, int revisionNumber,
                                           Instant createdAt, List<SeriesPeriod> periods) {}

    private static class InvalidConnectionLimitDocumentException extends Exception {
        InvalidConnectionLimitDocumentException(String message, UUID permissionId) {
            super("Rejected connection limit document for permission %s: %s".formatted(permissionId, message));
        }

        InvalidConnectionLimitDocumentException(String message, UUID permissionId, String mrid) {
            super("Rejected connection limit document for permission %s and mRID %s: %s".formatted(permissionId,
                                                                                                   mrid,
                                                                                                   message));
        }
    }
}
