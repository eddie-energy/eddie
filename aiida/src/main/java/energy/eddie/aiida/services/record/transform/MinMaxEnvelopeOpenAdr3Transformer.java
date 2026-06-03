// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.record.transform;

import com.fasterxml.jackson.annotation.JsonInclude;
import energy.eddie.aiida.models.permission.InboundMessageFormat;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import energy.eddie.cim.v1_12.recmmoe.*;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class MinMaxEnvelopeOpenAdr3Transformer implements InboundPayloadTransformer {
    private static final String OBJECT_TYPE = "EVENT";
    private static final String EVENT_PAYLOAD_DESCRIPTOR = "EVENT_PAYLOAD_DESCRIPTOR";
    private static final String IMPORT_CAPACITY_LIMIT = "IMPORT_CAPACITY_LIMIT";
    private static final String EXPORT_CAPACITY_LIMIT = "EXPORT_CAPACITY_LIMIT";
    private static final String CAPACITY_LIMIT_UNIT = "W";
    private static final BigDecimal WATT_PER_KILO_WATT = BigDecimal.valueOf(1000);
    private static final List<EventPayloadDescriptor> PAYLOAD_DESCRIPTORS = List.of(
            new EventPayloadDescriptor(EVENT_PAYLOAD_DESCRIPTOR, IMPORT_CAPACITY_LIMIT, CAPACITY_LIMIT_UNIT),
            new EventPayloadDescriptor(EVENT_PAYLOAD_DESCRIPTOR, EXPORT_CAPACITY_LIMIT, CAPACITY_LIMIT_UNIT)
    );

    @Override
    public boolean supports(AiidaSchema schema, InboundMessageFormat inboundMessageFormat) {
        return schema == AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12
               && inboundMessageFormat == InboundMessageFormat.OPENADR_3_1;
    }

    @Override
    public String transform(ObjectMapper objectMapper, InboundRecord inboundRecord) {
        var envelope = objectMapper.readValue(inboundRecord.payload(), RECMMOEEnvelope.class);
        return objectMapper.writeValueAsString(toEvent(envelope));
    }

    private OpenAdrEvent toEvent(RECMMOEEnvelope envelope) {
        var marketDocument = requireNonNull(envelope.getMarketDocument(), "marketDocument");
        var period = firstTimeSeriesPeriod(marketDocument);
        var messageHeader = envelope.getMessageDocumentHeader();

        return new OpenAdrEvent(
                id(marketDocument),
                createdDateTime(messageHeader),
                modificationDateTime(marketDocument),
                OBJECT_TYPE,
                programId(marketDocument),
                eventName(marketDocument),
                duration(marketDocument),
                PAYLOAD_DESCRIPTORS,
                intervalPeriod(marketDocument, period),
                intervals(period)
        );
    }

    private @Nullable String id(RECMMOEMarketDocument marketDocument) {
        return notBlankOrNull(marketDocument.getMRID());
    }

    private @Nullable String createdDateTime(@Nullable MessageDocumentHeader messageDocumentHeader) {
        return dateTimeOrNull(messageDocumentHeader == null ? null : messageDocumentHeader.getCreationDateTime());
    }

    private @Nullable String modificationDateTime(RECMMOEMarketDocument marketDocument) {
        return dateTimeOrNull(marketDocument.getLastModifiedDateTime());
    }

    private String programId(RECMMOEMarketDocument marketDocument) {
        return requireNonBlank(marketDocument.getProcessProcessType(), "marketDocument.process.processType");
    }

    private String eventName(RECMMOEMarketDocument marketDocument) {
        return marketDocument.getDescription();
    }

    private Duration duration(RECMMOEMarketDocument marketDocument) {
        var interval = requireNonNull(marketDocument.getPeriodTimeInterval(), "marketDocument.period.timeInterval");
        return Duration.between(OffsetDateTime.parse(interval.getStart()), OffsetDateTime.parse(interval.getEnd()));
    }

    private IntervalPeriod intervalPeriod(RECMMOEMarketDocument marketDocument, SeriesPeriod period) {
        var interval = requireNonNull(marketDocument.getPeriodTimeInterval(), "marketDocument.period.timeInterval");
        var start = requireNonBlank(interval.getStart(), "marketDocument.period.timeInterval.start");
        var normalizedStart = OffsetDateTime.parse(start).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        var resolution = requireNonNull(period.getResolution(), "period.resolution");

        return new IntervalPeriod(normalizedStart, resolution);
    }

    private List<Interval> intervals(SeriesPeriod period) {
        return period.getPoints().stream().map(this::interval).toList();
    }

    private SeriesPeriod firstTimeSeriesPeriod(RECMMOEMarketDocument marketDocument) {
        var timeSeriesSeries = marketDocument.getTimeSeriesSeries();
        if (timeSeriesSeries.size() != 1) {
            throw new IllegalArgumentException(
                    "OpenADR 3 inbound mapping supports exactly one timeSeries but found %d."
                            .formatted(timeSeriesSeries.size())
            );
        }

        var series = timeSeriesSeries.getFirst().getSeries();
        if (series.size() != 1) {
            throw new IllegalArgumentException(
                    "OpenADR 3 inbound mapping supports exactly one series but found %d."
                            .formatted(timeSeriesSeries.size())
            );
        }

        var periods = series.getFirst().getPeriods();
        if (periods.size() != 1) {
            throw new IllegalArgumentException(
                    "OpenADR 3 inbound mapping supports exactly one period but found %d."
                            .formatted(periods.size())
            );
        }

        return periods.getFirst();
    }

    private Interval interval(Point point) {
        int intervalId = requireNonNull(point.getPosition(), "point.position");
        if (intervalId < 0) {
            throw new IllegalArgumentException("Point position must be greater than or equal to zero.");
        }

        var minKiloWatt = requireNonNull(point.getMinQuantityQuantity(), "point.min_Quantity.quantity");
        var maxKiloWatt = requireNonNull(point.getMaxQuantityQuantity(), "point.max_Quantity.quantity");

        if (minKiloWatt.compareTo(maxKiloWatt) > 0) {
            throw new IllegalArgumentException(
                    "Export capacity limit must be less than or equal to import capacity limit."
            );
        }

        var exportCapacityLimit = minKiloWatt.multiply(WATT_PER_KILO_WATT);
        var importCapacityLimit = maxKiloWatt.multiply(WATT_PER_KILO_WATT);

        return new Interval(intervalId, intervalPayloads(importCapacityLimit, exportCapacityLimit));
    }

    private List<IntervalPayload> intervalPayloads(BigDecimal importCapacityLimit, BigDecimal exportCapacityLimit) {
        return List.of(
                new IntervalPayload(IMPORT_CAPACITY_LIMIT, List.of(importCapacityLimit)),
                new IntervalPayload(EXPORT_CAPACITY_LIMIT, List.of(exportCapacityLimit))
        );
    }

    private @Nullable String dateTimeOrNull(@Nullable ZonedDateTime value) {
        return value == null ? null : value.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private @Nullable String notBlankOrNull(@Nullable String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static <T> T requireNonNull(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException("Missing required field: %s.".formatted(fieldName));
        }
        return value;
    }

    private static String requireNonBlank(@Nullable String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required field: %s.".formatted(fieldName));
        }
        return value;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record OpenAdrEvent(
            @Nullable String id,
            @Nullable String createdDateTime,
            @Nullable String modificationDateTime,
            String objectType,
            String programID,
            @Nullable String eventName,
            Duration duration,
            List<EventPayloadDescriptor> payloadDescriptors,
            IntervalPeriod intervalPeriod,
            List<Interval> intervals
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record EventPayloadDescriptor(
            String objectType,
            String payloadType,
            String units
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record Interval(
            int id,
            List<IntervalPayload> payloads
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record IntervalPeriod(
            String start,
            javax.xml.datatype.Duration duration
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record IntervalPayload(
            String type,
            List<BigDecimal> values
    ) {
    }
}
