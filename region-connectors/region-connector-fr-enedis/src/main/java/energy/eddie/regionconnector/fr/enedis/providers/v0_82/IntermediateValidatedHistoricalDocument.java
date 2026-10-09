// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.providers.v0_82;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.utils.Pair;
import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v0_82.vhd.*;
import energy.eddie.regionconnector.fr.enedis.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.dto.readings.MeterReading;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableMeterReading;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpDateTime;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;
import energy.eddie.regionconnector.shared.cim.v0_82.vhd.VhdEnvelope;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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
                meterReading().period().start().atStartOfDay(ZONE_ID_FR),
                meterReading().period().end().atStartOfDay(ZONE_ID_FR)
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
        return this.identifiableMeterReading.payload();
    }

    private ValidatedHistoricalDataMarketDocumentComplexType.TimeSeriesList timeSeriesList() {
        var timeSeries = new ArrayList<TimeSeriesComplexType>();
        for (var reading : meterReading().readings()) {
            var ts = new TimeSeriesComplexType()
                    .withMRID(UUID.randomUUID().toString())
                    .withBusinessType(businessType(reading))
                    .withProduct(energyProductTypeList(reading))
                    .withVersion("1")
                    .withFlowDirectionDirection(direction(reading))
                    .withMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity(CommodityKind.ELECTRICITYPRIMARYMETERED)
                    .withMarketEvaluationPointMRID(
                            new MeasurementPointIDStringComplexType()
                                    .withCodingScheme(CodingSchemeTypeList.FRANCE_NATIONAL_CODING_SCHEME)
                                    .withValue(meterReading().usagePointId())
                    )
                    .withReasonList(REASON_LIST);
            switch (reading.unit()) {
                case "W", "VA" -> ts.withEnergyMeasurementUnitName(UnitOfMeasureTypeList.WATT)
                                    .withSeriesPeriodList(seriesPeriods(reading, false));
                case "Wh" -> ts.withEnergyMeasurementUnitName(UnitOfMeasureTypeList.KILOWATT_HOUR)
                               .withSeriesPeriodList(seriesPeriods(reading, true));
                default -> ts.withSeriesPeriodList(seriesPeriods(reading, false));
            }
            timeSeries.add(ts);
        }
        return new ValidatedHistoricalDataMarketDocumentComplexType.TimeSeriesList()
                .withTimeSeries(timeSeries);
    }

    private BusinessTypeList businessType(MeterReading.Reading reading) {
        return switch (reading.direction()) {
            case CONSUMPTION -> BusinessTypeList.CONSUMPTION;
            case PRODUCTION -> BusinessTypeList.PRODUCTION;
        };
    }

    @Nullable
    private EnergyProductTypeList energyProductTypeList(MeterReading.Reading reading) {
        return switch (reading.unit()) {
            case "W", "VA" -> EnergyProductTypeList.ACTIVE_POWER;
            case "Wh" -> EnergyProductTypeList.ACTIVE_ENERGY;
            default -> null;
        };
    }

    private DirectionTypeList direction(MeterReading.Reading reading) {
        return switch (reading.direction()) {
            case CONSUMPTION -> DirectionTypeList.DOWN;
            case PRODUCTION -> DirectionTypeList.UP;
        };
    }

    private TimeSeriesComplexType.SeriesPeriodList seriesPeriods(
            MeterReading.Reading reading,
            boolean convertToKiloWatt
    ) {
        var pointsWithResolutions = batchIntoChunks(reading, convertToKiloWatt);

        List<SeriesPeriodComplexType> seriesPeriods = new ArrayList<>();
        for (PointsWithResolution pointsWithResolution : pointsWithResolutions) {
            var resolution = pointsWithResolution.resolution();
            var start = pointsWithResolution.start();
            // We need to subtract the resolution from the start date for load curves (date represents the end of the measurement)
            if (resolution.toMinutes() <= 60) {
                start = start.minusMinutes(resolution.toMinutes());
            }
            var end = pointsWithResolution.end();
            // for daily resolution, we need to adjust the end date (date represents the whole day)
            if (resolution.equals(Granularity.P1D.duration())) {
                end = end.plusMinutes(resolution.toMinutes());
            }
            var interval = new EsmpTimeInterval(start, end);
            var seriesPeriod = new SeriesPeriodComplexType()
                    .withResolution(resolution.toString())
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

    private List<PointsWithResolution> batchIntoChunks(MeterReading.Reading reading, boolean convertToKiloWatt) {
        var points = reading.points();
        if (points.isEmpty()) {
            return List.of();
        }
        var prevResolution = resolutionOf(points.getFirst());
        var position = 1;
        var currentChunk = new ArrayList<>(List.of(new Pair<>(position++, points.getFirst())));
        var chunks = new ArrayList<PointsWithResolution>();
        for (int i = 1; i < points.size(); i++) {
            var cur = points.get(i);
            var curResolution = resolutionOf(cur);
            // Different interval length
            // Start new chunk
            if (!curResolution.equals(prevResolution)) {
                addChunk(convertToKiloWatt, chunks, currentChunk, prevResolution);
                currentChunk = new ArrayList<>();
                position = 1;
                prevResolution = curResolution;
            }
            currentChunk.add(new Pair<>(position++, cur));
        }
        addChunk(convertToKiloWatt, chunks, currentChunk, prevResolution);
        return chunks;
    }

    private Duration resolutionOf(MeterReading.Point point) {
        return point.granularity()
                    .orElseGet(() -> meterReading()
                            .granularity()
                            .orElse(Duration.ofDays(1)));
    }

    private void addChunk(
            boolean convertToKiloWatt,
            List<PointsWithResolution> chunks,
            List<Pair<Integer, MeterReading.Point>> currentChunk,
            Duration prevResolution
    ) {
        chunks.add(new PointsWithResolution(currentChunk.stream()
                                                        .map(p -> toPoint(p, convertToKiloWatt))
                                                        .toList(),
                                            prevResolution,
                                            currentChunk.getFirst().value().timestamp(),
                                            currentChunk.getLast().value().timestamp()));
    }

    private PointComplexType toPoint(Pair<Integer, MeterReading.Point> point, boolean convertToKiloWatt) {
        var quantity = point.value().value();
        if (convertToKiloWatt) {
            quantity = quantity / 1000.0;
        }
        return new PointComplexType()
                .withPosition(Integer.toString(point.key()))
                .withEnergyQuantityQuantity(BigDecimal.valueOf(quantity))
                .withEnergyQuantityQuality(QualityTypeList.AS_PROVIDED);
    }
}
