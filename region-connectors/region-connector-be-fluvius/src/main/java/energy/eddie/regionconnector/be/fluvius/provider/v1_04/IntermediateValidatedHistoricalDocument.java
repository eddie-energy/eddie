// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.provider.v1_04;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v1_04.*;
import energy.eddie.cim.v1_04.vhd.*;
import energy.eddie.regionconnector.be.fluvius.client.model.v3.energy.*;
import energy.eddie.regionconnector.be.fluvius.config.FluviusOAuthConfiguration;
import energy.eddie.regionconnector.be.fluvius.dtos.IdentifiableMeteringData;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;
import energy.eddie.regionconnector.shared.cim.v1_04.VhdEnvelopeWrapper;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class IntermediateValidatedHistoricalDocument {
    private final FluviusOAuthConfiguration fluviusConfig;
    private final IdentifiableMeteringData identifiableMeteredData;

    IntermediateValidatedHistoricalDocument(
            FluviusOAuthConfiguration fluviusConfiguration,
            IdentifiableMeteringData identifiableMeteredData
    ) {
        this.fluviusConfig = fluviusConfiguration;
        this.identifiableMeteredData = identifiableMeteredData;
    }

    public @Nullable VHDEnvelope toVHD() {
        GetEnergyResponseModelApiDataResponse meteringData = identifiableMeteredData.payload();
        if (meteringData.data().headpoint() == null) {
            return null;
        }
        return new VhdEnvelopeWrapper(createVHD(meteringData), identifiableMeteredData.permissionRequest()).wrap();
    }

    private @Nullable VHDMarketDocument createVHD(
            GetEnergyResponseModelApiDataResponse getEnergyResponseModel
    ) {
        var fetchTime = getEnergyResponseModel.metaData().fetchTime() == null
                ? ZonedDateTime.now(ZoneOffset.UTC)
                : getEnergyResponseModel.metaData().fetchTime();
        var headpoint = getEnergyResponseModel.data().headpoint();
        var granularity = identifiableMeteredData.permissionRequest().granularity();
        if (headpoint == null) {
            return null;
        }
        var timeSeriesList = createTimeSeriesList(headpoint);
        if (timeSeriesList.isEmpty()) {
            return null;
        }
        return new VHDMarketDocument()
                .withMRID(UUID.randomUUID().toString())
                .withRevisionNumber(CommonInformationModelVersions.V1_04.cimify())
                .withType(StandardDocumentTypeList.MEASUREMENT_VALUE_DOCUMENT.value())
                .withCreatedDateTime(fetchTime)
                .withSenderMarketParticipantMarketRoleType(StandardRoleTypeList.METERING_POINT_ADMINISTRATOR.value())
                .withReceiverMarketParticipantMarketRoleType(StandardRoleTypeList.CONSUMER.value())
                .withProcessProcessType(StandardProcessTypeList.REALISED.value())
                .withSenderMarketParticipantMRID(
                        new PartyIDString()
                                .withCodingScheme(StandardCodingSchemeTypeList.BELGIUM_NATIONAL_CODING_SCHEME.value())
                                .withValue("Fluvius")
                )
                .withReceiverMarketParticipantMRID(
                        new PartyIDString()
                                .withCodingScheme(StandardCodingSchemeTypeList.BELGIUM_NATIONAL_CODING_SCHEME.value())
                                .withValue(fluviusConfig.clientId())
                )
                .withPeriodTimeInterval(getInterval(headpoint, granularity))
                .withTimeSeries(timeSeriesList);
    }

    private List<TimeSeries> createTimeSeriesList(Headpoint headpoint) {
        var tss = new ArrayList<TimeSeries>();
        var granularity = identifiableMeteredData.permissionRequest().granularity();
        for (var meter : headpoint.allMeasurements()) {
            var measurementSlice = meter.getForGranularity(granularity);
            if (measurementSlice != null) {
                tss.addAll(createTimeSeriesList(headpoint, measurementSlice));
            }
        }
        return tss;
    }

    private List<TimeSeries> createTimeSeriesList(
            Headpoint headpoint,
            List<MeasurementSlice> measurementSlice
    ) {
        var tss = new ArrayList<TimeSeries>();
        for (var slice : measurementSlice) {
            tss.addAll(createTimeSeriesList(headpoint, slice));
        }
        return tss;
    }

    @SuppressWarnings("NullAway")
    private List<TimeSeries> createTimeSeriesList(Headpoint headpoint, MeasurementSlice slice) {
        var tss = new ArrayList<TimeSeries>();
        for (var kind : Reading.ReadingKind.values()) {
            var product = getProduct(slice, kind);
            if (product == null) {
                continue;
            }
            var directions = getFlowDirections(slice.measurements());
            for (var direction : directions) {
                var unit = getOverallUnit(slice, kind, direction);
                if (unit == null) continue;
                var ts = new TimeSeries()
                        .withMRID(UUID.randomUUID().toString())
                        .withBusinessType(getBusinessType(direction))
                        .withProduct(product.value())
                        .withFlowDirectionDirection(direction.value())
                        .withVersion("1")
                        .withMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity(getCommodity(headpoint))
                        .withEnergyMeasurementUnitName(getUnitOfMeasure(unit))
                        .withMarketEvaluationPointMRID(new MeasurementPointIDString()
                                                               .withCodingScheme(StandardCodingSchemeTypeList.BELGIUM_NATIONAL_CODING_SCHEME.value())
                                                               .withValue(headpoint.ean()))
                        .withReasonCode(StandardReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED.value())
                        .withPeriods(getSeriesPeriod(slice, kind, unit, direction));
                tss.add(ts);
            }
        }
        return tss;
    }

    private static @Nullable Unit getOverallUnit(
            MeasurementSlice slice,
            Reading.ReadingKind kind,
            StandardDirectionTypeList direction
    ) {
        if (slice.measurements() == null
            || slice.measurements().isEmpty()
            || slice.measurements().getFirst() == null) {
            return null;
        }
        var reading = getReading(direction, slice.measurements().getFirst());
        if (reading == null) {
            return null;
        }
        var total = reading.value(kind);
        if (total == null) {
            return null;
        }
        return total.unit();
    }

    private @Nullable SeriesPeriod getSeriesPeriod(
            MeasurementSlice slice,
            Reading.ReadingKind kind,
            Unit unit,
            StandardDirectionTypeList direction
    ) {
        var points = new ArrayList<Point>();
        var position = 1;
        if (slice.measurements() == null) {
            return null;
        }
        for (var measurement : slice.measurements()) {
            var reading = getReading(direction, measurement);
            var fallbackTotal = new Total(0.0, unit, null, null);
            reading = reading == null ? new Reading(fallbackTotal) : reading;
            var total = reading.value(kind);
            total = total == null ? fallbackTotal : total;
            var value = total.value() == null
                    ? BigDecimal.ZERO
                    : convertToStandardUnit(total.value(), unit);
            points.add(
                    new Point()
                            .withPosition(position)
                            .withEnergyQuantityQuantity(value)
                            .withEnergyQuantityQuality(getQuality(total).value())
            );
            position++;
        }
        var interval = new EsmpTimeInterval(slice.start(), slice.end());
        return new SeriesPeriod()
                .withResolution(getResolution())
                .withTimeInterval(new ESMPDateTimeInterval()
                                          .withStart(interval.start())
                                          .withEnd(interval.end()))
                .withPoints(points)
                .withReasonCode(StandardReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED.value());
    }

    @Nullable
    private static Reading getReading(StandardDirectionTypeList direction, Measurement measurement) {
        return switch (direction) {
            case UP -> measurement.injection();
            case DOWN -> measurement.offtake();
            default -> throw new IllegalStateException("Unexpected value: " + direction);
        };
    }

    private static @NonNull StandardQualityTypeList getQuality(Total total) {
        return switch (total.validationState()) {
            case READ, VAL -> StandardQualityTypeList.AS_PROVIDED;
            case EST, NVAL -> StandardQualityTypeList.ESTIMATED;
            case null -> StandardQualityTypeList.NOT_AVAILABLE;
        };
    }

    @Nullable
    private static String getUnitOfMeasure(Unit unit) {
        return switch (unit) {
            case UNKNOWN -> null;
            case KVA -> StandardUnitOfMeasureTypeList.MEGAVOLTAMPERE.value();
            case KVARH -> StandardUnitOfMeasureTypeList.MEGAVOLT_AMPERE_REACTIVE_HOURS.value();
            case KWH -> StandardUnitOfMeasureTypeList.KILOWATT_HOUR.value();
            case KW -> StandardUnitOfMeasureTypeList.KILOWATT.value();
            case M3 -> StandardUnitOfMeasureTypeList.CUBIC_METRE.value();
        };
    }

    private static BigDecimal convertToStandardUnit(double value, Unit unit) {
        return switch (unit) {
            case UNKNOWN, KWH, KW, M3 -> BigDecimal.valueOf(value);
            case KVA, KVARH -> BigDecimal.valueOf(value).divide(BigDecimal.valueOf(1000), RoundingMode.HALF_UP);
        };
    }

    private static energy.eddie.cim.v1_04.vhd.CommodityKind getCommodity(Headpoint headpoint) {
        return switch (headpoint.energyType()) {
            case ELECTRICITY -> CommodityKind.ELECTRICITYPRIMARYMETERED;
            case GAS -> CommodityKind.NATURALGAS;
        };
    }

    private @Nullable StandardEnergyProductTypeList getProduct(MeasurementSlice slice, Reading.ReadingKind kind) {
        if (slice.measurements() == null) {
            return null;
        }
        for (var measurement : slice.measurements()) {
            if (readingSupportsKind(kind, measurement.injection())
                || readingSupportsKind(kind, measurement.offtake())) {
                return toProduct(kind);
            }
        }
        return null;
    }

    private static boolean readingSupportsKind(Reading.ReadingKind kind, @Nullable Reading reading) {
        return reading != null && reading.value(kind) != null;
    }

    private static StandardEnergyProductTypeList toProduct(Reading.ReadingKind kind) {
        return switch (kind) {
            case TOTAL -> StandardEnergyProductTypeList.ACTIVE_ENERGY;
            case REACTIVE -> StandardEnergyProductTypeList.REACTIVE_ENERGY;
            case CAPACITIVE -> StandardEnergyProductTypeList.CAPACITIVE_REACTIVE_ENERGY;
            case INDUCTIVE -> StandardEnergyProductTypeList.INDUCTIVE_REACTIVE_ENERGY;
        };
    }

    private List<StandardDirectionTypeList> getFlowDirections(@Nullable List<Measurement> measurements) {
        if (measurements == null) {
            return List.of();
        }
        StandardDirectionTypeList direction = null;
        for (var measurement : measurements) {
            if (measurement.supportsOfftake()) {
                if (direction == null) {
                    direction = StandardDirectionTypeList.DOWN;
                } else if (direction == StandardDirectionTypeList.UP) {
                    return List.of(StandardDirectionTypeList.UP, StandardDirectionTypeList.DOWN);
                }
            }
            if (measurement.supportsInjection()) {
                if (direction == null) {
                    direction = StandardDirectionTypeList.UP;
                } else if (direction == StandardDirectionTypeList.DOWN) {
                    return List.of(StandardDirectionTypeList.UP, StandardDirectionTypeList.DOWN);
                }
            }
        }
        return direction == null ? List.of() : List.of(direction);
    }

    private Duration getResolution() {
        return DatatypeFactory.newDefaultInstance()
                              .newDuration(identifiableMeteredData.permissionRequest()
                                                                  .granularity()
                                                                  .duration()
                                                                  .toMillis());
    }

    @Nullable
    private String getBusinessType(@Nullable StandardDirectionTypeList direction) {
        return switch (direction) {
            case UP -> StandardBusinessTypeList.PRODUCTION.value();
            case DOWN -> StandardBusinessTypeList.CONSUMPTION.value();
            case UP_AND_DOWN -> StandardBusinessTypeList.NET_PRODUCTION__CONSUMPTION.value();
            case null, default -> null;
        };
    }

    private static ESMPDateTimeInterval getInterval(
            @Nullable Headpoint headpoint,
            Granularity granularity
    ) {
        if (headpoint == null) {
            return new ESMPDateTimeInterval();
        }
        var interval = new EsmpTimeInterval(headpoint.getEarliestMeterReading(granularity),
                                            headpoint.getLatestMeterReading(granularity));
        return new ESMPDateTimeInterval()
                .withStart(interval.start())
                .withEnd(interval.end());
    }
}
