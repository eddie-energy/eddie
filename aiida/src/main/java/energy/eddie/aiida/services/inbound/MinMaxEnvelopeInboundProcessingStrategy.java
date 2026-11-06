// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.inbound;

import energy.eddie.aiida.models.connectionlimit.ConnectionLimit;
import energy.eddie.aiida.models.record.InboundProcessingResult;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.aiida.services.connectionlimit.ConnectionLimitPersistenceService;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import energy.eddie.cim.v1_12.recmmoe.RECMMOEEnvelope;
import energy.eddie.cim.v1_12.recmmoe.Series;
import energy.eddie.cim.v1_12.recmmoe.SeriesPeriod;
import energy.eddie.cim.v1_12.recmmoe.TimeSeriesSeries;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class MinMaxEnvelopeInboundProcessingStrategy implements InboundProcessingStrategy {
    private static final Logger LOGGER = LoggerFactory.getLogger(MinMaxEnvelopeInboundProcessingStrategy.class);

    private final ObjectMapper objectMapper;
    private final ConnectionLimitPersistenceService connectionLimitPersistenceService;

    public MinMaxEnvelopeInboundProcessingStrategy(
            ObjectMapper objectMapper,
            ConnectionLimitPersistenceService connectionLimitPersistenceService
    ) {
        this.objectMapper = objectMapper;
        this.connectionLimitPersistenceService = connectionLimitPersistenceService;
    }

    @Override
    public AiidaSchema supportedSchema() {
        return AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12;
    }

    @Override
    public InboundProcessingResult process(InboundRecord inboundRecord) {
        try {
            var connectionLimits = parseConnectionLimits(inboundRecord);
            var persisted = persistConnectionLimits(connectionLimits);
            var total = connectionLimits.size();

            if (persisted == total) {
                return InboundProcessingResult.accepted();
            }

            var ignored = total - persisted;
            var reason = "%d of %d connection limit points were ignored because newer or equally recent values already exist"
                    .formatted(ignored, total);
            return persisted == 0
                    ? InboundProcessingResult.rejected(reason)
                    : InboundProcessingResult.partiallyAccepted(reason);
        } catch (InvalidConnectionLimitDocumentException exception) {
            LOGGER.warn(exception.getMessage());
            return InboundProcessingResult.rejected(Objects.requireNonNullElse(exception.getMessage(),
                                                                               "The connection limit document is invalid"));
        }
    }

    private int persistConnectionLimits(List<ConnectionLimit> connectionLimits) throws InvalidConnectionLimitDocumentException {
        var firstLimit = connectionLimits.getFirst();
        var latestRevision = connectionLimitPersistenceService.findMaxRevisionNumberByMrid(firstLimit.mrid());
        if (latestRevision.isPresent()) {
            if (firstLimit.revisionNumber() <= latestRevision.get()) {
                LOGGER.warn(
                        "Rejected connection limit document for permission {} and mRID {}: out-of-order revision {} <= latest revision {}",
                        firstLimit.permissionId(),
                        firstLimit.mrid(),
                        firstLimit.revisionNumber(),
                        latestRevision.get());
                throw new InvalidConnectionLimitDocumentException(
                        "Revision %d is not newer than the latest processed revision %d"
                                .formatted(firstLimit.revisionNumber(), latestRevision.get()),
                        firstLimit.permissionId(),
                        firstLimit.mrid());
            }

            connectionLimitPersistenceService.replaceByMrid(firstLimit.mrid(), connectionLimits);
            return connectionLimits.size();
        }

        return connectionLimitPersistenceService.upsertByCreatedAt(connectionLimits);
    }

    private List<ConnectionLimit> parseConnectionLimits(InboundRecord inboundRecord) throws InvalidConnectionLimitDocumentException {
        var envelope = objectMapper.readValue(inboundRecord.payload(), RECMMOEEnvelope.class);

        var document = parseEnvelope(envelope);
        var permission = inboundRecord.dataSource().permission();

        var permissionId = permission.id();
        if (!Objects.equals(permissionId, document.permissionId())) {
            throw new InvalidConnectionLimitDocumentException(
                    "Document permission id %s does not match receiving permission id %s".formatted(document.permissionId(),
                                                                                                    permissionId),
                    permissionId,
                    document.mrid());
        }

        var permissionMeterId = permission.meterId();
        var documentMeterId = document.meterId();

        if (documentMeterId != null && permissionMeterId != null && !documentMeterId.equals(permissionMeterId)) {
            throw new InvalidConnectionLimitDocumentException(
                    "Document meter id %s does not match permission meter id %s".formatted(documentMeterId,
                                                                                           permissionMeterId),
                    permissionId,
                    document.mrid());
        }

        var meterId = Objects.requireNonNullElse(document.meterId(), permission.meterId());

        var connectionLimits = new ArrayList<ConnectionLimit>();
        for (var period : document.periods()) {
            connectionLimits.addAll(toConnectionLimits(document.permissionId(),
                                                       meterId,
                                                       document.mrid(),
                                                       document.revisionNumber(),
                                                       document.createdAt(),
                                                       period));
        }

        if (connectionLimits.isEmpty()) {
            throw new InvalidConnectionLimitDocumentException(
                    "The document does not contain any connection limit points",
                    permissionId,
                    document.mrid());
        }

        return connectionLimits;
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
