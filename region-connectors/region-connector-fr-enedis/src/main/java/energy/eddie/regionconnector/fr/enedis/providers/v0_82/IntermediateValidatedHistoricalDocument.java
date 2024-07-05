package energy.eddie.regionconnector.fr.enedis.providers.v0_82;

import energy.eddie.api.CommonInformationModelVersions;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0_82.cim.EddieValidatedHistoricalDataMarketDocument;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.*;
import energy.eddie.regionconnector.fr.enedis.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.client.EnedisApiVersion;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.dto.IntervalReading;
import energy.eddie.regionconnector.fr.enedis.dto.MeterReading;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableMeterReading;
import energy.eddie.regionconnector.shared.utils.EsmpDateTime;
import energy.eddie.regionconnector.shared.utils.EsmpTimeInterval;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.*;

import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnectorMetadata.ZONE_ID_FR;

public final class IntermediateValidatedHistoricalDocument {
    public static final DateTimeFormatter ENEDIS_DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("yyyy-MM-dd")
            .optionalStart()
            .appendPattern(" HH:mm:ss")
            .optionalEnd()
            .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
            .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
            .toFormatter();
    private static final TimeSeriesComplexType.ReasonList REASON_LIST = new TimeSeriesComplexType.ReasonList()
            .withReasons(
                    new ReasonComplexType()
                            .withCode(ReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED)
            );
    private final ValidatedHistoricalDataMarketDocument vhd = new ValidatedHistoricalDataMarketDocument()
            .withMRID(UUID.randomUUID().toString())
            .withRevisionNumber(CommonInformationModelVersions.V0_82.version())
            .withType(MessageTypeList.MEASUREMENT_VALUE_DOCUMENT)
            .withSenderMarketParticipantMarketRoleType(RoleTypeList.METERING_POINT_ADMINISTRATOR)
            .withReceiverMarketParticipantMarketRoleType(RoleTypeList.CONSUMER)
            .withProcessProcessType(ProcessTypeList.REALISED)
            .withSenderMarketParticipantMRID(
                    new PartyIDStringComplexType()
                            .withCodingScheme(CodingSchemeTypeList.FRANCE_NATIONAL_CODING_SCHEME)
                            .withValue("ENEDIS") // No Mapping
            );
    private final CommonInformationModelConfiguration cimConfig;
    private final EnedisConfiguration enedisConfig;
    private final IdentifiableMeterReading identifiableMeterReading;

    IntermediateValidatedHistoricalDocument(
            IdentifiableMeterReading identifiableMeterReading,
            CommonInformationModelConfiguration cimConfig,
            EnedisConfiguration enedisConfig
    ) {
        this.identifiableMeterReading = identifiableMeterReading;
        this.cimConfig = cimConfig;
        this.enedisConfig = enedisConfig;
    }

    public EddieValidatedHistoricalDataMarketDocument eddieValidatedHistoricalDataMarketDocument() {
        var timeframe = new EsmpTimeInterval(
                meterReading().start().atStartOfDay(ZONE_ID_FR),
                meterReading().end().atStartOfDay(ZONE_ID_FR)
        );
        vhd
                .withCreatedDateTime(EsmpDateTime.now().toString())
                .withReceiverMarketParticipantMRID(
                        new PartyIDStringComplexType()
                                .withCodingScheme(cimConfig.eligiblePartyNationalCodingScheme())
                                .withValue(enedisConfig.clientId())
                )
                .withPeriodTimeInterval(
                        new ESMPDateTimeIntervalComplexType()
                                .withStart(timeframe.start())
                                .withEnd(timeframe.end())
                )
                .withTimeSeriesList(timeSeriesList());
        FrEnedisPermissionRequest permissionRequest = identifiableMeterReading.permissionRequest();
        return new EddieValidatedHistoricalDataMarketDocument(
                permissionRequest.connectionId(),
                permissionRequest.permissionId(),
                permissionRequest.dataNeedId(),
                vhd
        );
    }

    private MeterReading meterReading() {
        return this.identifiableMeterReading.meterReading();
    }

    private ValidatedHistoricalDataMarketDocument.TimeSeriesList timeSeriesList() {
        TimeSeriesComplexType reading = new TimeSeriesComplexType()
                .withMRID(UUID.randomUUID().toString())
                .withBusinessType(businessType())
                .withProduct(energyProductTypeList())
                .withVersion(EnedisApiVersion.V5.name())
                .withFlowDirectionDirection(direction())
                .withMarketEvaluationPointMeterReadingsReadingsReadingTypeAggregation(aggregateKind())
                .withMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity(CommodityKind.ELECTRICITYPRIMARYMETERED)
                .withMarketEvaluationPointMRID(
                        new MeasurementPointIDStringComplexType()
                                .withCodingScheme(CodingSchemeTypeList.FRANCE_NATIONAL_CODING_SCHEME)
                                .withValue(meterReading().usagePointId())
                )
                .withReasonList(REASON_LIST);

        reading = switch (meterReading().readingType().unit()) {
            case "W", "VA" -> reading.withEnergyMeasurementUnitName(UnitOfMeasureTypeList.WATT)
                                     .withSeriesPeriodList(seriesPeriods(false));
            case "Wh" -> reading.withEnergyMeasurementUnitName(UnitOfMeasureTypeList.KILOWATT_HOUR)
                                .withSeriesPeriodList(seriesPeriods(true));
            default -> reading.withSeriesPeriodList(seriesPeriods(false));
        };

        return new ValidatedHistoricalDataMarketDocument.TimeSeriesList()
                .withTimeSeries(List.of(reading));
    }

