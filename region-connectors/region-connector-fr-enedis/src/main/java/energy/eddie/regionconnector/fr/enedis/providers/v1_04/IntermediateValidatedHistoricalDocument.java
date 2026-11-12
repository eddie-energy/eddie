// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.providers.v1_04;

import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v1_04.*;
import energy.eddie.cim.v1_04.vhd.*;
import energy.eddie.regionconnector.fr.enedis.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.dto.readings.MeterReading;
import energy.eddie.regionconnector.fr.enedis.providers.EnedisMeteringSeries;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableMeterReading;
import energy.eddie.regionconnector.shared.cim.CimCodes;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;
import energy.eddie.regionconnector.shared.cim.v1_04.VhdEnvelopeWrapper;

import javax.xml.datatype.DatatypeFactory;
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
        for (var series : EnedisMeteringSeries.of(meterReading())) {
            tss.add(new TimeSeries()
                            .withMRID(UUID.randomUUID().toString())
                            .withBusinessType(series.businessType())
                            .withProduct(series.product())
                            .withVersion("1")
                            .withFlowDirectionDirection(series.direction())
                            .withMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity(CommodityKind.fromValue(CimCodes.Commodity.ELECTRICITY_PRIMARY_METERED))
                            .withMarketEvaluationPointMRID(
                                    new MeasurementPointIDString()
                                            .withCodingScheme(StandardCodingSchemeTypeList.FRANCE_NATIONAL_CODING_SCHEME.value())
                                            .withValue(meterReading().usagePointId())
                            )
                            .withReasonCode(StandardReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED.value())
                            .withEnergyMeasurementUnitName(series.unit())
                            .withPeriods(seriesPeriods(series)));
        }
        return tss;
    }

    private static List<SeriesPeriod> seriesPeriods(EnedisMeteringSeries.Series series) {
        List<SeriesPeriod> seriesPeriods = new ArrayList<>();
        for (var period : series.periods()) {
            var interval = new EsmpTimeInterval(period.start(), period.end());
            var points = new ArrayList<Point>(period.points().size());
            for (var point : period.points()) {
                points.add(new Point()
                                   .withPosition(point.position())
                                   .withEnergyQuantityQuantity(point.quantity())
                                   .withEnergyQuantityQuality(point.quality()));
            }
            seriesPeriods.add(new SeriesPeriod()
                                      .withResolution(DatatypeFactory.newDefaultInstance()
                                                                     .newDuration(period.resolution().toMillis()))
                                      .withTimeInterval(new ESMPDateTimeInterval()
                                                                .withStart(interval.start())
                                                                .withEnd(interval.end()))
                                      .withPoints(points));
        }
        return seriesPeriods;
    }
}
