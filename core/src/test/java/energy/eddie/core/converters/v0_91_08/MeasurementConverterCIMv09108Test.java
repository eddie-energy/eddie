package energy.eddie.core.converters.v0_91_08;

import energy.eddie.cim.v0_91_08.*;
import org.junit.jupiter.api.Test;

import javax.xml.datatype.DatatypeFactory;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MeasurementConverterCIMv09108Test {
    @Test
    void testConvert_returnsValidatedHistoricalDataMarketDocument() {
        // Given
        var point = new Point()
                .withPosition(0)
                .withEnergyQuantityQuantity(BigDecimal.valueOf(5));
        var resolution15Min = DatatypeFactory.newDefaultInstance()
                                             .newDuration(Duration.ofMinutes(15).toMillis());
        var resolution1H = DatatypeFactory.newDefaultInstance()
                                          .newDuration(Duration.ofHours(1).toMillis());
        var vhd = new VHDMarketDocument()
                .withTimeSeries(
                        new TimeSeries()
                                .withMarketEvaluationPointMRID(
                                        new MeasurementPointIDString()
                                                .withValue("MRID")
                                                .withCodingScheme(StandardCodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME.value())
                                )
                                .withEnergyMeasurementUnitName(StandardUnitOfMeasureTypeList.KILOWATT_HOUR.value())
                                .withProduct(StandardEnergyProductTypeList.ACTIVE_ENERGY.value())
                                .withPeriods(
                                        new SeriesPeriod()
                                                .withResolution(resolution15Min)
                                                .withPoints(point)
                                ),
                        new TimeSeries()
                                .withMarketEvaluationPointMRID(
                                        new MeasurementPointIDString()
                                                .withValue("MRID")
                                                .withCodingScheme(StandardCodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME.value())
                                )
                                .withEnergyMeasurementUnitName(StandardUnitOfMeasureTypeList.KILOWATT_HOUR.value())
                                .withPeriods(
                                        new SeriesPeriod()
                                                .withResolution(resolution15Min)
                                                .withPoints(point),
                                        new SeriesPeriod()
                                                .withResolution(resolution1H)
                                                .withPoints(point)
                                )
                );
        var envelope = new VHDEnvelope().withMarketDocument(vhd);
        var converter = new MeasurementConverterCIM_v0_91_08(List.of(new EnergyToPowerCalculation()));

        // When
        var res = converter.convert(envelope);

        // Then
        var resVhd = res.getMarketDocument();
        assertEquals(4, resVhd.getTimeSeries().size());
        var timeSeries = resVhd.getTimeSeries().getLast();
        assertAll(
                () -> assertEquals(StandardUnitOfMeasureTypeList.KILOWATT.value(), timeSeries.getEnergyMeasurementUnitName()),
                () -> assertEquals(StandardEnergyProductTypeList.ACTIVE_POWER.value(), timeSeries.getProduct()),
                () -> assertEquals(2, timeSeries.getPeriods().size()),
                () -> assertEquals(1, timeSeries.getPeriods().getFirst().getPoints().size()),
                () -> assertEquals("MRID", timeSeries.getMarketEvaluationPointMRID().getValue()),
                () -> assertEquals(StandardCodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME.value(),
                                   timeSeries.getMarketEvaluationPointMRID().getCodingScheme())
        );
    }

    @Test
    void testConvert_returnsValidatedHistoricalDataMarketDocumentWithoutExtraPowerTimeSeries() {
        // Given
        var resolution1H = DatatypeFactory.newDefaultInstance()
                                          .newDuration(Duration.ofHours(1).toMillis());
        var point = new Point()
                .withPosition(0)
                .withEnergyQuantityQuantity(BigDecimal.valueOf(5));
        var vhd = new VHDMarketDocument()
                .withTimeSeries(
                        new TimeSeries()
                                .withEnergyMeasurementUnitName(StandardUnitOfMeasureTypeList.KILOWATT_HOUR.value())
                                .withPeriods(
                                        new SeriesPeriod()
                                                .withResolution(resolution1H)
                                                .withPoints(point)
                                ),
                        new TimeSeries()
                                .withEnergyMeasurementUnitName(StandardUnitOfMeasureTypeList.KILOWATT.value())
                                .withPeriods(
                                        new SeriesPeriod()
                                                .withResolution(resolution1H)
                                                .withPoints(point)
                                )
                );
        var envelope = new VHDEnvelope().withMarketDocument(vhd);
        var converter = new MeasurementConverterCIM_v0_91_08(List.of(new EnergyToPowerCalculation()));

        // When
        var res = converter.convert(envelope);

        // Then
        var resVhd = res.getMarketDocument();
        assertEquals(2, resVhd.getTimeSeries().size());
        var timeSeries = resVhd.getTimeSeries().getFirst();
        assertEquals(1, timeSeries.getPeriods().size());
    }
}