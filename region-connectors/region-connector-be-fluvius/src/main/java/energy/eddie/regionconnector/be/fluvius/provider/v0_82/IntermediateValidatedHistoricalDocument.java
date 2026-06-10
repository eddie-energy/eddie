// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.provider.v0_82;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v0_82.vhd.*;
import energy.eddie.regionconnector.be.fluvius.client.model.v3.energy.*;
import energy.eddie.regionconnector.be.fluvius.config.FluviusOAuthConfiguration;
import energy.eddie.regionconnector.be.fluvius.dtos.IdentifiableMeteringData;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpDateTime;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;
import energy.eddie.regionconnector.shared.cim.v0_82.vhd.VhdEnvelope;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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

    public @Nullable ValidatedHistoricalDataEnvelope toVHD() {
        GetEnergyResponseModelApiDataResponse meteringData = identifiableMeteredData.payload();
        if (meteringData.data().headpoint() == null) {
            return null;
        }
        return new VhdEnvelope(createVHD(meteringData), identifiableMeteredData.permissionRequest()).wrap();
    }

    private @Nullable ValidatedHistoricalDataMarketDocumentComplexType createVHD(
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
        return new ValidatedHistoricalDataMarketDocumentComplexType()
                .withMRID(UUID.randomUUID().toString())
                .withRevisionNumber(CommonInformationModelVersions.V0_82.version())
                .withType(MessageTypeList.MEASUREMENT_VALUE_DOCUMENT)
                .withCreatedDateTime(new EsmpDateTime(fetchTime).toString())
                .withSenderMarketParticipantMarketRoleType(RoleTypeList.METERING_POINT_ADMINISTRATOR)
                .withReceiverMarketParticipantMarketRoleType(RoleTypeList.CONSUMER)
                .withProcessProcessType(ProcessTypeList.REALISED)
                .withSenderMarketParticipantMRID(
                        new PartyIDStringComplexType()
                                .withCodingScheme(CodingSchemeTypeList.BELGIUM_NATIONAL_CODING_SCHEME)
                                .withValue("Fluvius")
                )
                .withReceiverMarketParticipantMRID(
                        new PartyIDStringComplexType()
                                .withCodingScheme(CodingSchemeTypeList.BELGIUM_NATIONAL_CODING_SCHEME)
                                .withValue(fluviusConfig.clientId())
                )
                .withPeriodTimeInterval(getInterval(headpoint, granularity))
                .withTimeSeriesList(
                        new ValidatedHistoricalDataMarketDocumentComplexType.TimeSeriesList()
                                .withTimeSeries(timeSeriesList)
                );
    }

    private List<TimeSeriesComplexType> createTimeSeriesList(Headpoint headpoint) {
        var tss = new ArrayList<TimeSeriesComplexType>();
        var granularity = identifiableMeteredData.permissionRequest().granularity();
        for (var meter : headpoint.allMeasurements()) {
            var measurementSlice = meter.getForGranularity(granularity);
            if (measurementSlice != null) {
                tss.addAll(createTimeSeriesList(headpoint, measurementSlice));
            }
        }
        return tss;
    }

    private List<TimeSeriesComplexType> createTimeSeriesList(
            Headpoint headpoint,
            List<MeasurementSlice> measurementSlice
    ) {
        var tss = new ArrayList<TimeSeriesComplexType>();
        for (var slice : measurementSlice) {
            tss.addAll(createTimeSeriesList(headpoint, slice));
        }
        return tss;
    }

    @SuppressWarnings("NullAway")
    private List<TimeSeriesComplexType> createTimeSeriesList(Headpoint headpoint, MeasurementSlice slice) {
        var tss = new ArrayList<TimeSeriesComplexType>();
        for (var kind : Reading.ReadingKind.values()) {
            var product = getProduct(slice, kind);
            if (product == null) {
                continue;
            }
            var directions = getFlowDirections(slice.measurements());
            for (var direction : directions) {
                var unit = getOverallUnit(slice, kind, direction);
                if (unit == null) continue;
                var ts = new TimeSeriesComplexType()
                        .withMRID(UUID.randomUUID().toString())
                        .withBusinessType(getBusinessType(direction))
                        .withProduct(product)
                        .withFlowDirectionDirection(direction)
                        .withVersion("1")
                        .withMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity(getCommodity(headpoint))
                        .withEnergyMeasurementUnitName(getUnitOfMeasure(unit))
                        .withMarketEvaluationPointMRID(new MeasurementPointIDStringComplexType()
                                                               .withCodingScheme(CodingSchemeTypeList.BELGIUM_NATIONAL_CODING_SCHEME)
                                                               .withValue(headpoint.ean()))
                        .withReasonList(
                                new TimeSeriesComplexType.ReasonList()
                                        .withReasons(new ReasonComplexType()
                                                             .withCode(ReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED))
                        )
                        .withSeriesPeriodList(
                                new TimeSeriesComplexType.SeriesPeriodList()
                                        .withSeriesPeriods(getSeriesPeriod(slice, kind, unit, direction))
                        );
                tss.add(ts);
            }
        }
        return tss;
    }

    private static @Nullable Unit getOverallUnit(
            MeasurementSlice slice,
            Reading.ReadingKind kind,
            DirectionTypeList direction
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

    private @Nullable SeriesPeriodComplexType getSeriesPeriod(
            MeasurementSlice slice,
            Reading.ReadingKind kind,
            Unit unit,
            DirectionTypeList direction
    ) {
        var points = new ArrayList<PointComplexType>();
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
                    new PointComplexType()
                            .withPosition("" + position)
                            .withEnergyQuantityQuantity(value)
                            .withEnergyQuantityQuality(getQuality(total))
            );
            position++;
        }
        var interval = new EsmpTimeInterval(slice.start(), slice.end());
        return new SeriesPeriodComplexType()
                .withResolution(getResolution())
                .withTimeInterval(new ESMPDateTimeIntervalComplexType()
                                          .withStart(interval.start())
                                          .withEnd(interval.end()))
                .withPointList(new SeriesPeriodComplexType.PointList()
                                       .withPoints(points))
                .withReasonList(new SeriesPeriodComplexType.ReasonList());
    }

    @Nullable
    private static Reading getReading(DirectionTypeList direction, Measurement measurement) {
        return switch (direction) {
            case UP -> measurement.injection();
            case DOWN -> measurement.offtake();
            default -> throw new IllegalStateException("Unexpected value: " + direction);
        };
    }

    private static @NonNull QualityTypeList getQuality(Total total) {
        return switch (total.validationState()) {
            case READ, VAL -> QualityTypeList.AS_PROVIDED;
            case EST, NVAL -> QualityTypeList.ESTIMATED;
            case null -> QualityTypeList.NOT_AVAILABLE;
        };
    }

    @Nullable
    private static UnitOfMeasureTypeList getUnitOfMeasure(Unit unit) {
        return switch (unit) {
            case UNKNOWN -> null;
            case KVA -> UnitOfMeasureTypeList.MEGAVOLTAMPERE;
            case KVARH -> UnitOfMeasureTypeList.MEGAVOLT_AMPERE_REACTIVE_HOURS;
            case KWH -> UnitOfMeasureTypeList.KILOWATT_HOUR;
            case KW -> UnitOfMeasureTypeList.KILOWATT;
            case M3 -> UnitOfMeasureTypeList.CUBIC_METRE;
        };
    }

    private static BigDecimal convertToStandardUnit(double value, Unit unit) {
        return switch (unit) {
            case UNKNOWN, KWH, KW, M3 -> BigDecimal.valueOf(value);
            case KVA, KVARH -> BigDecimal.valueOf(value).divide(BigDecimal.valueOf(1000), RoundingMode.HALF_UP);
        };
    }

    private static @NonNull CommodityKind getCommodity(Headpoint headpoint) {
        return switch (headpoint.energyType()) {
            case ELECTRICITY -> CommodityKind.ELECTRICITYPRIMARYMETERED;
            case GAS -> CommodityKind.NATURALGAS;
        };
    }

    private @Nullable EnergyProductTypeList getProduct(MeasurementSlice slice, Reading.ReadingKind kind) {
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

    private static EnergyProductTypeList toProduct(Reading.ReadingKind kind) {
        return switch (kind) {
            case TOTAL -> EnergyProductTypeList.ACTIVE_ENERGY;
            case REACTIVE -> EnergyProductTypeList.REACTIVE_ENERGY;
            case CAPACITIVE -> EnergyProductTypeList.CAPACITIVE_REACTIVE_ENERGY;
            case INDUCTIVE -> EnergyProductTypeList.INDUCTIVE_REACTIVE_ENERGY;
        };
    }

    private List<DirectionTypeList> getFlowDirections(@Nullable List<Measurement> measurements) {
        if (measurements == null) {
            return List.of();
        }
        DirectionTypeList direction = null;
        for (var measurement : measurements) {
            if (measurement.supportsOfftake()) {
                if (direction == null) {
                    direction = DirectionTypeList.DOWN;
                } else if (direction == DirectionTypeList.UP) {
                    return List.of(DirectionTypeList.UP, DirectionTypeList.DOWN);
                }
            }
            if (measurement.supportsInjection()) {
                if (direction == null) {
                    direction = DirectionTypeList.UP;
                } else if (direction == DirectionTypeList.DOWN) {
                    return List.of(DirectionTypeList.UP, DirectionTypeList.DOWN);
                }
            }
        }
        return direction == null ? List.of() : List.of(direction);
    }

    private String getResolution() {
        return identifiableMeteredData.permissionRequest().granularity().toString();
    }

    @Nullable
    private BusinessTypeList getBusinessType(@Nullable DirectionTypeList direction) {
        return switch (direction) {
            case UP -> BusinessTypeList.PRODUCTION;
            case DOWN -> BusinessTypeList.CONSUMPTION;
            case UP_AND_DOWN -> BusinessTypeList.NET_PRODUCTION__CONSUMPTION;
            case null, default -> null;
        };
    }

    private static ESMPDateTimeIntervalComplexType getInterval(
            @Nullable Headpoint headpoint,
            Granularity granularity
    ) {
        if (headpoint == null) {
            return new ESMPDateTimeIntervalComplexType();
        }
        var interval = new EsmpTimeInterval(headpoint.getEarliestMeterReading(granularity),
                                            headpoint.getLatestMeterReading(granularity));
        return new ESMPDateTimeIntervalComplexType()
                .withStart(interval.start())
                .withEnd(interval.end());
    }
}
