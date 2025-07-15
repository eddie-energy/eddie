package energy.eddie.regionconnector.simulation.permission.request;

import energy.eddie.api.CommonInformationModelVersions;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.cim.v0_82.vhd.*;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpDateTime;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;
import energy.eddie.regionconnector.shared.cim.v0_82.vhd.VhdEnvelope;
import energy.eddie.regionconnector.simulation.SimulationConnectorMetadata;
import energy.eddie.regionconnector.simulation.dtos.Measurement;
import energy.eddie.regionconnector.simulation.dtos.SimulatedMeterReading;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record IntermediateValidatedHistoricalDataMarketDocument(SimulatedMeterReading simulatedMeterReading,
                                                                CommonInformationModelConfiguration cimConfig) {
    private static final TimeSeriesComplexType.ReasonList REASON_LIST = new TimeSeriesComplexType.ReasonList()
            .withReasons(
                    new ReasonComplexType()
                            .withCode(ReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED));

    public ValidatedHistoricalDataEnvelope value() {
        ZonedDateTime endDate = simulatedMeterReading
                .startDateTime()
                .plus(Duration.parse(simulatedMeterReading.meteringInterval())
                              .multipliedBy(simulatedMeterReading.measurements().size())
                );
        var timeframe = new EsmpTimeInterval(
                simulatedMeterReading.startDateTime(),
                endDate
        );

        var vhd = new ValidatedHistoricalDataMarketDocumentComplexType()
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
                .withReceiverMarketParticipantMRID(
                        new PartyIDStringComplexType()
                                .withCodingScheme(cimConfig.eligiblePartyNationalCodingScheme())
                                .withValue(SimulationConnectorMetadata.REGION_CONNECTOR_ID)
                )
                .withPeriodTimeInterval(
                        new ESMPDateTimeIntervalComplexType()
                                .withStart(timeframe.start())
                                .withEnd(timeframe.end())
                )
                .withTimeSeriesList(
                        new ValidatedHistoricalDataMarketDocumentComplexType.TimeSeriesList()
                                .withTimeSeries(
                                        new TimeSeriesComplexType()
                                                .withMRID(UUID.randomUUID().toString())
                                                .withBusinessType(BusinessTypeList.CONSUMPTION)
                                                .withProduct(EnergyProductTypeList.ACTIVE_ENERGY)
                                                .withVersion("1.0")
                                                .withFlowDirectionDirection(DirectionTypeList.DOWN)
                                                .withMarketEvaluationPointMeterReadingsReadingsReadingTypeAggregation(
                                                        AggregateKind.SUM)
                                                .withMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity(
                                                        CommodityKind.NONE)
                                                .withEnergyMeasurementUnitName(UnitOfMeasureTypeList.WATT)
                                                .withMarketEvaluationPointMRID(
                                                        new MeasurementPointIDStringComplexType()
                                                                .withCodingScheme(
                                                                        CodingSchemeTypeList.FRANCE_NATIONAL_CODING_SCHEME)
                                                                .withValue(
                                                                        simulatedMeterReading.meteringPoint())
                                                )
                                                .withReasonList(REASON_LIST)
                                                .withSeriesPeriodList(
                                                        new TimeSeriesComplexType.SeriesPeriodList()
                                                                .withSeriesPeriods(
                                                                        seriesPeriods(
                                                                                simulatedMeterReading,
                                                                                timeframe
                                                                        )
                                                                )
                                                )
                                )
                );

        return new VhdEnvelope(
                vhd,
                new SimulationPermissionRequest(
                        simulatedMeterReading.connectionId(),
                        simulatedMeterReading.permissionId(),
                        simulatedMeterReading.dataNeedId(),
                        PermissionProcessStatus.ACCEPTED
                )
        ).wrap();
    }

    private List<SeriesPeriodComplexType> seriesPeriods(
            SimulatedMeterReading simulatedMeterReading,
            EsmpTimeInterval interval
    ) {
        String resolution = simulatedMeterReading.meteringInterval();
        List<PointComplexType> points = new ArrayList<>();
        int position = 0;
        for (var measurement : simulatedMeterReading.measurements()) {
            PointComplexType point = new PointComplexType()
                    .withPosition("%d".formatted(position))
                    .withEnergyQuantityQuantity(BigDecimal.valueOf(measurement.value()))
                    .withEnergyQuantityQuality(
                            measurement.measurementType() == Measurement.MeasurementType.MEASURED
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
