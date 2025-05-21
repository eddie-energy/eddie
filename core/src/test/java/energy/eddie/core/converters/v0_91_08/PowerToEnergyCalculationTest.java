package energy.eddie.core.converters.v0_91_08;

import energy.eddie.cim.v0_91_08.StandardEnergyProductTypeList;
import energy.eddie.cim.v0_91_08.StandardUnitOfMeasureTypeList;
import energy.eddie.core.converters.UnsupportedUnitException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.stream.Stream;

import static energy.eddie.cim.v0_91_08.StandardEnergyProductTypeList.ACTIVE_ENERGY;
import static energy.eddie.cim.v0_91_08.StandardEnergyProductTypeList.REACTIVE_ENERGY;
import static energy.eddie.cim.v0_91_08.StandardUnitOfMeasureTypeList.*;
import static org.junit.jupiter.api.Assertions.*;

class PowerToEnergyCalculationTest {

    @ParameterizedTest
    @MethodSource
    void testConverts_powerToEnergy(int powerInt, Duration duration, int energy) {
        // Given
        var power = BigDecimal.valueOf(powerInt);
        var calculation = new PowerToEnergyCalculation();
        var expected = BigDecimal.valueOf(energy).setScale(2, RoundingMode.HALF_UP);

        // When
        var res = calculation.convert(power, duration, BigDecimal.TEN);


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
        var calculation = new PowerToEnergyCalculation();

        // When
        var scaledUnit = calculation.scaledUnit(unit);

        // Then
        assertAll(
                () -> assertEquals(expectedUnit, scaledUnit.unit()),
                () -> assertEquals(expectedEnergyProduct, scaledUnit.energyProduct())
        );
    }

    @Test
    void testConvertUnit_throwsOnInvalidUnit() {
        // Given
        var calculation = new PowerToEnergyCalculation();

        // When
        // Then
        assertThrows(UnsupportedUnitException.class, () -> calculation.scaledUnit(CELSIUS));
    }

    @ParameterizedTest
    @MethodSource
    void testIsTargetUnit_returns(StandardUnitOfMeasureTypeList unit, boolean expected) {
        // Given
        var calculation = new PowerToEnergyCalculation();

        // When
        var res = calculation.isTargetUnit(unit);

        // Then
        assertEquals(expected, res);
    }

    @Test
    void testConvertUnit_withZeroValues() {
        // Given
        var calculation = new PowerToEnergyCalculation();

        // When
        var res = calculation.convert(BigDecimal.ZERO, Duration.ofHours(1), BigDecimal.ONE);

        // Then
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), res);
    }

    @Test
    void testConvertUnit_with15MinuteGranularity() {
        // Given
        var calculation = new PowerToEnergyCalculation();

        // When
        var res = calculation.convert(BigDecimal.ONE, Duration.ofMinutes(15), BigDecimal.ONE);

        // Then
        assertNotEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), res);
    }

    private static Stream<Arguments> testIsTargetUnit_returns() {
        return Stream.of(
                Arguments.of(KILOWATT_HOUR, true),
                Arguments.of(KILOWATT, false)
        );
    }

    private static Stream<Arguments> testScaledUnit_returnsCorrect() {
        return Stream.of(
                Arguments.of(KILOWATT, KILOWATT_HOUR, ACTIVE_ENERGY),
                Arguments.of(GIGAWATT, GIGAWATT_HOUR, ACTIVE_ENERGY),
                Arguments.of(MEGAVOLT_AMPERE_REACTIVE, MEGAVOLT_AMPERE_REACTIVE_HOURS, REACTIVE_ENERGY),
                Arguments.of(KILOVOLT_AMPERE_REACTIVE, MEGAVOLT_AMPERE_REACTIVE_HOURS, REACTIVE_ENERGY),
                Arguments.of(MEGAWATT, MEGAWATT_HOURS, ACTIVE_ENERGY),
                Arguments.of(WATT, KILOWATT_HOUR, ACTIVE_ENERGY)
        );
    }

    private static Stream<Arguments> testConverts_powerToEnergy() {
        return Stream.of(
                Arguments.of(5, Duration.ofHours(1), 50),
                Arguments.of(5, Duration.ofDays(1), 1200)
        );
    }
}