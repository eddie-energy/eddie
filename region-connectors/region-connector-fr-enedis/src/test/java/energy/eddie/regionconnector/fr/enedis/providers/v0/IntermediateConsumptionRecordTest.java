package energy.eddie.regionconnector.fr.enedis.providers.v0;

import energy.eddie.regionconnector.fr.enedis.model.ConsumptionLoadCurveIntervalReading;
import energy.eddie.regionconnector.fr.enedis.model.ConsumptionLoadCurveMeterReading;
import energy.eddie.regionconnector.fr.enedis.providers.agnostic.IdentifiableMeterReading;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

import static energy.eddie.api.v0.ConsumptionPoint.MeteringType.MEASURED_VALUE;
import static energy.eddie.regionconnector.fr.enedis.model.ConsumptionLoadCurveIntervalReading.IntervalLengthEnum.*;
import static energy.eddie.regionconnector.fr.enedis.model.ConsumptionLoadCurveIntervalReading.MeasureTypeEnum.B;
import static org.junit.jupiter.api.Assertions.*;

class IntermediateConsumptionRecordTest {
    public static Stream<Arguments> testConsumptionRecord_withIntervalsAndMeasurementTypes() {
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
    void testConsumptionRecord_withIntervalsAndMeasurementTypes(ConsumptionLoadCurveIntervalReading.IntervalLengthEnum interval, String duration) {
        // Given
        var intervalReading = new ConsumptionLoadCurveIntervalReading();
        intervalReading.setIntervalLength(interval);
        intervalReading.setValue("100");
        intervalReading.setMeasureType(B);
        var clcMeterReading = new ConsumptionLoadCurveMeterReading();
        clcMeterReading.setUsagePointId("uid");
        clcMeterReading.setStart(ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE));
        var meterReading = new IdentifiableMeterReading("pid", "cid", "dnid", clcMeterReading);
        clcMeterReading.setIntervalReading(List.of(intervalReading));
        var intermediateRecord = new IntermediateConsumptionRecord(meterReading);

        // When
        var result = intermediateRecord.consumptionRecord();

        // Then
        assertAll(
                () -> assertEquals("pid", result.getPermissionId()),
                () -> assertEquals("cid", result.getConnectionId()),
                () -> assertEquals("dnid", result.getDataNeedId()),
                () -> assertEquals(1, result.getConsumptionPoints().size()),
                () -> assertEquals(100, result.getConsumptionPoints().get(0).getConsumption()),
                () -> assertEquals(MEASURED_VALUE, result.getConsumptionPoints().get(0).getMeteringType()),
                () -> assertEquals(duration, result.getMeteringInterval().toString())
        );
    }

    @Test
    void testConsumptionRecord_withEmptyIntervalReading() {
        // Given
        var clcMeterReading = new ConsumptionLoadCurveMeterReading();
        clcMeterReading.setIntervalReading(List.of());
        var meterReading = new IdentifiableMeterReading("pid", "cid", "dnid", clcMeterReading);
        var intermediateRecord = new IntermediateConsumptionRecord(meterReading);

        // When, Then
        assertThrows(IllegalStateException.class, intermediateRecord::consumptionRecord);
    }

}