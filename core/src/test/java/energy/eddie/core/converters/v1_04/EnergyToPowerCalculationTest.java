package energy.eddie.core.converters.v1_04;

import energy.eddie.cim.v1_04.StandardEnergyProductTypeList;
import energy.eddie.cim.v1_04.StandardUnitOfMeasureTypeList;
import energy.eddie.core.converters.UnsupportedUnitException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class EnergyToPowerCalculationTest {
    @ParameterizedTest
    @MethodSource
    void testConverts_energyToPower(int energyInt, Duration resolution) {
        // Given
        var energy = BigDecimal.valueOf(energyInt);
        var calculation = new EnergyToPowerCalculation();
        var expected = BigDecimal.valueOf(5).setScale(1, RoundingMode.HALF_UP);

        // When
        var res = calculation.convert(energy, resolution, BigDecimal.ONE);

        // Then
        assertEquals(expected, res);
    }

    @ParameterizedTest
    @MethodSource
    void testScaledUnit_returnsCorrect(
            StandardUnitOfMeasureTypeList unit,
            StandardUnitOfMeasureTypeList expectedUnit,
            StandardEnergyProductTypeList expectedEnergyProduct
    ) throws UnsupportedUnitException {
        // Given
        var calculation = new EnergyToPowerCalculation();

        // When
        var scaledUnit = calculation.scaledUnit(unit);

        // Then
        assertAll(
                () -> assertEquals(expectedUnit, scaledUnit.unit()),
                () -> assertEquals(expectedEnergyProduct, scaledUnit.energyProduct()),
                () -> assertEquals(BigDecimal.ONE, scaledUnit.scale())
        );
    }

    @Test
    void testConvertUnit_throwsOnInvalidUnit() {
        // Given
        var calculation = new EnergyToPowerCalculation();

        // When
        // Then
        assertThrows(UnsupportedUnitException.class, () -> calculation.scaledUnit(StandardUnitOfMeasureTypeList.CELSIUS));
    }

    @ParameterizedTest
    @MethodSource
    void testIsTargetUnit_returns(StandardUnitOfMeasureTypeList unit, boolean expected) {
        // Given
        var calculation = new EnergyToPowerCalculation();

        // When
        var res = calculation.isTargetUnit(unit);

        // Then
        assertEquals(expected, res);
    }

    @Test
    void testConvertUnit_withZeroValues() {
        // Given
        var calculation = new EnergyToPowerCalculation();

        // When
        var res = calculation.convert(BigDecimal.ZERO, Duration.ofHours(1), BigDecimal.ONE);

        // Then
        assertEquals(BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP), res);
    }

    @Test
    void testConvertUnit_with15MinuteGranularity() {
        // Given
        var calculation = new EnergyToPowerCalculation();

        // When
        var res = calculation.convert(BigDecimal.ONE, Duration.ofMinutes(15), BigDecimal.ONE);

        // Then
        assertNotEquals(BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP), res);
    }

    private static Stream<Arguments> testIsTargetUnit_returns() {
        return Stream.of(
                Arguments.of(StandardUnitOfMeasureTypeList.WATT, true),
                Arguments.of(StandardUnitOfMeasureTypeList.KILOWATT_HOUR, false)
        );
    }

    private static Stream<Arguments> testScaledUnit_returnsCorrect() {
        return Stream.of(
                Arguments.of(StandardUnitOfMeasureTypeList.KILOWATT_HOUR,
                             StandardUnitOfMeasureTypeList.KILOWATT,
                             StandardEnergyProductTypeList.ACTIVE_POWER),
                Arguments.of(StandardUnitOfMeasureTypeList.GIGAWATT_HOUR,
                             StandardUnitOfMeasureTypeList.GIGAWATT,
                             StandardEnergyProductTypeList.ACTIVE_POWER),
                Arguments.of(StandardUnitOfMeasureTypeList.MEGAVOLT_AMPERE_REACTIVE_HOURS,
                             StandardUnitOfMeasureTypeList.MEGAVOLT_AMPERE_REACTIVE,
                             StandardEnergyProductTypeList.REACTIVE_POWER),
                Arguments.of(StandardUnitOfMeasureTypeList.MEGAWATT_HOURS,
                             StandardUnitOfMeasureTypeList.MEGAWATT,
                             StandardEnergyProductTypeList.ACTIVE_POWER)
        );
    }

    private static Stream<Arguments> testConverts_energyToPower() {
        return Stream.of(
                Arguments.of(5, Duration.ofHours(1)),
                Arguments.of(120, Duration.ofDays(1))
        );
    }
}