// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.providers.v1_04;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.utils.Pair;
import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v1_04.*;
import energy.eddie.cim.v1_04.vhd.*;
import energy.eddie.regionconnector.fr.enedis.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.dto.readings.MeterReading;
import energy.eddie.regionconnector.fr.enedis.dto.readings.MeterReading.Reading;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableMeterReading;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;
import energy.eddie.regionconnector.shared.cim.v1_04.VhdEnvelopeWrapper;
import jakarta.annotation.Nullable;

import javax.xml.datatype.DatatypeFactory;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
                meterReading().period().start().atStartOfDay(ZONE_ID_FR),
                meterReading().period().end().atStartOfDay(ZONE_ID_FR)
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
        return this.identifiableMeterReading.payload();
    }

    private List<TimeSeries> timeSeries() {
        var tss = new ArrayList<TimeSeries>();
        for (var reading : meterReading().readings()) {
            var ts = new TimeSeries()
                    .withMRID(UUID.randomUUID().toString())
                    .withBusinessType(businessType(reading).value())
                    .withProduct(energyProductTypeList(reading))
                    .withVersion("1")
                    .withFlowDirectionDirection(direction(reading).value())
                    .withMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity(CommodityKind.ELECTRICITYPRIMARYMETERED)
                    .withMarketEvaluationPointMRID(
                            new MeasurementPointIDString()
                                    .withCodingScheme(StandardCodingSchemeTypeList.FRANCE_NATIONAL_CODING_SCHEME.value())
                                    .withValue(meterReading().usagePointId())
                    )
                    .withReasonCode(StandardReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED.value());
            switch (reading.unit()) {
                case "W", "VA" -> ts.withEnergyMeasurementUnitName(StandardUnitOfMeasureTypeList.WATT.value())
                                    .withPeriods(seriesPeriods(reading, false));
                case "Wh" -> ts.withEnergyMeasurementUnitName(StandardUnitOfMeasureTypeList.KILOWATT_HOUR.value())
                               .withPeriods(seriesPeriods(reading, true));
                default -> ts.withPeriods(seriesPeriods(reading, false));
            }
            tss.add(ts);
        }
        return tss;
    }

    private StandardBusinessTypeList businessType(Reading reading) {
        return switch (reading.direction()) {
            case CONSUMPTION -> StandardBusinessTypeList.CONSUMPTION;
            case PRODUCTION -> StandardBusinessTypeList.PRODUCTION;
        };
    }

    @Nullable
    private String energyProductTypeList(Reading reading) {
        return switch (reading.unit()) {
            case "W", "VA" -> StandardEnergyProductTypeList.ACTIVE_POWER.value();
            case "Wh" -> StandardEnergyProductTypeList.ACTIVE_ENERGY.value();
            default -> null;
        };
    }

    private StandardDirectionTypeList direction(Reading reading) {
        return switch (reading.direction()) {
            case CONSUMPTION -> StandardDirectionTypeList.DOWN;
            case PRODUCTION -> StandardDirectionTypeList.UP;
        };
    }

    private List<SeriesPeriod> seriesPeriods(Reading reading, boolean convertToKiloWatt) {
        var pointsWithResolutions = batchIntoChunks(reading, convertToKiloWatt);

        List<SeriesPeriod> seriesPeriods = new ArrayList<>();
        for (PointsWithResolution pointsWithResolution : pointsWithResolutions) {
            var resolution = pointsWithResolution.resolution();
            var start = pointsWithResolution.start();
            // We need to subtract the resolution from the start date for load curves (date represents the end of the measurement)
            if (resolution.toMinutes() <= 60) {
                start = start.minusMinutes(resolution.toMinutes());
            }
            var end = pointsWithResolution.end();
            // for daily resolution, we need to adjust the end date (date represents the whole day)
            if (resolution.equals(Duration.ofDays(1))) {
                end = end.plusMinutes(resolution.toMinutes());
            }
            var interval = new EsmpTimeInterval(start, end);
            var seriesPeriod = new SeriesPeriod()
                    .withResolution(DatatypeFactory.newDefaultInstance()
                                                   .newDuration(resolution.toMillis()))
                    .withTimeInterval(new ESMPDateTimeInterval()
                                              .withStart(interval.start())
                                              .withEnd(interval.end())
                    )
                    .withPoints(pointsWithResolution.points());
            seriesPeriods.add(seriesPeriod);
        }
        return seriesPeriods;
    }

    private List<PointsWithResolution> batchIntoChunks(Reading reading, boolean convertToKiloWatt) {
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
                    .orElseGet(() -> meterReading().granularity()
                                                   .orElse(Granularity.P1D.duration()));
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

    private Point toPoint(Pair<Integer, MeterReading.Point> point, boolean convertToKiloWatt) {
        var quantity = point.value().value();
        if (convertToKiloWatt) {
            quantity = quantity / 1000.0;
        }
        return new Point()
                .withPosition(point.key())
                .withEnergyQuantityQuantity(BigDecimal.valueOf(quantity))
                .withEnergyQuantityQuality(StandardQualityTypeList.AS_PROVIDED.value());
    }
}
