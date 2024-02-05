package energy.eddie.regionconnector.fr.enedis.providers.v0_82;


import energy.eddie.cim.v0_82.vhd.AggregateKind;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.cim.v0_82.vhd.SeriesPeriodComplexType;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataMarketDocument;
import energy.eddie.regionconnector.fr.enedis.config.PlainEnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.model.ConsumptionLoadCurveIntervalReading;
import energy.eddie.regionconnector.fr.enedis.model.ConsumptionLoadCurveMeterReading;
import energy.eddie.regionconnector.fr.enedis.model.ConsumptionLoadCurveReadingType;
import energy.eddie.regionconnector.fr.enedis.providers.agnostic.IdentifiableMeterReading;
import energy.eddie.regionconnector.shared.utils.EsmpTimeInterval;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static energy.eddie.cim.v0_82.vhd.EnergyProductTypeList.ACTIVE_POWER;
import static energy.eddie.regionconnector.fr.enedis.model.ConsumptionLoadCurveIntervalReading.IntervalLengthEnum.*;
import static energy.eddie.regionconnector.fr.enedis.providers.v0_82.IntermediateValidatedHistoricalDocument.ECT;
import static org.junit.jupiter.api.Assertions.*;

class IntermediateValidatedHistoricalDocumentTest {
    public static Stream<Arguments> testEddieValidatedHistoricalDataMarketDocument_returnsMappedDocument() {
        return Stream.of(
                Arguments.of(PT5M, "PT5M"),
                Arguments.of(PT10M, "PT10M"),
                Arguments.of(PT15M, "PT15M"),
                Arguments.of(PT30M, "PT30M"),
                Arguments.of(PT60M, "PT60M")
        );
    }

    @ParameterizedTest
    @MethodSource
    void testEddieValidatedHistoricalDataMarketDocument_returnsMappedDocument(
            ConsumptionLoadCurveIntervalReading.IntervalLengthEnum interval,
            String duration
    ) {
        // Given
        var intervalReading = new ConsumptionLoadCurveIntervalReading();
        intervalReading.setIntervalLength(interval);
        intervalReading.setValue("100");
        intervalReading.setMeasureType(ConsumptionLoadCurveIntervalReading.MeasureTypeEnum.B);
        String date = LocalDate.now(ECT).format(IntermediateValidatedHistoricalDocument.ENEDIS_DATE_FORMAT);
        intervalReading.setDate(date);
        var clcMeterReading = new ConsumptionLoadCurveMeterReading();
        clcMeterReading.setUsagePointId("uid");
        var start = LocalDate.now(ECT).atStartOfDay(ECT).minusDays(10);
        var end = LocalDate.now(ECT).atStartOfDay(ECT).minusDays(1);
        clcMeterReading.setStart(start.format(IntermediateValidatedHistoricalDocument.ENEDIS_DATE_FORMAT));
        clcMeterReading.setEnd(end.format(IntermediateValidatedHistoricalDocument.ENEDIS_DATE_FORMAT));
        ConsumptionLoadCurveReadingType readingType = new ConsumptionLoadCurveReadingType();
        readingType.setMeasurementKind("power");
        readingType.setAggregate("sum");
        clcMeterReading.setReadingType(readingType);
        var meterReading = new IdentifiableMeterReading("pid", "cid", "dnid", clcMeterReading);
        clcMeterReading.setIntervalReading(List.of(intervalReading));
        var intermediateVHD = new IntermediateValidatedHistoricalDocument(
                meterReading,
                () -> CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                new PlainEnedisConfiguration("clientId", "clientSecret", "basepath")
        );
        var esmpTimeInterval = new EsmpTimeInterval(start, end);
        // When
        var res = intermediateVHD.eddieValidatedHistoricalDataMarketDocument();


        ValidatedHistoricalDataMarketDocument marketDocument = res.marketDocument();
        var timeSeries = marketDocument.getTimeSeriesList().getTimeSeries().getFirst();
        SeriesPeriodComplexType seriesPeriod = timeSeries.getSeriesPeriodList().getSeriesPeriods().getFirst();

        // Then
        // The optional is checked, but intellij does not infer this.
        //noinspection OptionalGetWithoutIsPresent
        assertAll(
                () -> assertTrue(res.permissionId().isPresent()),
                () -> assertTrue(res.connectionId().isPresent()),
                () -> assertTrue(res.dataNeedId().isPresent()),
                () -> assertEquals("pid", res.permissionId().get()),
                () -> assertEquals("cid", res.connectionId().get()),
                () -> assertEquals("dnid", res.dataNeedId().get()),
                () -> assertEquals("clientId", marketDocument.getReceiverMarketParticipantMRID().getValue()),
                () -> assertEquals(esmpTimeInterval.start(), marketDocument.getPeriodTimeInterval().getStart()),
                () -> assertEquals(esmpTimeInterval.end(), marketDocument.getPeriodTimeInterval().getEnd()),
                () -> assertEquals(ACTIVE_POWER, timeSeries.getProduct()),
                () -> assertEquals(AggregateKind.SUM, timeSeries.getMarketEvaluationPointMeterReadingsReadingsReadingTypeAggregation()),
                () -> assertEquals("uid", timeSeries.getMarketEvaluationPointMRID().getValue()),
                () -> assertEquals(1, timeSeries.getSeriesPeriodList().getSeriesPeriods().size()),
                () -> assertEquals(duration, seriesPeriod.getResolution()),
                () -> assertEquals(esmpTimeInterval.start(), seriesPeriod.getTimeInterval().getStart()),
                () -> assertEquals(esmpTimeInterval.end(), seriesPeriod.getTimeInterval().getEnd()),
                () -> assertEquals("0", seriesPeriod.getPointList().getPoints().getFirst().getPosition()),
                () -> assertEquals(new BigDecimal(100), seriesPeriod.getPointList().getPoints().getFirst().getEnergyQuantityQuantity())
        );
    }

}