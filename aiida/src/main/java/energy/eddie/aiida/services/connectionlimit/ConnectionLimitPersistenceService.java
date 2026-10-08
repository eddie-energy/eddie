// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.connectionlimit;

import energy.eddie.aiida.aggregator.InboundAggregator;
import energy.eddie.aiida.models.connectionlimit.ConnectionLimit;
import energy.eddie.aiida.models.record.InboundProcessingResult;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.aiida.repositories.ConnectionLimitRepository;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.services.InboundProcessingResultHandler;
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
    private final InboundProcessingResultHandler processingResultHandler;

    public ConnectionLimitPersistenceService(
            InboundAggregator inboundAggregator,
            ConnectionLimitRepository connectionLimitRepository,
            PermissionRepository permissionRepository,
            ObjectMapper objectMapper,
            TransactionTemplate transactionTemplate,
            InboundProcessingResultHandler processingResultHandler
    ) {
        this.inboundAggregator = inboundAggregator;
        this.connectionLimitRepository = connectionLimitRepository;
        this.permissionRepository = permissionRepository;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
        this.processingResultHandler = processingResultHandler;
    }

    @PostConstruct
    void subscribeToInboundRecords() {
        inboundAggregator.inboundRecordFlux()
                         .filter(inboundRecord -> inboundRecord.schema() == AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12)
                         .publishOn(Schedulers.boundedElastic())
                         .doOnNext(this::process)
                         .onErrorContinue((error, value) -> LOGGER.error(
                                 "Failed to process connection limits for record {}",
                                 value,
                                 error))
                         .subscribe();
    }

    void process(InboundRecord inboundRecord) {
        try {
            persistConnectionLimits(inboundRecord);
        } catch (InvalidConnectionLimitDocumentException e) {
            LOGGER.warn(e.getMessage());
            processingResultHandler.handle(
                    inboundRecord,
                    InboundProcessingResult.rejected(
                            Objects.requireNonNullElse(e.getMessage(), "The connection limit document is invalid")
                    )
            );
        } catch (RuntimeException e) {
            LOGGER.error("Failed to persist connection limit from inbound record {}: {}",
                         inboundRecord,
                         e.getMessage(),
                         e);
            processingResultHandler.handle(
                    inboundRecord,
                    InboundProcessingResult.rejected(
                            "AIIDA could not process the connection limit document: " + errorMessage(e)
                    )
            );
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
            throw new InvalidConnectionLimitDocumentException(
                    "Document permission id %s does not match receiving permission id %s"
                            .formatted(document.permissionId(), permissionId),
                    permissionId,
                    document.mrid());
        }

        var permissionMeterId = permission.meterId();
        var documentMeterId = document.meterId();

        if (documentMeterId != null && permissionMeterId != null && !documentMeterId.equals(permissionMeterId)) {
            throw new InvalidConnectionLimitDocumentException(
                    "Document meter id %s does not match permission meter id %s"
                            .formatted(documentMeterId, permissionMeterId),
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

        persist(inboundRecord, incomingLimits);
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

    private void persist(InboundRecord inboundRecord, List<ConnectionLimit> incomingLimits) {
        if (incomingLimits.isEmpty()) {
            processingResultHandler.handle(
                    inboundRecord,
                    InboundProcessingResult.rejected("The document does not contain any connection limit points")
            );
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
                processingResultHandler.handle(
                        inboundRecord,
                        InboundProcessingResult.rejected(
                                "Revision %d is not newer than the latest processed revision %d for mRID %s".formatted(
                                        document.revisionNumber(),
                                        latestRevision.get(),
                                        document.mrid()
                                )
                        )
                );
                return;
            }

            replaceByMrid(document.mrid(), incomingLimits);
            processingResultHandler.handle(inboundRecord, InboundProcessingResult.accepted());
            return;
        }

        upsertByCreatedAt(inboundRecord, incomingLimits);
    }

    private void replaceByMrid(String mrid, List<ConnectionLimit> incomingLimits) {
        transactionTemplate.executeWithoutResult(status -> {
            connectionLimitRepository.deleteByMrid(mrid);
            connectionLimitRepository.saveAll(incomingLimits);
        });
    }

    private void upsertByCreatedAt(InboundRecord inboundRecord, List<ConnectionLimit> incomingLimits) {
        int persistedCount = 0;
        for (var incoming : incomingLimits) {
            var existing = connectionLimitRepository.findCreatedAtByPermissionMeterAndInterval(incoming.permissionId(),
                                                                                               incoming.meterId(),
                                                                                               incoming.intervalStart(),
                                                                                               incoming.intervalEnd());

            if (existing.isEmpty() || incoming.createdAt().isAfter(existing.get())) {
                connectionLimitRepository.save(incoming);
                persistedCount++;
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

        int rejectedCount = incomingLimits.size() - persistedCount;
        if (rejectedCount == 0) {
            processingResultHandler.handle(inboundRecord, InboundProcessingResult.accepted());
            return;
        }

        var reason = "%d of %d connection limit points were ignored because newer or equally recent values already exist"
                .formatted(rejectedCount, incomingLimits.size());
        var processingResult = persistedCount == 0
                ? InboundProcessingResult.rejected(reason)
                : InboundProcessingResult.partiallyAccepted(reason);
        processingResultHandler.handle(inboundRecord, processingResult);
    }

    private static String errorMessage(RuntimeException exception) {
        var message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
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
