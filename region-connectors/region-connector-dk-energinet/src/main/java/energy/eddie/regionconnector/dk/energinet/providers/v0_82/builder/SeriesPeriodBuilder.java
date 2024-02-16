package energy.eddie.regionconnector.dk.energinet.providers.v0_82.builder;

import energy.eddie.cim.v0_82.vhd.*;
import energy.eddie.regionconnector.dk.energinet.customer.model.Period;
import energy.eddie.regionconnector.dk.energinet.customer.model.Point;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public class SeriesPeriodBuilder {

    private final TimeSeriesComplexType.SeriesPeriodList seriesPeriodList = new TimeSeriesComplexType.SeriesPeriodList();

    public SeriesPeriodBuilder withPeriods(List<Period> periodList) {
        periodList.forEach(period -> {
            var timeInterval = Objects.requireNonNull(period.getTimeInterval());
            var pointList = Objects.requireNonNull(period.getPoint());
            var periodComplexType = new SeriesPeriodComplexType()
                    .withResolution(Objects.requireNonNull(period.getResolution()))
                    .withTimeInterval(
                            new ESMPDateTimeIntervalComplexType()
                                    .withStart(Objects.requireNonNull(timeInterval.getStart()))
                                    .withEnd(Objects.requireNonNull(timeInterval.getEnd()))
                    )
                    .withPointList(fromPointList(pointList))
                    .withReasonList(new SeriesPeriodComplexType.ReasonList());

            seriesPeriodList.withSeriesPeriods(periodComplexType);
        });

        return this;
    }

    private SeriesPeriodComplexType.PointList fromPointList(List<Point> pointList) {
        var list = new SeriesPeriodComplexType.PointList();
        pointList.forEach(point -> {
            var pointComplexType = new PointComplexType()
                    .withPosition(Objects.requireNonNull(point.getPosition()))
                    .withEnergyQuantityQuantity(BigDecimal.valueOf(Double.parseDouble(Objects.requireNonNull(point.getOutQuantityQuantity()))))
                    .withEnergyQuantityQuality(QualityTypeList.fromValue(Objects.requireNonNull(point.getOutQuantityQuality())));

            list.withPoints(pointComplexType);
        });

        return list;
    }

    public TimeSeriesComplexType.SeriesPeriodList build() {
        return seriesPeriodList;
    }
}