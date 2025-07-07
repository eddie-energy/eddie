package energy.eddie.regionconnector.fi.fingrid.services;

import energy.eddie.api.CommonInformationModelVersions;
import energy.eddie.cim.v0_82.vhd.*;
import energy.eddie.regionconnector.fi.fingrid.client.model.Observation;
import energy.eddie.regionconnector.fi.fingrid.client.model.TimeSeries;
import energy.eddie.regionconnector.fi.fingrid.client.model.TimeSeriesData;
import energy.eddie.regionconnector.fi.fingrid.client.model.TimeSeriesResponse;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpDateTime;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;

import java.time.temporal.ChronoUnit;
import java.util.*;

final class IntermediateValidatedHistoricalDataMarketDocument {
    private final List<TimeSeriesResponse> timeSeriesResponses;

    IntermediateValidatedHistoricalDataMarketDocument(List<TimeSeriesResponse> timeSeriesResponses) {this.timeSeriesResponses = timeSeriesResponses;}

    public List<ValidatedHistoricalDataMarketDocumentComplexType> toVhds() {
        List<ValidatedHistoricalDataMarketDocumentComplexType> vhds = new ArrayList<>();
        for (var timeSeriesResponse : timeSeriesResponses) {
            vhds.add(toSingleVhd(timeSeriesResponse));
        }
        return vhds;
    }

    @SuppressWarnings("NullAway")
    // NullAway cannot handle the ternary operator combined with the boolean expressions
    private ValidatedHistoricalDataMarketDocumentComplexType toSingleVhd(TimeSeriesResponse timeSeriesResponse) {
        var data = timeSeriesResponse.data();
        var timeSeries = data.transaction().timeSeries();
        var hasTimeSeries = timeSeries != null && !timeSeries.isEmpty();
        var meteringPointEAN = hasTimeSeries ? timeSeries.getFirst().meteringPointEAN() : null;
        var unitOfMeasurement = hasTimeSeries
                ? UnitOfMeasureTypeList.fromValue(timeSeries.getFirst().unitType().toUpperCase(Locale.ROOT))
                : null;
        var seriesPeriods = hasTimeSeries
                ? getSeriesPeriods(data.transaction().timeSeries())
                : null;
        return new ValidatedHistoricalDataMarketDocumentComplexType()
                .withMRID(data.header().identification())
                .withRevisionNumber(CommonInformationModelVersions.V0_82.version())
                .withType(MessageTypeList.MEASUREMENT_VALUE_DOCUMENT)
                .withCreatedDateTime(new EsmpDateTime(data.header().creation()).toString())
                .withSenderMarketParticipantMarketRoleType(RoleTypeList.METERING_POINT_ADMINISTRATOR)
                .withReceiverMarketParticipantMarketRoleType(RoleTypeList.CONSUMER)
                .withProcessProcessType(ProcessTypeList.REALISED)
                .withSenderMarketParticipantMRID(
                        new PartyIDStringComplexType()
                                .withCodingScheme(CodingSchemeTypeList.FINLAND_NATIONAL_CODING_SCHEME)
                                .withValue(data.header().juridicalSenderParty().identification())
                )
                .withReceiverMarketParticipantMRID(
                        new PartyIDStringComplexType()
                                .withCodingScheme(CodingSchemeTypeList.FINLAND_NATIONAL_CODING_SCHEME)
                                .withValue(data.header().juridicalReceiverParty().identification())
                )
                .withTimeSeriesList(
                        new ValidatedHistoricalDataMarketDocumentComplexType.TimeSeriesList()
                                .withTimeSeries(
                                        new TimeSeriesComplexType()
                                                .withMRID(UUID.randomUUID().toString())
                                                .withProduct(EnergyProductTypeList.ACTIVE_ENERGY)
                                                .withFlowDirectionDirection(DirectionTypeList.UP_AND_DOWN)
                                                .withMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity(
                                                        CommodityKind.ELECTRICITYPRIMARYMETERED
                                                )
                                                .withEnergyMeasurementUnitName(unitOfMeasurement)
                                                .withMarketEvaluationPointMRID(
                                                        new MeasurementPointIDStringComplexType()
                                                                .withCodingScheme(CodingSchemeTypeList.FINLAND_NATIONAL_CODING_SCHEME)
                                                                .withValue(meteringPointEAN)
                                                )
                                                .withReasonList(
                                                        new TimeSeriesComplexType.ReasonList()
                                                                .withReasons(getReasons(data))
                                                )
                                                .withSeriesPeriodList(
                                                        new TimeSeriesComplexType.SeriesPeriodList()
                                                                .withSeriesPeriods(seriesPeriods)
                                                )
                                )
                );
    }

    private List<SeriesPeriodComplexType> getSeriesPeriods(List<TimeSeries> timeSeries) {
        var list = new ArrayList<SeriesPeriodComplexType>();
        for (var series : timeSeries) {
            var interval = new EsmpTimeInterval(series.start(), series.end());
            List<PointComplexType> result = new ArrayList<>();
            for (Observation obs : series.observations()) {
                var position = series.start().until(obs.start(), ChronoUnit.MINUTES) / series.resolutionDuration()
                                                                                             .minutes();
                PointComplexType pointComplexType = new PointComplexType()
                        .withPosition(String.valueOf(position))
                        .withEnergyQualityQuantityQuantity(obs.quantity())
                        .withEnergyQualityQuantityQuality(
                                obs.quality().equals("OK") ? QualityTypeList.AS_PROVIDED : QualityTypeList.NOT_AVAILABLE
                        );
                result.add(pointComplexType);
            }
            list.add(
                    new SeriesPeriodComplexType()
                            .withResolution(series.resolutionDuration().name())
                            .withTimeInterval(new ESMPDateTimeIntervalComplexType()
                                                      .withStart(interval.start())
                                                      .withEnd(interval.end()))
                            .withPointList(
                                    new SeriesPeriodComplexType.PointList()
                                            .withPoints(result)
                            )
            );
        }
        return list;
    }

    private static List<ReasonComplexType> getReasons(TimeSeriesData data) {
        if (data.transaction().eventReasons() == null) {
            return List.of(new ReasonComplexType().withCode(ReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED));
        }
        var reasons = new ArrayList<ReasonComplexType>();
        for (var reason : data.transaction().eventReasons().reasons()) {
            var item = new ReasonComplexType()
                    .withCode(ReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED)
                    .withText(reason.eventReasonText());
            reasons.add(item);
        }
        return reasons;
    }
}
