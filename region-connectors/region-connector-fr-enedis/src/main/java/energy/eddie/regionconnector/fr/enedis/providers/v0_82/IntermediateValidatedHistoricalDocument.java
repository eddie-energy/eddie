// SPDX-FileCopyrightText: 2023-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.providers.v0_82;

import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.utils.Pair;
import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v0_82.vhd.*;
import energy.eddie.regionconnector.fr.enedis.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.client.EnedisApiVersion;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.dto.readings.IntervalReading;
import energy.eddie.regionconnector.fr.enedis.dto.readings.MeterReading;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableMeterReading;
import energy.eddie.regionconnector.fr.enedis.providers.agnostic.EnedisDateTime;
import energy.eddie.regionconnector.fr.enedis.providers.agnostic.EnedisResolution;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpDateTime;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;
import energy.eddie.regionconnector.shared.cim.v0_82.vhd.VhdEnvelope;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnectorMetadata.ZONE_ID_FR;

public final class IntermediateValidatedHistoricalDocument {
    private static final TimeSeriesComplexType.ReasonList REASON_LIST = new TimeSeriesComplexType.ReasonList()
            .withReasons(
                    new ReasonComplexType()
                            .withCode(ReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED)
            );
    private final ValidatedHistoricalDataMarketDocumentComplexType vhd = new ValidatedHistoricalDataMarketDocumentComplexType()
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

    public ValidatedHistoricalDataEnvelope eddieValidatedHistoricalDataMarketDocument() {
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
        return new VhdEnvelope(vhd, permissionRequest).wrap();
    }

    private MeterReading meterReading() {
        return this.identifiableMeterReading.meterReading();
    }

    private ValidatedHistoricalDataMarketDocumentComplexType.TimeSeriesList timeSeriesList() {
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

        return new ValidatedHistoricalDataMarketDocumentComplexType.TimeSeriesList()
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
        var pointsWithResolutions = batchIntoChunks(convertToKiloWatt);

        List<SeriesPeriodComplexType> seriesPeriods = new ArrayList<>();
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
                                                        .map(p -> map(p, convertToKiloWatt))
                                                        .toList(),
                                            prevResolution,
                                            currentChunk.getFirst().value().date(),
                                            currentChunk.getLast().value().date()));
    }

    private PointComplexType map(Pair<Integer, IntervalReading> intervalReading, boolean convertToKiloWatt) {
        var quantity = Double.parseDouble(intervalReading.value().value());
        if (convertToKiloWatt) {
            quantity = quantity / 1000.0;
        }
        return new PointComplexType()
                .withPosition(Integer.toString(intervalReading.key()))
                .withEnergyQuantityQuantity(BigDecimal.valueOf(quantity))
                .withEnergyQuantityQuality(QualityTypeList.AS_PROVIDED);
    }
}
