package energy.eddie.core.converters;

import energy.eddie.cim.v0_82.vhd.*;
import energy.eddie.core.converters.calculations.EnergyToPowerCalculation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MeasurementConverterTest {
    @Test
    void testConvert_returnsValidatedHistoricalDataMarketDocument() {
        // Given
        var point = new PointComplexType()
                .withPosition("0")
                .withEnergyQuantityQuantity(BigDecimal.valueOf(5));
        var vhd = new ValidatedHistoricalDataMarketDocument()
                .withTimeSeriesList(
                        new ValidatedHistoricalDataMarketDocument.TimeSeriesList()
                                .withTimeSeries(
                                        new TimeSeriesComplexType()
                                                .withMarketEvaluationPointMRID(
                                                        new MeasurementPointIDStringComplexType()
                                                                .withValue("MRID")
                                                                .withCodingScheme(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME)
                                                )
                                                .withEnergyMeasurementUnitName(UnitOfMeasureTypeList.KILOWATT_HOUR)
                                                .withSeriesPeriodList(
                                                        new TimeSeriesComplexType.SeriesPeriodList()
                                                                .withSeriesPeriods(
                                                                        new SeriesPeriodComplexType()
                                                                                .withResolution("PT1H")
                                                                                .withPointList(
                                                                                        new SeriesPeriodComplexType.PointList()
                                                                                                .withPoints(point)
                                                                                )
                                                                )
                                                ),
                                        new TimeSeriesComplexType()
                                                .withMarketEvaluationPointMRID(
                                                        new MeasurementPointIDStringComplexType()
                                                                .withValue("MRID")
                                                                .withCodingScheme(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME)
                                                )
                                                .withEnergyMeasurementUnitName(UnitOfMeasureTypeList.KILOWATT_HOUR)
                                                .withSeriesPeriodList(
                                                        new TimeSeriesComplexType.SeriesPeriodList()
                                                                .withSeriesPeriods(
                                                                        new SeriesPeriodComplexType()
                                                                                .withResolution("PT1H")
                                                                                .withPointList(
                                                                                        new SeriesPeriodComplexType.PointList()
                                                                                                .withPoints(point)
                                                                                ),
                                                                        new SeriesPeriodComplexType()
                                                                                .withResolution("PT1H")
                                                                                .withPointList(
                                                                                        new SeriesPeriodComplexType.PointList()
                                                                                                .withPoints(point)
                                                                                )
                                                                )
                                                )
                                )
                );
        var converter = new MeasurementConverter(List.of(new EnergyToPowerCalculation()));

        // When
        var res = converter.convert(vhd);

        // Then
        assertEquals(4, res.getTimeSeriesList().getTimeSeries().size());
        var timeSeries = res.getTimeSeriesList().getTimeSeries().getLast();
        assertAll(
                () -> assertEquals(UnitOfMeasureTypeList.KILOWATT,
                                   timeSeries.getEnergyMeasurementUnitName()),
                () -> assertEquals(EnergyProductTypeList.ACTIVE_POWER,
                                   timeSeries.getProduct()),
                () -> assertEquals(2,
                                   timeSeries
                                           .getSeriesPeriodList()
                                           .getSeriesPeriods()
                                           .size()),
                () -> assertEquals(1,
                                   timeSeries
                                           .getSeriesPeriodList()
                                           .getSeriesPeriods()
                                           .getFirst()
                                           .getPointList()
                                           .getPoints()
                                           .size()),
                () -> assertEquals("MRID", timeSeries.getMarketEvaluationPointMRID().getValue()),
                () -> assertEquals(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                                   timeSeries.getMarketEvaluationPointMRID().getCodingScheme())
        );
    }

    @Test
    void testConvert_returnsValidatedHistoricalDataMarketDocumentWithoutExtraPowerTimeSeries() {
        // Given
        var point = new PointComplexType()
                .withPosition("0")
                .withEnergyQuantityQuantity(BigDecimal.valueOf(5));
        var vhd = new ValidatedHistoricalDataMarketDocument()
                .withTimeSeriesList(
                        new ValidatedHistoricalDataMarketDocument.TimeSeriesList()
                                .withTimeSeries(
                                        new TimeSeriesComplexType()
                                                .withEnergyMeasurementUnitName(UnitOfMeasureTypeList.KILOWATT_HOUR)
                                                .withSeriesPeriodList(
                                                        new TimeSeriesComplexType.SeriesPeriodList()
                                                                .withSeriesPeriods(
                                                                        new SeriesPeriodComplexType()
                                                                                .withResolution("PT1H")
                                                                                .withPointList(
                                                                                        new SeriesPeriodComplexType.PointList()
                                                                                                .withPoints(point)
                                                                                )
                                                                )
                                                ),
                                        new TimeSeriesComplexType()
                                                .withEnergyMeasurementUnitName(UnitOfMeasureTypeList.KILOWATT)
                                                .withSeriesPeriodList(
                                                        new TimeSeriesComplexType.SeriesPeriodList()
                                                                .withSeriesPeriods(
                                                                        new SeriesPeriodComplexType()
                                                                                .withResolution("PT1H")
                                                                                .withPointList(
                                                                                        new SeriesPeriodComplexType.PointList()
                                                                                                .withPoints(point)
                                                                                )
                                                                )
                                                )
                                )
                );
        var converter = new MeasurementConverter(List.of(new EnergyToPowerCalculation()));

        // When
        var res = converter.convert(vhd);

        // Then
        assertEquals(2, res.getTimeSeriesList().getTimeSeries().size());
        var timeSeries = res.getTimeSeriesList().getTimeSeries().getFirst();
        assertEquals(1,
                     timeSeries
                             .getSeriesPeriodList()
                             .getSeriesPeriods()
                             .size());
    }
}