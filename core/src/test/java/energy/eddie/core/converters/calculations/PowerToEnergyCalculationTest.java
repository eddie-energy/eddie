package energy.eddie.core.converters.calculations;

import energy.eddie.cim.v0_82.vhd.EnergyProductTypeList;
import energy.eddie.cim.v0_82.vhd.UnitOfMeasureTypeList;
import energy.eddie.core.converters.UnsupportedUnitException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class PowerToEnergyCalculationTest {

    public static Stream<Arguments> testConverts_powerToEnergy() {
        return Stream.of(
                Arguments.of(5, "PT1H", 50),
                Arguments.of(5, "P1D", 1200)
        );
    }

    public static Stream<Arguments> testScaledUnit_returnsCorrect() {
        return Stream.of(
                Arguments.of(UnitOfMeasureTypeList.KILOWATT,
                             UnitOfMeasureTypeList.KILOWATT_HOUR,
                             EnergyProductTypeList.ACTIVE_ENERGY),
                Arguments.of(UnitOfMeasureTypeList.GIGAWATT,
                             UnitOfMeasureTypeList.GIGAWATT_HOUR,
                             EnergyProductTypeList.ACTIVE_ENERGY),
                Arguments.of(UnitOfMeasureTypeList.MEGAVOLT_AMPERE_REACTIVE,
                             UnitOfMeasureTypeList.MEGAVOLT_AMPERE_REACTIVE_HOURS,
                             EnergyProductTypeList.REACTIVE_ENERGY),
                Arguments.of(UnitOfMeasureTypeList.KILOVOLT_AMPERE_REACTIVE,
                             UnitOfMeasureTypeList.MEGAVOLT_AMPERE_REACTIVE_HOURS,
                             EnergyProductTypeList.REACTIVE_ENERGY),
                Arguments.of(UnitOfMeasureTypeList.MEGAWATT,
                             UnitOfMeasureTypeList.MEGAWATT_HOURS,
                             EnergyProductTypeList.ACTIVE_ENERGY),
                Arguments.of(UnitOfMeasureTypeList.WATT,
                             UnitOfMeasureTypeList.KILOWATT_HOUR,
                             EnergyProductTypeList.ACTIVE_ENERGY)
        );
    }

    public static Stream<Arguments> testIsTargetUnit_returns() {
        return Stream.of(
                Arguments.of(UnitOfMeasureTypeList.WATT, false),
                Arguments.of(UnitOfMeasureTypeList.KILOWATT_HOUR, true)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testConverts_powerToEnergy(int powerInt, String hours, int energy) {
        // Given
        var power = BigDecimal.valueOf(powerInt);
        var calculation = new PowerToEnergyCalculation();
        var expected = BigDecimal.valueOf(energy).setScale(2, RoundingMode.HALF_UP);

        // When
        var res = calculation.convert(power, hours, BigDecimal.TEN);


        // Then
        assertEquals(expected, res);
    }

    @ParameterizedTest
    @MethodSource
    void testScaledUnit_returnsCorrect(
            UnitOfMeasureTypeList unit,
            UnitOfMeasureTypeList expectedUnit,
            EnergyProductTypeList expectedEnergyProduct
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
        assertThrows(UnsupportedUnitException.class,
                     () -> calculation.scaledUnit(UnitOfMeasureTypeList.WATT_PER_SQUARE_METER));
    }

    @ParameterizedTest
    @MethodSource
    void testIsTargetUnit_returns(UnitOfMeasureTypeList unit, boolean expected) {
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
        var res = calculation.convert(BigDecimal.ZERO, "PT1H", BigDecimal.ONE);

        // Then
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), res);
    }

    @Test
    void testConvertUnit_with15MinuteGranularity() {
        // Given
        var calculation = new PowerToEnergyCalculation();

        // When
        var res = calculation.convert(BigDecimal.ONE, "PT15M", BigDecimal.ONE);

        // Then
        assertNotEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), res);
    }
}