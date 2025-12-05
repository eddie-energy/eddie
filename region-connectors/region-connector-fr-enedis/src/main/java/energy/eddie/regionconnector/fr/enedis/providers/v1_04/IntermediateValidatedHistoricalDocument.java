package energy.eddie.regionconnector.fr.enedis.providers.v1_04;

import energy.eddie.api.utils.Pair;
import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v1_04.*;
import energy.eddie.cim.v1_04.vhd.*;
import energy.eddie.regionconnector.fr.enedis.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.dto.readings.IntervalReading;
import energy.eddie.regionconnector.fr.enedis.dto.readings.MeterReading;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableMeterReading;
import energy.eddie.regionconnector.fr.enedis.providers.agnostic.EnedisDateTime;
import energy.eddie.regionconnector.fr.enedis.providers.agnostic.EnedisResolution;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;
import energy.eddie.regionconnector.shared.cim.v1_04.VhdEnvelopeWrapper;
import jakarta.annotation.Nullable;

import javax.xml.datatype.DatatypeFactory;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;

import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnectorMetadata.ZONE_ID_FR;

public final class IntermediateValidatedHistoricalDocument {
    private final EnedisConfiguration enedisConfig;
    private final IdentifiableMeterReading identifiableMeterReading;

    IntermediateValidatedHistoricalDocument(
            IdentifiableMeterReading identifiableMeterReading,
            EnedisConfiguration enedisConfig
    ) {
        this.identifiableMeterReading = identifiableMeterReading;
        this.enedisConfig = enedisConfig;
    }

    public VHDEnvelope value() {
        var timeframe = new EsmpTimeInterval(
                meterReading().start().atStartOfDay(ZONE_ID_FR),
                meterReading().end().atStartOfDay(ZONE_ID_FR)
        );
        var vhd = new VHDMarketDocument()
                .withMRID(UUID.randomUUID().toString())
                .withRevisionNumber(CommonInformationModelVersions.V1_04.cimify())
                .withType(StandardMessageTypeList.MEASUREMENT_VALUE_DOCUMENT.value())
                .withSenderMarketParticipantMarketRoleType(StandardRoleTypeList.METERING_POINT_ADMINISTRATOR.value())
                .withReceiverMarketParticipantMarketRoleType(StandardRoleTypeList.CONSUMER.value())
                .withProcessProcessType(StandardProcessTypeList.REALISED.value())
                .withSenderMarketParticipantMRID(
                        new PartyIDString()
                                .withCodingScheme(StandardCodingSchemeTypeList.FRANCE_NATIONAL_CODING_SCHEME.value())
                                .withValue("ENEDIS")
                )
                .withCreatedDateTime(ZonedDateTime.now(ZONE_ID_FR))
                .withReceiverMarketParticipantMRID(
                        new PartyIDString()
                                .withCodingScheme(StandardCodingSchemeTypeList.FRANCE_NATIONAL_CODING_SCHEME.value())
                                .withValue(enedisConfig.clientId())
                )
                .withPeriodTimeInterval(
                        new ESMPDateTimeInterval()
                                .withStart(timeframe.start())
                                .withEnd(timeframe.end())
                )
                .withTimeSeries(timeSeries());
        FrEnedisPermissionRequest permissionRequest = identifiableMeterReading.permissionRequest();
        return new VhdEnvelopeWrapper(vhd, permissionRequest).wrap();
    }

    private MeterReading meterReading() {
        return this.identifiableMeterReading.meterReading();
    }

    private TimeSeries timeSeries() {
        TimeSeries reading = new TimeSeries()
                .withMRID(UUID.randomUUID().toString())
                .withBusinessType(businessType().value())
                .withProduct(energyProductTypeList())
                .withVersion("1")
                .withFlowDirectionDirection(direction().value())
                .withMarketEvaluationPointMeterReadingsReadingsReadingTypeAggregate(aggregateKind())
                .withMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity(CommodityKind.ELECTRICITYPRIMARYMETERED)
                .withMarketEvaluationPointMRID(
                        new MeasurementPointIDString()
                                .withCodingScheme(StandardCodingSchemeTypeList.FRANCE_NATIONAL_CODING_SCHEME.value())
                                .withValue(meterReading().usagePointId())
                )
                .withReasonCode(StandardReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED.value());

        return switch (meterReading().readingType().unit()) {
            case "W", "VA" -> reading.withEnergyMeasurementUnitName(StandardUnitOfMeasureTypeList.WATT.value())
                                     .withPeriods(seriesPeriods(false));
            case "Wh" -> reading.withEnergyMeasurementUnitName(StandardUnitOfMeasureTypeList.KILOWATT_HOUR.value())
                                .withPeriods(seriesPeriods(true));
            default -> reading.withPeriods(seriesPeriods(false));
        };
    }

