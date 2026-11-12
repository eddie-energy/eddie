// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.providers.v1_12;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v1_12.*;
import energy.eddie.cim.v1_12.vhd.*;
import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.config.DatadisConfiguration;
import energy.eddie.regionconnector.es.datadis.dtos.MeteringData;
import energy.eddie.regionconnector.es.datadis.permission.request.DistributorCode;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
import energy.eddie.regionconnector.shared.cim.HistoricalDataEmissionMetadata;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;
import energy.eddie.regionconnector.shared.cim.v1_12.VhdEnvelopeWrapper;

import javax.xml.datatype.DatatypeFactory;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.ToDoubleFunction;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;

public final class IntermediateValidatedHistoricalDocument {
    private final HistoricalDataEmissionMetadata metadata;
    private final CommonInformationModelConfiguration cimConfig;
    private final DatadisConfiguration datadisConfig;
    private final IdentifiableMeteringData identifiableMeteringData;

    IntermediateValidatedHistoricalDocument(
            IdentifiableMeteringData identifiableMeteringData,
            CommonInformationModelConfiguration cimConfig,
            DatadisConfiguration datadisConfig
    ) {
        this(identifiableMeteringData, cimConfig, datadisConfig, new HistoricalDataEmissionMetadata());
    }

    IntermediateValidatedHistoricalDocument(
            IdentifiableMeteringData identifiableMeteringData,
            CommonInformationModelConfiguration cimConfig,
            DatadisConfiguration datadisConfig,
            HistoricalDataEmissionMetadata metadata
    ) {
        this.metadata = metadata;
        this.identifiableMeteringData = identifiableMeteringData;
        this.cimConfig = cimConfig;
        this.datadisConfig = datadisConfig;
    }

    public Envelope value() {
        var timeframe = new EsmpTimeInterval(
                identifiableMeteringData.intermediateMeteringData().start(),
                identifiableMeteringData.intermediateMeteringData().end(),
                ZONE_ID_SPAIN
        );

        var receiverCodingScheme = StandardCodingSchemeTypeList.fromValue(
                cimConfig.eligiblePartyNationalCodingScheme().value()
        );
        var vhd = new VHDMarketDocument()
                .withMRID(metadata.id("document"))
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
                .withCreatedDateTime(metadata.created(ZONE_ID_SPAIN))
                .withReceiverMarketParticipantMRID(
                        new PartyIDString()
                                .withCodingScheme(receiverCodingScheme.value())
                                .withValue(datadisConfig.username())
                )
                .withTimeSeries(timeSeriesList(timeframe));
        return new VhdEnvelopeWrapper(
                vhd, identifiableMeteringData.permissionRequest(), metadata.envelopeCreated()
        ).wrap();
    }

    private List<TimeSeries> timeSeriesList(EsmpTimeInterval timeframe) {
        var list = new ArrayList<>(timeSeries(
                timeframe,
                StandardBusinessTypeList.CONSUMPTION,
                StandardDirectionTypeList.DOWN,
                consumptionPoints()
        ));

        if (identifiableMeteringData.permissionRequest().productionSupport()) {
            list.addAll(timeSeries(
                    timeframe,
                    StandardBusinessTypeList.PRODUCTION,
                    StandardDirectionTypeList.UP,
                    productionPoints()
            ));
        }
        return list;
    }

    private List<TimeSeries> timeSeries(
            EsmpTimeInterval timeframe,
            StandardBusinessTypeList businessType,
            StandardDirectionTypeList directionTypeList,
            Map<String, List<Point>> points
    ) {
        var list = new ArrayList<TimeSeries>();
        for (var cups : points.entrySet()) {
            list.add(
                    new TimeSeries()
                            .withVersion("1")
                            .withMRID(metadata.child(cups.getKey()).id(directionTypeList.value()))
                            .withBusinessType(businessType.value())
                            .withProduct(StandardEnergyProductTypeList.ACTIVE_ENERGY.value())
                            .withFlowDirectionDirection(directionTypeList.value())
                            .withAccountingPointMeterReadingsReadingsReadingTypeAggregate(AggregateKind._26)
                            .withAccountingPointMeterReadingsReadingsReadingTypeCommodity(CommodityKind._2)
                            .withEnergyMeasurementUnitName(StandardUnitOfMeasureTypeList.KILOWATT_HOUR.value())
                            .withAccountingPointMRID(
                                    new MeasurementPointIDString()
                                            .withCodingScheme(StandardCodingSchemeTypeList.SPAIN_NATIONAL_CODING_SCHEME.value())
                                            .withValue(identifiableMeteringData.permissionRequest().meteringPointId())
                            )
                            .withReasonCode(StandardReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED.value())
                            .withRegisteredResourceMRID(
                                    new ResourceIDString()
                                            .withCodingScheme(StandardCodingSchemeTypeList.SPAIN_NATIONAL_CODING_SCHEME.value())
                                            .withValue(cups.getKey())
                            )
                            .withSeriesPeriods(seriesPeriod(timeframe, cups.getValue()))
            );
        }
        return list;
    }

    private Map<String, List<Point>> consumptionPoints() {
        return toPoints(MeteringData::consumptionKWh);
    }

    private Map<String, List<Point>> productionPoints() {
        return toPoints(MeteringData::surplusEnergyKWh);
    }

    private Map<String, List<Point>> toPoints(ToDoubleFunction<MeteringData> energyAccessor) {
        Map<String, List<Point>> points = new HashMap<>();
        int position = 1;
        for (MeteringData meteringData : identifiableMeteringData.intermediateMeteringData().meteringData()) {
            StandardQualityTypeList qualityTypeList = qualityTypeList(meteringData);
            Point consumptionPoint = new Point()
                    .withPosition(position)
                    .withEnergyQuantityQuantity(BigDecimal.valueOf(energyAccessor.applyAsDouble(meteringData)))
                    .withEnergyQuantityQuality(qualityTypeList.value());
            points.putIfAbsent(meteringData.cups(), new ArrayList<>());
            points.get(meteringData.cups()).add(consumptionPoint);
            position++;
        }
        return points;
    }

    private SeriesPeriod seriesPeriod(
            EsmpTimeInterval timeInterval,
            List<Point> points
    ) {
        var dataTypeFactory = DatatypeFactory.newDefaultInstance();
        var granularity = switch (identifiableMeteringData.permissionRequest().measurementType()) {
            case MeasurementType.QUARTER_HOURLY -> Granularity.PT15M;
            case MeasurementType.HOURLY -> Granularity.PT1H;
        };
        return new SeriesPeriod()
                .withResolution(dataTypeFactory.newDuration(granularity.duration().toMillis()))
                .withTimeInterval(
                        new ESMPDateTimeInterval()
                                .withStart(timeInterval.start())
                                .withEnd(timeInterval.end())
                )
                .withPoints(points);
    }

    private static StandardQualityTypeList qualityTypeList(MeteringData meteringData) {
        return switch (meteringData.obtainMethod()) {
            case ESTIMATED -> StandardQualityTypeList.ESTIMATED;
            case REAL -> StandardQualityTypeList.AS_PROVIDED;
            case UNKNOWN -> StandardQualityTypeList.NOT_AVAILABLE;
        };
    }
}
