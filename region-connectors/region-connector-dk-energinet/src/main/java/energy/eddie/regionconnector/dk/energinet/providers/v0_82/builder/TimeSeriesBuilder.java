// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.dk.energinet.providers.v0_82.builder;

import energy.eddie.cim.v0_82.vhd.*;
import energy.eddie.regionconnector.dk.energinet.customer.model.TimeSeries;

import java.util.List;
import java.util.Objects;

public class TimeSeriesBuilder {

    private final ValidatedHistoricalDataMarketDocumentComplexType.TimeSeriesList timeSeriesList = new ValidatedHistoricalDataMarketDocumentComplexType.TimeSeriesList();

    private final SeriesPeriodBuilderFactory seriesPeriodBuilderFactory;

    public TimeSeriesBuilder(SeriesPeriodBuilderFactory seriesPeriodBuilderFactory) {
        this.seriesPeriodBuilderFactory = seriesPeriodBuilderFactory;
    }

    public TimeSeriesBuilder withTimeSeriesList(List<TimeSeries> timeSeriesList) {
        timeSeriesList.forEach(timeSeries -> {
            var businessType = BusinessTypeList.fromValue(Objects.requireNonNull(timeSeries.getBusinessType()));
            var marketEvaluationPoint = Objects.requireNonNull(Objects.requireNonNull(timeSeries.getMarketEvaluationPoint()).getmRID());
            var periodList = Objects.requireNonNull(timeSeries.getPeriod());

            var timeSeriesComplexType = new TimeSeriesComplexType()
                    .withMRID(Objects.requireNonNull(timeSeries.getmRID()))
                    .withBusinessType(businessType)
                    .withProduct(EnergyProductTypeList.ACTIVE_ENERGY)
                    .withFlowDirectionDirection(businessType == BusinessTypeList.CONSUMPTION ? DirectionTypeList.DOWN : DirectionTypeList.UP)
                    .withMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity(CommodityKind.ELECTRICITYPRIMARYMETERED)
                    .withEnergyMeasurementUnitName(UnitOfMeasureTypeList.fromValue(Objects.requireNonNull(timeSeries.getMeasurementUnitName())))
                    .withMarketEvaluationPointMRID(
                            new MeasurementPointIDStringComplexType()
                                    .withValue(marketEvaluationPoint.getName())
                                    .withCodingScheme(CodingSchemeTypeList.fromValue(marketEvaluationPoint.getCodingScheme()))
                    )
                    .withReasonList(new TimeSeriesComplexType.ReasonList().withReasons(new ReasonComplexType().withCode(ReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED)))
                    .withSeriesPeriodList(seriesPeriodBuilderFactory.create().withPeriods(periodList).build());

            this.timeSeriesList.withTimeSeries(timeSeriesComplexType);
        });

        return this;
    }

    public ValidatedHistoricalDataMarketDocumentComplexType.TimeSeriesList build() {
        return timeSeriesList;
    }
}