    private BusinessTypeList businessType() {
        return switch (identifiableMeterReading.meterReadingType()) {
            case CONSUMPTION -> BusinessTypeList.CONSUMPTION;
            case PRODUCTION -> BusinessTypeList.PRODUCTION;
        };
    }

    private EnergyProductTypeList energyProductTypeList() {
        return switch (meterReading().readingType().measurementKind()) {
            case "power" -> EnergyProductTypeList.ACTIVE_POWER;
            case "energy" -> EnergyProductTypeList.ACTIVE_ENERGY;
            default -> throw new IllegalStateException("Unknown measurement kind '%s'".formatted(
                    meterReading().readingType().measurementKind())
            );
        };
    }

    private DirectionTypeList direction() {
        return switch (identifiableMeterReading.meterReadingType()) {
            case CONSUMPTION -> DirectionTypeList.DOWN;
            case PRODUCTION -> DirectionTypeList.UP;
        };
    }

    private AggregateKind aggregateKind() {
        String aggregate = meterReading().readingType().aggregate();
        return AggregateKind.valueOf(aggregate.toUpperCase(Locale.ROOT));
    }

    private TimeSeriesComplexType.SeriesPeriodList seriesPeriods(boolean convertToKiloWatt) {
        var meterReading = meterReading();
        String currentResolution = meterReading.intervalReadings().getFirst().intervalLength().orElse("P1D");
        List<PointsWithResolution> pointsWithResolutions = new ArrayList<>();
        pointsWithResolutions.add(new PointsWithResolution(new ArrayList<>(),
                                                           currentResolution,
                                                           meterReading.intervalReadings().getFirst().date()));
        int position = 0;
        int pointsWithResolutionIndex = 0;
        for (IntervalReading intervalReading
                : meterReading.intervalReadings()) {
            // if resolution changed we need to create a new list of points
            if (intervalReading.intervalLength().isPresent() &&
                !Objects.equals(intervalReading.intervalLength().get(), currentResolution)) {
                position = 0;
                pointsWithResolutionIndex++;
                currentResolution = intervalReading.intervalLength().get();
                pointsWithResolutions.add(new PointsWithResolution(
                        new ArrayList<>(),
                        currentResolution,
                        intervalReading.date()
                ));
            }

            var quantity = Double.parseDouble(intervalReading.value());
            if (convertToKiloWatt) {
                quantity = quantity / 1000.0;
            }
            PointComplexType point = new PointComplexType()
                    .withPosition("%d".formatted(position))
                    .withEnergyQuantityQuantity(BigDecimal.valueOf(quantity))
                    .withEnergyQuantityQuality(QualityTypeList.AS_PROVIDED);
            pointsWithResolutions.get(pointsWithResolutionIndex).points().add(point);
            pointsWithResolutions.get(pointsWithResolutionIndex).setEnd(intervalReading.date());
            position++;
        }

        List<SeriesPeriodComplexType> seriesPeriods = new ArrayList<>();
        for (PointsWithResolution pointsWithResolution : pointsWithResolutions) {
            EnedisResolution duration = EnedisResolution.valueOf(pointsWithResolution.resolution());
            ZonedDateTime start = convertEnedisDateToZonedDateTime(pointsWithResolution.start());
            // We need to subtract the resolution from the start date for load curves (date represents the end of the measurement)
            if (start != null && duration.granularity.minutes() <= 60) {
                start = start.minusMinutes(duration.granularity.minutes());
            }
            ZonedDateTime end = convertEnedisDateToZonedDateTime(pointsWithResolution.end());
            // for daily resolution we need to adjust the end date (date represents the whole day)
            if (end != null && duration == EnedisResolution.P1D) {
                end = end.plusMinutes(duration.granularity.minutes());
            }
            var interval = new EsmpTimeInterval(start, end);
            var seriesPeriod = new SeriesPeriodComplexType()
                    .withResolution(duration.granularity().name())
                    .withTimeInterval(new ESMPDateTimeIntervalComplexType()
                                              .withStart(interval.start())
                                              .withEnd(interval.end())
                    )
                    .withPointList(
                            new SeriesPeriodComplexType.PointList()
                                    .withPoints(pointsWithResolution.points())
                    );
            seriesPeriods.add(seriesPeriod);
        }

        return new TimeSeriesComplexType.SeriesPeriodList()
                .withSeriesPeriods(seriesPeriods);
    }

    private @Nullable ZonedDateTime convertEnedisDateToZonedDateTime(@Nullable String date) {
        if (date == null) {
            return null;
        }

        try {
            LocalDateTime localDateTime = LocalDateTime.parse(date, ENEDIS_DATE_TIME_FORMATTER);
            return localDateTime.atZone(ZONE_ID_FR).withZoneSameInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private enum EnedisResolution {
        PT10M(Granularity.PT10M),
        PT15M(Granularity.PT15M),
        PT30M(Granularity.PT30M),
        PT60M(Granularity.PT1H),
        P1D(Granularity.P1D);

        private final Granularity granularity;

        EnedisResolution(Granularity iso8601) {
            this.granularity = iso8601;
        }

        public Granularity granularity() {
            return granularity;
        }
    }

    private static final class PointsWithResolution {
        private final List<PointComplexType> points;
        private final String resolution;
        private final String start;
        private @Nullable String end;

        private PointsWithResolution(
                List<PointComplexType> points,
                String resolution,
                String start
        ) {
            this.points = points;
            this.resolution = resolution;
            this.start = start;
        }

        public List<PointComplexType> points() {return points;}

        public String resolution() {return resolution;}

        public String start() {return start;}

        public @Nullable String end() {
            return end;
        }

        public void setEnd(String end) {
            this.end = end;
        }
    }
}
