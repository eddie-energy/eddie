/*
 * SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
 * SPDX-License-Identifier: Apache-2.0
 */

package energy.eddie.regionconnector.nl.mijn.aansluiting.providers.v0_92;

import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v0_92.*;
import energy.eddie.cim.v0_92.nrtmd.*;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.MijnAansluitingResponse;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.ReadingType;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.Register;
import energy.eddie.regionconnector.nl.mijn.aansluiting.config.MijnAansluitingConfiguration;
import energy.eddie.regionconnector.nl.mijn.aansluiting.dtos.IdentifiableMeteredData;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeFactory;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

class IntermediateRealTimeMeasurementMarketDocument {
    public static final Duration ONE_DAY = Duration.ofDays(1);
    private static final Logger LOGGER = LoggerFactory.getLogger(IntermediateRealTimeMeasurementMarketDocument.class);
    private static final javax.xml.datatype.Duration P1D = DatatypeFactory.newDefaultInstance()
                                                                          .newDuration(ONE_DAY.toMillis());
    private final IdentifiableMeteredData identifiableMeteredData;
    private final MijnAansluitingConfiguration config;

    public IntermediateRealTimeMeasurementMarketDocument(
            IdentifiableMeteredData identifiableMeteredData,
            MijnAansluitingConfiguration config
    ) {
        this.identifiableMeteredData = identifiableMeteredData;
        this.config = config;
    }

    public List<ReferenceEnergyCurveNearRealTimeMeasurementMarketDocument> value() {
        var rtms = new ArrayList<ReferenceEnergyCurveNearRealTimeMeasurementMarketDocument>();
        for (var response : identifiableMeteredData.meteredData()) {
            for (var register : response.marketEvaluationPoint().registerList()) {
                rtms.add(toNearRealTimeMeasurement(response, register));
            }
        }
        return rtms;
    }

    private ReferenceEnergyCurveNearRealTimeMeasurementMarketDocument toNearRealTimeMeasurement(
            MijnAansluitingResponse response,
            Register register
    ) {
        var interval = new EsmpTimeInterval(register.readingList()
                                                    .getFirst()
                                                    .dateAndOrTime()
                                                    .dateTime(),
                                            register.readingList()
                                                    .getLast()
                                                    .dateAndOrTime()
                                                    .dateTime()
                                                    .plus(ONE_DAY));
        return new ReferenceEnergyCurveNearRealTimeMeasurementMarketDocument()
                .withMRID(UUID.randomUUID().toString())
                .withRevisionNumber(CommonInformationModelVersions.V0_92.cimify())
                .withType(StandardMessageTypeList.MEASUREMENT_VALUE_DOCUMENT.value())
                .withCreatedDateTime(ZonedDateTime.now(ZoneOffset.UTC))
                .withSenderMarketParticipantMRID(
                        new PartyIDString()
                                .withCodingScheme(StandardCodingSchemeTypeList.NETHERLANDS_NATIONAL_CODING_SCHEME.value())
                                .withValue("EDSN")
                )
                .withSenderMarketParticipantMarketRoleType(StandardRoleTypeList.METERING_POINT_ADMINISTRATOR.value())
                .withReceiverMarketParticipantMRID(
                        new PartyIDString()
                                .withCodingScheme(StandardCodingSchemeTypeList.NETHERLANDS_NATIONAL_CODING_SCHEME.value())
                                .withValue(config.continuousClientId().getValue())
                )
                .withReceiverMarketParticipantMarketRoleType(StandardRoleTypeList.CONSUMER.value())
                .withProcessProcessType(StandardProcessTypeList.REALISED.value())
                .withPeriodTimeInterval(
                        new ESMPDateTimeInterval()
                                .withStart(interval.start())
                                .withEnd(interval.end())
                )
                .withSeries(timeSeries(response, register));
    }

    private TimeSeriesSeries timeSeries(MijnAansluitingResponse response, Register register) {
        var flowDirection = getFlowDirection(register);
        var businessType = getBusinessType(flowDirection);
        return new TimeSeriesSeries()
                .withRegisteredResourceMRID(
                        new ResourceIDString()
                                .withCodingScheme(StandardCodingSchemeTypeList.NETHERLANDS_NATIONAL_CODING_SCHEME.value())
                                .withValue(register.mrid())
                )
                .withBusinessType(businessType)
                .withCurveType(StandardCurveTypeList.POINT.value())
                // TODO: No active power is available for the market product type list, consult wout
                //.withMarketProductMarketProductType(StandardMarketProductTypeList.ACTIVE_POWER.value())
                .withFlowDirectionDirection(flowDirection == null ? null : flowDirection.value())
                .withResourceTimeSeriesValue1ScheduleType(ScheduleKind.fromValue("load"))
                .withMarketEvaluationPointMRID(
                        new MeasurementPointIDString()
                                .withCodingScheme(StandardCodingSchemeTypeList.NETHERLANDS_NATIONAL_CODING_SCHEME.value())
                                .withValue(response.marketEvaluationPoint().mrid())
                )
                .withSeries(getSeries(register));
    }

    private List<Series> getSeries(Register register) {
        var points = new ArrayList<Point>();
        var unit = readingUnit(register.readingList().getFirst().readingType());
        var interval = new EsmpTimeInterval(
                register.readingList().getFirst().dateAndOrTime().dateTime(),
                register.readingList().getLast().dateAndOrTime().dateTime().plus(ONE_DAY)
        );
        var pos = 1;
        for (var reading : register.readingList()) {
            points.add(
                    new Point()
                            .withPosition(pos++)
                            .withMeasuredQuantityQuality(StandardQualityTypeList.AS_PROVIDED)
                            .withMeasuredQuantityQuantity(reading.value())
            );
        }

        var series = new Series()
                .withMeasurementUnitName(unit)
                .withPeriods(
                        new SeriesPeriod()
                                .withResolution(P1D)
                                .withTimeInterval(
                                        new ESMPDateTimeInterval()
                                                .withStart(interval.start())
                                                .withEnd(interval.end())
                                )
                                .withPoints(points)
                );
        return List.of(series);
    }

    @Nullable
    private String getBusinessType(StandardDirectionTypeList flowDirection) {
        return switch (flowDirection) {
            case DOWN -> StandardBusinessTypeList.CONSUMPTION.value();
            case UP -> StandardBusinessTypeList.PRODUCTION.value();
            case null, default -> null;
        };
    }

    private static StandardDirectionTypeList getFlowDirection(Register register) {
        var mrid = register.meter().mrid();
        if (mrid.startsWith("1.8.1") || mrid.startsWith("1.8.2")) {
            return StandardDirectionTypeList.DOWN;
        }
        if (mrid.startsWith("2.8.1") || mrid.startsWith("2.8.2")) {
            return StandardDirectionTypeList.UP;
        }
        return StandardDirectionTypeList.UP_AND_DOWN;
    }

    @Nullable
    private static String readingUnit(ReadingType readingType) {
        var multiplier = (readingType.multiplier() == null ? "" : readingType.multiplier().toString());
        var readingUnit = multiplier + readingType.unit();

        var unit = readingUnit.toUpperCase(Locale.ROOT);
        try {
            return unit.equals("M3")
                    ? StandardUnitOfMeasureTypeList.CUBIC_METRE.value()
                    : StandardUnitOfMeasureTypeList.fromValue(unit).value();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Got unknown unit {}", unit, e);
            return null;
        }
    }
}
