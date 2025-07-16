package energy.eddie.core.converters;

import energy.eddie.cim.v0_82.vhd.*;
import energy.eddie.core.converters.calculations.MeasurementCalculation;
import energy.eddie.core.converters.calculations.ScaledUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A component that can convert the measurement of a validated historical data market document.
 */
@Component
public class MeasurementConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MeasurementConverter.class);
    private final List<MeasurementCalculation> calculations;

    public MeasurementConverter(List<MeasurementCalculation> calculations) {
        this.calculations = calculations;
    }

    public ValidatedHistoricalDataEnvelope convert(ValidatedHistoricalDataEnvelope convertee) {
        LOGGER.info("Applying converters to validated historical data market document");
        var doc = convertee.getValidatedHistoricalDataMarketDocument();
        var convertedTimeSeries = new ArrayList<TimeSeriesComplexType>();
        if (doc.getTimeSeriesList() == null) {
            LOGGER.atWarn()
                  .addArgument(() -> convertee.getMessageDocumentHeader()
                                              .getMessageDocumentHeaderMetaInformation()
                                              .getPermissionid())
                  .log("TimeSeries list is null for permission request {}");
            return convertee;
        }
        for (var calculation : calculations) {
            if (doesNotContainTargetUnit(calculation, doc)) {
                convert(doc, convertedTimeSeries, calculation);
            }
        }
        doc.getTimeSeriesList().withTimeSeries(convertedTimeSeries);
        return convertee;
    }

    private boolean doesNotContainTargetUnit(
            MeasurementCalculation calculation,
            ValidatedHistoricalDataMarketDocumentComplexType convertee
    ) {
        for (var timeSeries : convertee.getTimeSeriesList().getTimeSeries()) {
            if (calculation.isTargetUnit(timeSeries.getEnergyMeasurementUnitName())) {
                return false;
            }
        }
        return true;
    }

    private void convert(
            ValidatedHistoricalDataMarketDocumentComplexType convertee,
            List<TimeSeriesComplexType> convertedTimeSeries,
            MeasurementCalculation calculation
    ) {
        for (var timeSeries : convertee.getTimeSeriesList().getTimeSeries()) {
            var oldUnit = timeSeries.getEnergyMeasurementUnitName();
            ScaledUnit scaledUnit;
            try {
                scaledUnit = calculation.scaledUnit(oldUnit);
            } catch (UnsupportedUnitException e) {
                continue;
            }
            var converted = clone(timeSeries)
                    .withEnergyMeasurementUnitName(scaledUnit.unit())
                    .withProduct(scaledUnit.energyProduct());
            var seriesPeriods = new ArrayList<SeriesPeriodComplexType>();
            for (var seriesPeriod : timeSeries.getSeriesPeriodList().getSeriesPeriods()) {
                var resolution = seriesPeriod.getResolution();
                var points = new ArrayList<PointComplexType>();
                for (var point : seriesPeriod.getPointList().getPoints()) {
                    var measurement = point.getEnergyQuantityQuantity();
                    var newMeasurement = calculation.convert(measurement, resolution, scaledUnit.scale());
                    points.add(
                            new PointComplexType()
                                    .withEnergyQuantityQuantity(newMeasurement)
                                    .withEnergyQuantityQuality(QualityTypeList.ADJUSTED)
                                    .withPosition(point.getPosition())
                    );
                }
                seriesPeriods.add(
                        clone(seriesPeriod)
                                .withPointList(
                                        new SeriesPeriodComplexType.PointList()
                                                .withPoints(points)
                                )
                );
            }
            converted.withSeriesPeriodList(
                    new TimeSeriesComplexType.SeriesPeriodList()
                            .withSeriesPeriods(seriesPeriods)
            );
            convertedTimeSeries.add(converted);
        }
    }

    private TimeSeriesComplexType clone(TimeSeriesComplexType source) {
        return new TimeSeriesComplexType()
                .withMRID(UUID.randomUUID().toString())
                .withVersion(source.getVersion())
                .withBusinessType(source.getBusinessType())
                .withFlowDirectionDirection(source.getFlowDirectionDirection())
                .withReasonList(source.getReasonList())
                .withMarketEvaluationPointMeterReadingsMRID(source.getMarketEvaluationPointMeterReadingsMRID())
                .withMarketEvaluationPointMeterReadingsReadingsMRID(source.getMarketEvaluationPointMeterReadingsReadingsMRID())
                .withMarketEvaluationPointMeterReadingsReadingsReadingTypeAccumulate(source.getMarketEvaluationPointMeterReadingsReadingsReadingTypeAccumulate())
                .withInDomainMRID(source.getInDomainMRID())
                .withMarketEvaluationPointMRID(source.getMarketEvaluationPointMRID())
                .withMarketEvaluationPointMeterReadingsReadingsReadingTypeAggregation(source.getMarketEvaluationPointMeterReadingsReadingsReadingTypeAggregation())
                .withMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity(source.getMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity())
                .withRegisteredResource(source.getRegisteredResource())
                .withMarketEvaluationPointUsagePointLocationGeoInfoReference(source.getMarketEvaluationPointUsagePointLocationGeoInfoReference());
    }

    private SeriesPeriodComplexType clone(SeriesPeriodComplexType source) {
        return new SeriesPeriodComplexType()
                .withResolution(source.getResolution())
                .withReasonList(source.getReasonList())
                .withTimeInterval(source.getTimeInterval());
    }
}
