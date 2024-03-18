package energy.eddie.regionconnector.simulation.permission.request;

import energy.eddie.api.CommonInformationModelVersions;
import energy.eddie.api.v0.ConsumptionPoint;
import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.api.v0_82.cim.EddieValidatedHistoricalDataMarketDocument;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.*;
import energy.eddie.regionconnector.shared.utils.EsmpDateTime;
import energy.eddie.regionconnector.shared.utils.EsmpTimeInterval;
import energy.eddie.regionconnector.simulation.SimulationConnectorMetadata;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record IntermediateValidatedHistoricalDataMarketDocument(ConsumptionRecord consumptionRecord,
                                                                CommonInformationModelConfiguration cimConfig) {
    private static final TimeSeriesComplexType.ReasonList REASON_LIST = new TimeSeriesComplexType.ReasonList()
            .withReasons(
                    new ReasonComplexType()
                            .withCode(ReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED));

    public EddieValidatedHistoricalDataMarketDocument value() {
        ZonedDateTime endDate = consumptionRecord.getStartDateTime()
                .plus(
                        Duration.parse(consumptionRecord.getMeteringInterval())
                                .multipliedBy(consumptionRecord.getConsumptionPoints().size())
                );
        var timeframe = new EsmpTimeInterval(
                consumptionRecord.getStartDateTime(),
                endDate
        );

        var vhd = new ValidatedHistoricalDataMarketDocument()
                .withRevisionNumber(CommonInformationModelVersions.V0_82.version())
                .withType(MessageTypeList.MEASUREMENT_VALUE_DOCUMENT)
                .withSenderMarketParticipantMarketRoleType(RoleTypeList.METERING_POINT_ADMINISTRATOR)
                .withReceiverMarketParticipantMarketRoleType(RoleTypeList.CONSUMER)
                .withProcessProcessType(ProcessTypeList.REALISED)
                .withSenderMarketParticipantMRID(
                        new PartyIDStringComplexType()
                                .withCodingScheme(CodingSchemeTypeList.EIC)
                                .withValue(SimulationConnectorMetadata.REGION_CONNECTOR_ID)
                )
                .withMRID(UUID.randomUUID().toString())
                .withCreatedDateTime(EsmpDateTime.now().toString())
                .withReceiverMarketParticipantMRID(new PartyIDStringComplexType()
                                                           .withCodingScheme(
                                                                   cimConfig.eligiblePartyNationalCodingScheme())
                                                           .withValue(SimulationConnectorMetadata.REGION_CONNECTOR_ID))
                .withPeriodTimeInterval(
                        new ESMPDateTimeIntervalComplexType()
                                .withStart(timeframe.start())
                                .withEnd(timeframe.end())
                )
                .withTimeSeriesList(new ValidatedHistoricalDataMarketDocument.TimeSeriesList()
                                            .withTimeSeries(new TimeSeriesComplexType()
                                                                    .withMRID(UUID.randomUUID().toString())
                                                                    .withBusinessType(BusinessTypeList.CONSUMPTION)
                                                                    .withProduct(EnergyProductTypeList.ACTIVE_ENERGY)
                                                                    .withVersion("1.0")
                                                                    .withFlowDirectionDirection(DirectionTypeList.DOWN)
                                                                    .withMarketEvaluationPointMeterReadingsReadingsReadingTypeAggregation(
                                                                            AggregateKind.SUM)
                                                                    .withMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity(
                                                                            CommodityKind.NONE)
                                                                    .withEnergyMeasurementUnitName(
                                                                            UnitOfMeasureTypeList.WATT)
                                                                    .withMarketEvaluationPointMRID(
                                                                            new MeasurementPointIDStringComplexType()
                                                                                    .withCodingScheme(
                                                                                            CodingSchemeTypeList.FRANCE_NATIONAL_CODING_SCHEME)
                                                                                    .withValue(
                                                                                            consumptionRecord.getMeteringPoint())
                                                                    )
                                                                    .withReasonList(REASON_LIST)
                                                                    .withSeriesPeriodList(
                                                                            new TimeSeriesComplexType.SeriesPeriodList()
                                                                                    .withSeriesPeriods(
                                                                                            seriesPeriods(
                                                                                                    consumptionRecord,
                                                                                                    timeframe)
                                                                                    )
                                                                    )
                                            ));

        return new EddieValidatedHistoricalDataMarketDocument(
                Optional.of(consumptionRecord.getConnectionId()),
                Optional.of(consumptionRecord.getPermissionId()),
                Optional.of(consumptionRecord.getDataNeedId()),
                vhd
        );
    }

    private List<SeriesPeriodComplexType> seriesPeriods(ConsumptionRecord consumptionRecord,
                                                        EsmpTimeInterval interval) {
        String resolution = consumptionRecord.getMeteringInterval();
        List<PointComplexType> points = new ArrayList<>();
        int position = 0;
        for (var consumptionPoint
                : consumptionRecord.getConsumptionPoints()) {
            PointComplexType point = new PointComplexType()
                    .withPosition("%d".formatted(position))
                    .withEnergyQuantityQuantity(BigDecimal.valueOf(consumptionPoint.getConsumption()))
                    .withEnergyQuantityQuality(
                            consumptionPoint.getMeteringType() == ConsumptionPoint.MeteringType.MEASURED_VALUE
                                    ? QualityTypeList.AS_PROVIDED
                                    : QualityTypeList.ADJUSTED);
            points.add(point);
            position++;
        }

        var seriesPeriod = new SeriesPeriodComplexType()
                .withResolution(resolution)
                .withTimeInterval(new ESMPDateTimeIntervalComplexType()
                                          .withStart(interval.start())
                                          .withEnd(interval.end())
                )
                .withPointList(
                        new SeriesPeriodComplexType.PointList()
                                .withPoints(points)
                );
        return List.of(seriesPeriod);
    }
}
