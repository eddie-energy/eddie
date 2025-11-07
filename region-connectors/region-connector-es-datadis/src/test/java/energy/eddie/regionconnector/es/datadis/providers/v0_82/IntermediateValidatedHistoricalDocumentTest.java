package energy.eddie.regionconnector.es.datadis.providers.v0_82;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v0_82.vhd.*;
import energy.eddie.regionconnector.es.datadis.DatadisPermissionRequestBuilder;
import energy.eddie.regionconnector.es.datadis.MeteringDataProvider;
import energy.eddie.regionconnector.es.datadis.PointType;
import energy.eddie.regionconnector.es.datadis.config.PlainDatadisConfiguration;
import energy.eddie.regionconnector.es.datadis.dtos.IntermediateMeteringData;
import energy.eddie.regionconnector.es.datadis.permission.request.DistributorCode;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class IntermediateValidatedHistoricalDocumentTest {

    public static final PlainCommonInformationModelConfiguration CIM_CONFIG = new PlainCommonInformationModelConfiguration(
            CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
            "fallbackId"
    );

    public static IdentifiableMeteringData identifiableMeterReading(boolean production) throws IOException {
        var intermediateMeteringData = IntermediateMeteringData.fromMeteringData(
                production ? MeteringDataProvider.loadSurplusMeteringData() : MeteringDataProvider.loadMeteringData()
        ).block(Duration.ofMinutes(10));
        assert intermediateMeteringData != null;
        EsPermissionRequest permissionRequest = new DatadisPermissionRequestBuilder()
                .setPermissionId("permissionId")
                .setConnectionId("connectionId")
                .setDataNeedId("dataNeedId")
                .setGranularity(Granularity.PT1H)
                .setNif("nif")
                .setMeteringPointId("meteringPointId")
                .setStart(intermediateMeteringData.start())
                .setEnd(intermediateMeteringData.end())
                .setDistributorCode(DistributorCode.ASEME)
                .setPointType(PointType.TYPE_1)
                .setStatus(PermissionProcessStatus.ACCEPTED)
                .setCreated(ZonedDateTime.now(ZoneOffset.UTC))
                .setProductionSupport(production)
                .build();
        return new IdentifiableMeteringData(permissionRequest, intermediateMeteringData);
    }

    @SuppressWarnings("java:S5961") // Sonar complains about the nr of assertions
    @Test
    void eddieValidatedHistoricalDataMarketDocument_withoutProduction() throws IOException {

        IdentifiableMeteringData identifiableMeteringData = identifiableMeterReading(false);
        PlainDatadisConfiguration datadisConfig = new PlainDatadisConfiguration("clientId",
                                                                                "clientSecret",
                                                                                "basepath"
        );
        var intermediateVHD = new IntermediateValidatedHistoricalDocument(
                identifiableMeteringData,
                CIM_CONFIG,
                datadisConfig
        );

        var eddieMarketDocument = intermediateVHD.eddieValidatedHistoricalDataMarketDocument();
        var marketDocument = eddieMarketDocument.getValidatedHistoricalDataMarketDocument();

        var timeframe = new EsmpTimeInterval(
                identifiableMeteringData.intermediateMeteringData().start(),
                identifiableMeteringData.intermediateMeteringData().end(),
                ZONE_ID_SPAIN
        );
        var timeSeries = marketDocument.getTimeSeriesList().getTimeSeries().getFirst();
        var seriesPeriod = timeSeries.getSeriesPeriodList().getSeriesPeriods().getFirst();

        assertAll(
                // Metadata
                () -> assertEquals(CommonInformationModelVersions.V0_82.version(), marketDocument.getRevisionNumber()),
                () -> assertEquals(MessageTypeList.MEASUREMENT_VALUE_DOCUMENT, marketDocument.getType()),
                () -> assertEquals(RoleTypeList.METERING_POINT_ADMINISTRATOR,
                                   marketDocument.getSenderMarketParticipantMarketRoleType()),
                () -> assertEquals(RoleTypeList.CONSUMER, marketDocument.getReceiverMarketParticipantMarketRoleType()),
                () -> assertEquals(ProcessTypeList.REALISED, marketDocument.getProcessProcessType()),
                // Sender
                () -> assertEquals(CodingSchemeTypeList.SPAIN_NATIONAL_CODING_SCHEME,
                                   marketDocument.getSenderMarketParticipantMRID().getCodingScheme()),
                () -> assertEquals(DistributorCode.ASEME.name(),
                                   marketDocument.getSenderMarketParticipantMRID().getValue()),
                // Receiver
                () -> assertEquals(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                                   marketDocument.getReceiverMarketParticipantMRID().getCodingScheme()),
                () -> assertEquals(datadisConfig.username(),
                                   marketDocument.getReceiverMarketParticipantMRID().getValue()),
                // Period TimeInterval
                () -> assertEquals(timeframe.start(), marketDocument.getPeriodTimeInterval().getStart()),
                () -> assertEquals(timeframe.end(), marketDocument.getPeriodTimeInterval().getEnd()),
                // TimeSeries
                () -> assertEquals(1, marketDocument.getTimeSeriesList().getTimeSeries().size()),
                () -> assertEquals(BusinessTypeList.CONSUMPTION, timeSeries.getBusinessType()),
                () -> assertEquals(EnergyProductTypeList.ACTIVE_ENERGY, timeSeries.getProduct()),
                () -> assertEquals(DirectionTypeList.DOWN, timeSeries.getFlowDirectionDirection()),
                () -> assertEquals(AggregateKind.SUM,
                                   timeSeries.getMarketEvaluationPointMeterReadingsReadingsReadingTypeAggregation()),
                () -> assertEquals(CommodityKind.ELECTRICITYPRIMARYMETERED,
                                   timeSeries.getMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity()),
                () -> assertEquals(CodingSchemeTypeList.SPAIN_NATIONAL_CODING_SCHEME,
                                   timeSeries.getMarketEvaluationPointMRID().getCodingScheme()),
                () -> assertEquals(identifiableMeteringData.permissionRequest().meteringPointId(),
                                   timeSeries.getMarketEvaluationPointMRID().getValue()),
                // Reason
                () -> assertEquals(ReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED,
                                   timeSeries.getReasonList().getReasons().getFirst().getCode()),
                // Resource
                () -> assertEquals(identifiableMeteringData.permissionRequest().meteringPointId(),
                                   timeSeries.getRegisteredResource().getMRID()),
                // SeriesPeriod
                () -> assertEquals(1, timeSeries.getSeriesPeriodList().getSeriesPeriods().size()),
                () -> assertEquals(timeframe.start(), seriesPeriod.getTimeInterval().getStart()),
                () -> assertEquals(timeframe.end(), seriesPeriod.getTimeInterval().getEnd()),

                () -> assertEquals(Granularity.PT1H.name(), seriesPeriod.getResolution()),
                () -> assertEquals(identifiableMeteringData.intermediateMeteringData().meteringData().size(),
                                   seriesPeriod.getPointList().getPoints().size()),
                () -> {
                    // assert that all point in the array have the same value as meteringData.cunsuptionKwh
                    for (int i = identifiableMeteringData.intermediateMeteringData()
                                                         .meteringData()
                                                         .size() - 1; i >= 0; i--) {
                        var point = seriesPeriod.getPointList().getPoints().get(i);
                        var meteringData = identifiableMeteringData.intermediateMeteringData().meteringData().get(i);
                        int finalI = i;
                        QualityTypeList expectedQuality = switch (meteringData.obtainMethod()) {
                            case ESTIMATED -> QualityTypeList.ESTIMATED;
                            case REAL -> QualityTypeList.AS_PROVIDED;
                            case UNKNOWN -> QualityTypeList.NOT_AVAILABLE;
                        };
                        assertAll(
                                () -> assertEquals(String.valueOf(finalI), point.getPosition()),
                                () -> assertEquals(BigDecimal.valueOf(meteringData.consumptionKWh()),
                                                   point.getEnergyQuantityQuantity()),
                                () -> assertEquals(expectedQuality, point.getEnergyQuantityQuality())
                        );
                    }
                }

        );
    }

    @SuppressWarnings("java:S5961") // Sonar complains about the nr of assertions
    @Test
    void eddieValidatedHistoricalDataMarketDocument_withProduction() throws IOException {

        IdentifiableMeteringData identifiableMeteringData = identifiableMeterReading(true);
        PlainDatadisConfiguration datadisConfig = new PlainDatadisConfiguration("clientId",
                                                                                "clientSecret",
                                                                                "basepath"
        );
        var intermediateVHD = new IntermediateValidatedHistoricalDocument(
                identifiableMeteringData,
                CIM_CONFIG,
                datadisConfig
        );

        var eddieMarketDocument = intermediateVHD.eddieValidatedHistoricalDataMarketDocument();
        var marketDocument = eddieMarketDocument.getValidatedHistoricalDataMarketDocument();

        var timeframe = new EsmpTimeInterval(
                identifiableMeteringData.intermediateMeteringData().start(),
                identifiableMeteringData.intermediateMeteringData().end(),
                ZONE_ID_SPAIN
        );
        var consumptionTimeSeries = marketDocument.getTimeSeriesList().getTimeSeries().getFirst();
        var consumptionSeriesPeriod = consumptionTimeSeries.getSeriesPeriodList().getSeriesPeriods().getFirst();

        var productionTimeSeries = marketDocument.getTimeSeriesList().getTimeSeries().getLast();
        var productionSeriesPeriod = productionTimeSeries.getSeriesPeriodList().getSeriesPeriods().getFirst();

        assertAll(
                // Metadata
                () -> assertEquals(CommonInformationModelVersions.V0_82.version(), marketDocument.getRevisionNumber()),
                () -> assertEquals(MessageTypeList.MEASUREMENT_VALUE_DOCUMENT, marketDocument.getType()),
                () -> assertEquals(RoleTypeList.METERING_POINT_ADMINISTRATOR,
                                   marketDocument.getSenderMarketParticipantMarketRoleType()),
                () -> assertEquals(RoleTypeList.CONSUMER, marketDocument.getReceiverMarketParticipantMarketRoleType()),
                () -> assertEquals(ProcessTypeList.REALISED, marketDocument.getProcessProcessType()),
                // Sender
                () -> assertEquals(CodingSchemeTypeList.SPAIN_NATIONAL_CODING_SCHEME,
                                   marketDocument.getSenderMarketParticipantMRID().getCodingScheme()),
                () -> assertEquals(DistributorCode.ASEME.name(),
                                   marketDocument.getSenderMarketParticipantMRID().getValue()),
                // Receiver
                () -> assertEquals(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                                   marketDocument.getReceiverMarketParticipantMRID().getCodingScheme()),
                () -> assertEquals(datadisConfig.username(),
                                   marketDocument.getReceiverMarketParticipantMRID().getValue()),
                // Period TimeInterval
                () -> assertEquals(timeframe.start(), marketDocument.getPeriodTimeInterval().getStart()),
                () -> assertEquals(timeframe.end(), marketDocument.getPeriodTimeInterval().getEnd()),
                () -> assertEquals(2, marketDocument.getTimeSeriesList().getTimeSeries().size()),
                // Consumption TimeSeries
                () -> assertEquals(BusinessTypeList.CONSUMPTION, consumptionTimeSeries.getBusinessType()),
                () -> assertEquals(EnergyProductTypeList.ACTIVE_ENERGY, consumptionTimeSeries.getProduct()),
                () -> assertEquals(DirectionTypeList.DOWN, consumptionTimeSeries.getFlowDirectionDirection()),
                () -> assertEquals(AggregateKind.SUM,
                                   consumptionTimeSeries.getMarketEvaluationPointMeterReadingsReadingsReadingTypeAggregation()),
                () -> assertEquals(CommodityKind.ELECTRICITYPRIMARYMETERED,
                                   consumptionTimeSeries.getMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity()),
                () -> assertEquals(CodingSchemeTypeList.SPAIN_NATIONAL_CODING_SCHEME,
                                   consumptionTimeSeries.getMarketEvaluationPointMRID().getCodingScheme()),
                () -> assertEquals(identifiableMeteringData.permissionRequest().meteringPointId(),
                                   consumptionTimeSeries.getMarketEvaluationPointMRID().getValue()),
                // Production TimeSeries
                () -> assertEquals(BusinessTypeList.PRODUCTION, productionTimeSeries.getBusinessType()),
                () -> assertEquals(EnergyProductTypeList.ACTIVE_ENERGY, productionTimeSeries.getProduct()),
                () -> assertEquals(DirectionTypeList.UP, productionTimeSeries.getFlowDirectionDirection()),
                () -> assertEquals(AggregateKind.SUM,
                                   productionTimeSeries.getMarketEvaluationPointMeterReadingsReadingsReadingTypeAggregation()),
                () -> assertEquals(CommodityKind.ELECTRICITYPRIMARYMETERED,
                                   productionTimeSeries.getMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity()),
                () -> assertEquals(CodingSchemeTypeList.SPAIN_NATIONAL_CODING_SCHEME,
                                   productionTimeSeries.getMarketEvaluationPointMRID().getCodingScheme()),
                () -> assertEquals(identifiableMeteringData.permissionRequest().meteringPointId(),
                                   productionTimeSeries.getMarketEvaluationPointMRID().getValue()),
                // Reason
                () -> assertEquals(ReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED,
                                   consumptionTimeSeries.getReasonList().getReasons().getFirst().getCode()),
                // Resource
                () -> assertEquals(identifiableMeteringData.permissionRequest().meteringPointId(),
                                   consumptionTimeSeries.getRegisteredResource().getMRID()),
                // SeriesPeriod
                () -> assertEquals(1, consumptionTimeSeries.getSeriesPeriodList().getSeriesPeriods().size()),
                () -> assertEquals(timeframe.start(), consumptionSeriesPeriod.getTimeInterval().getStart()),
                () -> assertEquals(timeframe.end(), consumptionSeriesPeriod.getTimeInterval().getEnd()),

                () -> assertEquals(Granularity.PT1H.name(), consumptionSeriesPeriod.getResolution()),
                () -> assertEquals(identifiableMeteringData.intermediateMeteringData().meteringData().size(),
                                   consumptionSeriesPeriod.getPointList().getPoints().size()),
                () -> {
                    // assert that all points in the array have the same value as meteringData.consumptionKwh
                    for (int i = identifiableMeteringData.intermediateMeteringData()
                                                         .meteringData()
                                                         .size() - 1; i >= 0; i--) {
                        var consumptionPoint = consumptionSeriesPeriod.getPointList().getPoints().get(i);
                        var meteringData = identifiableMeteringData.intermediateMeteringData().meteringData().get(i);
                        int finalI = i;
                        QualityTypeList expectedQuality = switch (meteringData.obtainMethod()) {
                            case ESTIMATED -> QualityTypeList.ESTIMATED;
                            case REAL -> QualityTypeList.AS_PROVIDED;
                            case UNKNOWN -> QualityTypeList.NOT_AVAILABLE;
                        };
                        assertAll(
                                () -> assertEquals(String.valueOf(finalI), consumptionPoint.getPosition()),
                                () -> assertEquals(BigDecimal.valueOf(meteringData.consumptionKWh()),
                                                   consumptionPoint.getEnergyQuantityQuantity()),
                                () -> assertEquals(expectedQuality, consumptionPoint.getEnergyQuantityQuality())
                        );
                    }
                },
                () -> {
                    // assert that all points in the array have the same value as meteringData.consumptionKwh
                    for (int i = identifiableMeteringData.intermediateMeteringData()
                                                         .meteringData()
                                                         .size() - 1; i >= 0; i--) {
                        var productionPoint = productionSeriesPeriod.getPointList().getPoints().get(i);
                        var meteringData = identifiableMeteringData.intermediateMeteringData().meteringData().get(i);
                        int finalI = i;
                        QualityTypeList expectedQuality = switch (meteringData.obtainMethod()) {
                            case ESTIMATED -> QualityTypeList.ESTIMATED;
                            case REAL -> QualityTypeList.AS_PROVIDED;
                            case UNKNOWN -> QualityTypeList.NOT_AVAILABLE;
                        };
                        assertAll(
                                () -> assertEquals(String.valueOf(finalI), productionPoint.getPosition()),
                                () -> assertEquals(BigDecimal.valueOf(meteringData.surplusEnergyKWh()),
                                                   productionPoint.getEnergyQuantityQuantity()),
                                () -> assertEquals(expectedQuality, productionPoint.getEnergyQuantityQuality())
                        );
                    }
                }

        );
    }
}
