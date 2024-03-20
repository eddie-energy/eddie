package energy.eddie.regionconnector.es.datadis.providers.v0_82;

import energy.eddie.api.CommonInformationModelVersions;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.cim.v0_82.vhd.*;
import energy.eddie.regionconnector.es.datadis.MeteringDataProvider;
import energy.eddie.regionconnector.es.datadis.config.PlainDatadisConfiguration;
import energy.eddie.regionconnector.es.datadis.dtos.IntermediateMeteringData;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.DistributorCode;
import energy.eddie.regionconnector.es.datadis.permission.request.StateBuilderFactory;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
import energy.eddie.regionconnector.shared.utils.EsmpTimeInterval;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;

import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.ZONE_ID_SPAIN;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class IntermediateValidatedHistoricalDocumentTest {

    public static IdentifiableMeteringData identifiableMeterReading() throws IOException {
        var intermediateMeteringData = IntermediateMeteringData.fromMeteringData(MeteringDataProvider.loadMeteringData());
        StateBuilderFactory stateBuilderFactory = new StateBuilderFactory(null);
        PermissionRequestForCreation permissionRequestForCreation = new PermissionRequestForCreation(
                "connectionId",
                "dataNeedId",
                "nif",
                "meteringPointId",
                intermediateMeteringData.start().atStartOfDay(ZONE_ID_SPAIN),
                intermediateMeteringData.end().atStartOfDay(ZONE_ID_SPAIN),
                Granularity.PT1H);
        EsPermissionRequest permissionRequest = new DatadisPermissionRequest("permissionId", permissionRequestForCreation, stateBuilderFactory);
        permissionRequest.changeState(stateBuilderFactory.create(permissionRequest, PermissionProcessStatus.ACCEPTED).build());
        permissionRequest.setDistributorCodeAndPointType(DistributorCode.ASEME, 1);
        return new IdentifiableMeteringData(permissionRequest, intermediateMeteringData);
    }

    @SuppressWarnings("java:S5961") // Sonar complains about the nr of assertions
    @Test
    void eddieValidatedHistoricalDataMarketDocument() throws IOException {

        IdentifiableMeteringData identifiableMeteringData = identifiableMeterReading();
        PlainDatadisConfiguration datadisConfig = new PlainDatadisConfiguration("clientId", "clientSecret", "basepath");
        var intermediateVHD = new IntermediateValidatedHistoricalDocument(
                identifiableMeteringData,
                () -> CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                datadisConfig
        );

        var eddieMarketDocument = intermediateVHD.eddieValidatedHistoricalDataMarketDocument();
        var marketDocument = eddieMarketDocument.marketDocument();

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
                () -> assertEquals(RoleTypeList.METERING_POINT_ADMINISTRATOR, marketDocument.getSenderMarketParticipantMarketRoleType()),
                () -> assertEquals(RoleTypeList.CONSUMER, marketDocument.getReceiverMarketParticipantMarketRoleType()),
                () -> assertEquals(ProcessTypeList.REALISED, marketDocument.getProcessProcessType()),
                // Sender
                () -> assertEquals(CodingSchemeTypeList.SPAIN_NATIONAL_CODING_SCHEME, marketDocument.getSenderMarketParticipantMRID().getCodingScheme()),
                () -> assertEquals(DistributorCode.ASEME.name(), marketDocument.getSenderMarketParticipantMRID().getValue()),
                // Receiver
                () -> assertEquals(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME, marketDocument.getReceiverMarketParticipantMRID().getCodingScheme()),
                () -> assertEquals(datadisConfig.username(), marketDocument.getReceiverMarketParticipantMRID().getValue()),
                // Period TimeInterval
                () -> assertEquals(timeframe.start(), marketDocument.getPeriodTimeInterval().getStart()),
                () -> assertEquals(timeframe.end(), marketDocument.getPeriodTimeInterval().getEnd()),
                // TimeSeries
                () -> assertEquals(1, marketDocument.getTimeSeriesList().getTimeSeries().size()),
                () -> assertEquals(BusinessTypeList.CONSUMPTION, timeSeries.getBusinessType()),
                () -> assertEquals(EnergyProductTypeList.ACTIVE_ENERGY, timeSeries.getProduct()),
                () -> assertEquals(DirectionTypeList.DOWN, timeSeries.getFlowDirectionDirection()),
                () -> assertEquals(AggregateKind.SUM, timeSeries.getMarketEvaluationPointMeterReadingsReadingsReadingTypeAggregation()),
                () -> assertEquals(CommodityKind.ELECTRICITYPRIMARYMETERED, timeSeries.getMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity()),
                () -> assertEquals(CodingSchemeTypeList.SPAIN_NATIONAL_CODING_SCHEME, timeSeries.getMarketEvaluationPointMRID().getCodingScheme()),
                () -> assertEquals(identifiableMeterReading().permissionRequest().meteringPointId(), timeSeries.getMarketEvaluationPointMRID().getValue()),
                // Reason
                () -> assertEquals(ReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED, timeSeries.getReasonList().getReasons().getFirst().getCode()),
                // Resource
                () -> assertEquals(identifiableMeterReading().permissionRequest().meteringPointId(), timeSeries.getRegisteredResource().getMRID()),
                // SeriesPeriod
                () -> assertEquals(1, timeSeries.getSeriesPeriodList().getSeriesPeriods().size()),
                () -> assertEquals(timeframe.start(), seriesPeriod.getTimeInterval().getStart()),
                () -> assertEquals(timeframe.end(), seriesPeriod.getTimeInterval().getEnd()),

                () -> assertEquals(Granularity.PT1H.name(), seriesPeriod.getResolution()),
                () -> assertEquals(identifiableMeteringData.intermediateMeteringData().meteringData().size(), seriesPeriod.getPointList().getPoints().size()),
                () -> {
                    // assert that all point in the array have the same value as meteringData.cunsuptionKwh
                    for (int i = identifiableMeteringData.intermediateMeteringData().meteringData().size() - 1; i >= 0; i--) {
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
                                () -> assertEquals(BigDecimal.valueOf(meteringData.consumptionKWh()), point.getEnergyQuantityQuantity()),
                                () -> assertEquals(expectedQuality, point.getEnergyQuantityQuality())
                        );
                    }
                }

        );
    }
}
