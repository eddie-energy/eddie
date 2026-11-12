// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.providers.v1_04;

import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v1_04.*;
import energy.eddie.cim.v1_04.vhd.*;
import energy.eddie.regionconnector.es.datadis.config.DatadisConfiguration;
import energy.eddie.regionconnector.es.datadis.permission.request.DistributorCode;
import energy.eddie.regionconnector.es.datadis.providers.DatadisMeteringSeries;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
import energy.eddie.regionconnector.shared.cim.CimCodes;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;
import energy.eddie.regionconnector.shared.cim.v1_04.VhdEnvelopeWrapper;

import javax.xml.datatype.DatatypeFactory;
import java.time.ZonedDateTime;
import java.util.*;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;

public final class IntermediateValidatedHistoricalDocument {
    private final CommonInformationModelConfiguration cimConfig;
    private final DatadisConfiguration datadisConfig;
    private final IdentifiableMeteringData identifiableMeteringData;

    IntermediateValidatedHistoricalDocument(
            IdentifiableMeteringData identifiableMeteringData,
            CommonInformationModelConfiguration cimConfig,
            DatadisConfiguration datadisConfig
    ) {
        this.identifiableMeteringData = identifiableMeteringData;
        this.cimConfig = cimConfig;
        this.datadisConfig = datadisConfig;
    }

    public VHDEnvelope value() {
        var timeframe = new EsmpTimeInterval(
                identifiableMeteringData.intermediateMeteringData().start(),
                identifiableMeteringData.intermediateMeteringData().end(),
                ZONE_ID_SPAIN
        );

        var receiverCodingScheme = StandardCodingSchemeTypeList.fromValue(
                cimConfig.eligiblePartyNationalCodingScheme().value()
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
                                .withCodingScheme(StandardCodingSchemeTypeList.SPAIN_NATIONAL_CODING_SCHEME.value())
                                .withValue(identifiableMeteringData.permissionRequest()
                                                                   .distributorCode()
                                                                   .map(DistributorCode::name)
                                                                   .orElse("Datadis"))
                )
                .withCreatedDateTime(ZonedDateTime.now(ZONE_ID_SPAIN))
                .withReceiverMarketParticipantMRID(
                        new PartyIDString()
                                .withCodingScheme(receiverCodingScheme.value())
                                .withValue(datadisConfig.username())
                )
                .withPeriodTimeInterval(
                        new ESMPDateTimeInterval()
                                .withStart(timeframe.start())
                                .withEnd(timeframe.end())
                )
                .withTimeSeries(timeSeriesList(timeframe));
        return new VhdEnvelopeWrapper(vhd, identifiableMeteringData.permissionRequest()).wrap();
    }

    private List<TimeSeries> timeSeriesList(EsmpTimeInterval timeframe) {
        var list = new ArrayList<TimeSeries>();
        for (var series : DatadisMeteringSeries.of(identifiableMeteringData)) {
            list.add(
                    new TimeSeries()
                            .withVersion("1")
                            .withMRID(UUID.randomUUID().toString())
                            .withBusinessType(series.businessType())
                            .withProduct(CimCodes.EnergyProduct.ACTIVE_ENERGY)
                            .withFlowDirectionDirection(series.direction())
                            .withMarketEvaluationPointMeterReadingsReadingsReadingTypeAggregate(AggregateKind.fromValue(CimCodes.Aggregate.SUM))
                            // No mapping available
                            .withMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity(CommodityKind.fromValue(CimCodes.Commodity.ELECTRICITY_PRIMARY_METERED))
                            .withEnergyMeasurementUnitName(CimCodes.UnitOfMeasure.KILOWATT_HOUR)
                            .withMarketEvaluationPointMRID(
                                    new MeasurementPointIDString()
                                            .withCodingScheme(StandardCodingSchemeTypeList.SPAIN_NATIONAL_CODING_SCHEME.value())
                                            .withValue(identifiableMeteringData.permissionRequest().meteringPointId())
                            )
                            .withReasonCode(CimCodes.ReasonCode.ERRORS_NOT_SPECIFICALLY_IDENTIFIED)
                            .withRegisteredResourceMRID(
                                    new ResourceIDString()
                                            .withCodingScheme(StandardCodingSchemeTypeList.SPAIN_NATIONAL_CODING_SCHEME.value())
                                            .withValue(series.cups())
                            )
                            .withPeriods(seriesPeriod(timeframe, series))
            );
        }
        return list;
    }

    private SeriesPeriod seriesPeriod(EsmpTimeInterval timeInterval, DatadisMeteringSeries.Series series) {
        var points = new ArrayList<Point>(series.points().size());
        for (var point : series.points()) {
            points.add(new Point()
                               .withPosition(point.position())
                               .withEnergyQuantityQuantity(point.quantity())
                               .withEnergyQuantityQuality(point.quality()));
        }
        var resolution = DatadisMeteringSeries.resolution(identifiableMeteringData);
        return new SeriesPeriod()
                .withResolution(DatatypeFactory.newDefaultInstance().newDuration(resolution.toMillis()))
                .withTimeInterval(
                        new ESMPDateTimeInterval()
                                .withStart(timeInterval.start())
                                .withEnd(timeInterval.end())
                )
                .withPoints(points);
    }
}
