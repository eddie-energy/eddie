package energy.eddie.core.converters.v0_91_08;

import energy.eddie.cim.v0_91_08.*;
import energy.eddie.core.converters.UnsupportedUnitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

@Component
@SuppressWarnings("java:S101") // Names shouldn't contain underscores, but this is required to not have bean name clashes with the other MeasurementConverter
public class MeasurementConverterCIM_v0_91_08 {
    private static final Logger LOGGER = LoggerFactory.getLogger(MeasurementConverterCIM_v0_91_08.class);
    private final List<MeasurementCalculation> newCalculations;

    MeasurementConverterCIM_v0_91_08(
            List<MeasurementCalculation> newCalculations
    ) {
        this.newCalculations = newCalculations;
    }

    public VHDEnvelope convert(VHDEnvelope convertee) {
        LOGGER.info("Applying converters to validated historical data market document");
        var doc = convertee.getMarketDocument();
        var convertedTimeSeries = new ArrayList<TimeSeries>();
        for (var calculation : newCalculations) {
            if (doesNotContainTargetUnit(calculation, doc)) {
                convertedTimeSeries.addAll(convert(doc, calculation));
            }
        }
        doc.withTimeSeries(convertedTimeSeries);
        return convertee;
    }

    private boolean doesNotContainTargetUnit(
            MeasurementCalculation calculation,
            VHDMarketDocument convertee
    ) {
        for (var timeSeries : convertee.getTimeSeries()) {
            if (calculation.isTargetUnit(StandardUnitOfMeasureTypeList.fromValue(timeSeries.getEnergyMeasurementUnitName()))) {
                return false;
            }
        }
        return true;
    }

    private List<TimeSeries> convert(
            VHDMarketDocument convertee,
            MeasurementCalculation calculation
    ) {
        var convertedTimeSeries = new ArrayList<TimeSeries>();
        for (var timeSeries : convertee.getTimeSeries()) {
            var oldUnit = timeSeries.getEnergyMeasurementUnitName();
            ScaledUnit scaledUnit;
            try {
                scaledUnit = calculation.scaledUnit(StandardUnitOfMeasureTypeList.fromValue(oldUnit));
            } catch (UnsupportedUnitException e) {
                continue;
            }
            var seriesPeriods = new ArrayList<SeriesPeriod>();
            for (var seriesPeriod : timeSeries.getPeriods()) {
                var resolution = toJavaDuration(seriesPeriod.getResolution());
                var points = new ArrayList<Point>();
                for (var point : seriesPeriod.getPoints()) {
                    var measurement = point.getEnergyQuantityQuantity();
                    var newMeasurement = calculation.convert(measurement, resolution, scaledUnit.scale());
                    points.add(
                            new Point()
                                    .withEnergyQuantityQuantity(newMeasurement)
                                    .withEnergyQuantityQuality(StandardQualityTypeList.ADJUSTED.value())
                                    .withPosition(point.getPosition())
                    );
                }
                seriesPeriods.add(clone(seriesPeriod).withPoints(points));
            }
            convertedTimeSeries.add(
                    clone(timeSeries)
                            .withProduct(scaledUnit.energyProduct().value())
                            .withEnergyMeasurementUnitName(scaledUnit.unit().value())
                            .withPeriods(seriesPeriods)
            );
        }
        return convertedTimeSeries;
    }

    private static Duration toJavaDuration(javax.xml.datatype.Duration oldRes) {
        var now = Instant.now(Clock.systemUTC());
        GregorianCalendar calendar = GregorianCalendar.from(java.time.ZonedDateTime.ofInstant(now,
                                                                                              java.time.ZoneOffset.UTC));
        oldRes.addTo(calendar);

        Instant future = calendar.toInstant();
        return Duration.between(now, future);
    }

    private static TimeSeries clone(TimeSeries source) {
        return new TimeSeries()
                .withVersion(source.getVersion())
                .withDateAndOrTimeDateTime(source.getDateAndOrTimeDateTime())
                .withEnergyMeasurementUnitName(source.getEnergyMeasurementUnitName())
                .withFlowDirectionDirection(source.getFlowDirectionDirection())
                .withRegisteredResourceMRID(source.getRegisteredResourceMRID())
                .withRegisteredResourceFuelFuel(source.getRegisteredResourceFuelFuel())
                .withRegisteredResourceLocationMRID(source.getRegisteredResourceLocationMRID())
                .withRegisteredResourceLocationType(source.getRegisteredResourceLocationType())
                .withRegisteredResourceLocationCoordinateSystemCrsUrn(source.getRegisteredResourceLocationCoordinateSystemCrsUrn())
                .withRegisteredResourceLocationPositionPointsSequenceNumber(source.getRegisteredResourceLocationPositionPointsSequenceNumber())
                .withRegisteredResourceLocationPositionPointsXPosition(source.getRegisteredResourceLocationPositionPointsXPosition())
                .withRegisteredResourceLocationPositionPointsYPosition(source.getRegisteredResourceLocationPositionPointsYPosition())
                .withRegisteredResourceLocationPositionPointsZPosition(source.getRegisteredResourceLocationPositionPointsZPosition())
                .withRegisteredResourcePSRTypePsrType(source.getRegisteredResourcePSRTypePsrType())
                .withMarketEvaluationPointMRID(source.getMarketEvaluationPointMRID())
                .withMarketEvaluationPointMeterReadingsMRID(source.getMarketEvaluationPointMeterReadingsMRID())
                .withMarketEvaluationPointMeterReadingsReadingsMRID(source.getMarketEvaluationPointMeterReadingsReadingsMRID())
                .withMarketEvaluationPointMeterReadingsReadingsReadingTypeAccumulation(source.getMarketEvaluationPointMeterReadingsReadingsReadingTypeAccumulation())
                .withMarketEvaluationPointMeterReadingsReadingsReadingTypeAggregate(source.getMarketEvaluationPointMeterReadingsReadingsReadingTypeAggregate())
                .withMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity(source.getMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity())
                .withMarketEvaluationPointUsagePointLocationGeoInfoReference(source.getMarketEvaluationPointUsagePointLocationGeoInfoReference())
                .withReasonCode(source.getReasonCode())
                .withReasonText(source.getReasonText());
    }

    private static SeriesPeriod clone(SeriesPeriod source) {
        return new SeriesPeriod()
                .withResolution(source.getResolution())
                .withTimeInterval(source.getTimeInterval())
                .withReasonCode(source.getReasonCode())
                .withReasonText(source.getReasonText());
    }
}