    private StandardBusinessTypeList businessType() {
        return switch (identifiableMeterReading.meterReadingType()) {
            case CONSUMPTION -> StandardBusinessTypeList.CONSUMPTION;
            case PRODUCTION -> StandardBusinessTypeList.PRODUCTION;
        };
    }

    @Nullable
    private String energyProductTypeList() {
        return switch (meterReading().readingType().measurementKind()) {
            case "power" -> StandardEnergyProductTypeList.ACTIVE_POWER.value();
            case "energy" -> StandardEnergyProductTypeList.ACTIVE_ENERGY.value();
            default -> null;
        };
    }

    private StandardDirectionTypeList direction() {
        return switch (identifiableMeterReading.meterReadingType()) {
            case CONSUMPTION -> StandardDirectionTypeList.DOWN;
            case PRODUCTION -> StandardDirectionTypeList.UP;
        };
    }

    @Nullable
    private AggregateKind aggregateKind() {
        String aggregate = meterReading().readingType().aggregate();
        return toEnum(aggregate.toUpperCase(Locale.ROOT), AggregateKind::valueOf);
    }

    private List<SeriesPeriod> seriesPeriods(boolean convertToKiloWatt) {
        var pointsWithResolutions = batchIntoChunks(convertToKiloWatt);

        List<SeriesPeriod> seriesPeriods = new ArrayList<>();
        for (PointsWithResolution pointsWithResolution : pointsWithResolutions) {
            EnedisResolution duration = EnedisResolution.valueOf(pointsWithResolution.resolution());
            ZonedDateTime start = new EnedisDateTime(pointsWithResolution.start()).toZonedDateTime();
            // We need to subtract the resolution from the start date for load curves (date represents the end of the measurement)
            if (start != null && duration.granularity().minutes() <= 60) {
                start = start.minusMinutes(duration.granularity().minutes());
            }
            ZonedDateTime end = new EnedisDateTime(pointsWithResolution.end()).toZonedDateTime();
            // for daily resolution, we need to adjust the end date (date represents the whole day)
            if (end != null && duration == EnedisResolution.P1D) {
                end = end.plusMinutes(duration.granularity().minutes());
            }
            var interval = new EsmpTimeInterval(start, end);
            var seriesPeriod = new SeriesPeriod()
                    .withResolution(DatatypeFactory.newDefaultInstance()
                                                   .newDuration(duration.granularity().duration().toMillis()))
                    .withTimeInterval(new ESMPDateTimeInterval()
                                              .withStart(interval.start())
                                              .withEnd(interval.end())
                    )
                    .withPoints(pointsWithResolution.points());
            seriesPeriods.add(seriesPeriod);
        }
        return seriesPeriods;
    }

    private List<PointsWithResolution> batchIntoChunks(boolean convertToKiloWatt) {
        if (meterReading().intervalReadings().isEmpty()) {
            return List.of();
        }
        var cur = meterReading().intervalReadings().getFirst();
        var prevResolution = cur.intervalLength().orElse("P1D");
        var position = 1;
        var currentChunk = new ArrayList<>(List.of(new Pair<>(position++, cur)));
        var chunks = new ArrayList<PointsWithResolution>();
        for (int i = 1; i < meterReading().intervalReadings().size(); i++) {
            cur = meterReading().intervalReadings().get(i);
            // Different interval length
            // Start new chunk
            if (!cur.intervalLength().map(prevResolution::equals).orElse(true)) {
                addChunk(convertToKiloWatt, chunks, currentChunk, prevResolution);
                currentChunk = new ArrayList<>();
                position = 1;
                prevResolution = cur.intervalLength().orElse("P1D");
            }
            currentChunk.add(new Pair<>(position++, cur));
        }
        addChunk(convertToKiloWatt, chunks, currentChunk, prevResolution);
        return chunks;
    }

    private void addChunk(
            boolean convertToKiloWatt,
            List<PointsWithResolution> chunks,
            List<Pair<Integer, IntervalReading>> currentChunk,
            String prevResolution
    ) {
        chunks.add(new PointsWithResolution(currentChunk.stream()
                                                        .map(p -> toPoint(p, convertToKiloWatt))
                                                        .toList(),
                                            prevResolution,
                                            currentChunk.getFirst().value().date(),
                                            currentChunk.getLast().value().date()));
    }

    private Point toPoint(Pair<Integer, IntervalReading> intervalReading, boolean convertToKiloWatt) {
        var quantity = Double.parseDouble(intervalReading.value().value());
        if (convertToKiloWatt) {
            quantity = quantity / 1000.0;
        }
        return new Point()
                .withPosition(intervalReading.key())
                .withEnergyQuantityQuantity(BigDecimal.valueOf(quantity))
                .withEnergyQuantityQuality(StandardQualityTypeList.AS_PROVIDED.value());
    }


    @Nullable
    private static <T> T toEnum(@Nullable String value, Function<String, T> converter) {
        try {
            return converter.apply(value);
        } catch (Exception e) {
            return null;
        }
    }
}
