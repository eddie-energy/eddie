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
import reactor.core.scheduler.Schedulers;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class ConnectionLimitPersistenceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionLimitPersistenceService.class);

    private final InboundAggregator inboundAggregator;
    private final ConnectionLimitRepository connectionLimitRepository;
    private final ObjectMapper objectMapper;

    public ConnectionLimitPersistenceService(
            InboundAggregator inboundAggregator,
            ConnectionLimitRepository connectionLimitRepository,
            ObjectMapper objectMapper
    ) {
        this.inboundAggregator = inboundAggregator;
        this.connectionLimitRepository = connectionLimitRepository;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void subscribeToInboundRecords() {
        inboundAggregator.inboundRecordFlux()
                         .filter(inboundRecord -> inboundRecord.schema() == AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12)
                         .publishOn(Schedulers.boundedElastic())
                         .doOnNext(this::persistConnectionLimits)
                         .onErrorContinue((error, value) ->
                                                  LOGGER.error("Failed to persist connection limits for record {}",
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

        for (var timeSeriesSeries : marketDocument.getTimeSeriesSeries()) {
            for (var series : timeSeriesSeries.getSeries()) {
                for (var period : series.getPeriods()) {
                    persistPeriod(permissionId, meterId, period);
                }
            }
        }
    }

    private void persistPeriod(UUID permissionId, @Nullable String meterId, SeriesPeriod period) {
        var periodStart = Instant.parse(period.getTimeInterval().getStart());
        var resolution = Duration.parse(period.getResolution().toString());

        for (var point : period.getPoints()) {
            var intervalStart = periodStart.plus(resolution.multipliedBy(point.getPosition() - 1L));
            var intervalEnd = intervalStart.plus(resolution);

            LOGGER.debug("Persisting connection limit for permission {} at {}", permissionId, intervalStart);
            connectionLimitRepository.save(new ConnectionLimit(
                    permissionId,
                    meterId,
                    intervalStart,
                    intervalEnd,
                    point.getMinQuantityQuantity(),
                    point.getMaxQuantityQuantity()
            ));
        }
    }
}
