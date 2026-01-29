// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.core.converters;

import energy.eddie.cim.v0_82.vhd.EnergyProductTypeList;
import energy.eddie.cim.v0_82.vhd.UnitOfMeasureTypeList;
import energy.eddie.core.converters.calculations.EnergyToPowerCalculation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class EnergyToPowerCalculationTest {

    public static Stream<Arguments> testConverts_energyToPower() {
        return Stream.of(
                Arguments.of(5, "PT1H"),
                Arguments.of(120, "P1D")
        );
    }

    public static Stream<Arguments> testScaledUnit_returnsCorrect() {
        return Stream.of(
                Arguments.of(UnitOfMeasureTypeList.KILOWATT_HOUR,
                             UnitOfMeasureTypeList.KILOWATT,
                             EnergyProductTypeList.ACTIVE_POWER),
                Arguments.of(UnitOfMeasureTypeList.GIGAWATT_HOUR,
                             UnitOfMeasureTypeList.GIGAWATT,
                             EnergyProductTypeList.ACTIVE_POWER),
                Arguments.of(UnitOfMeasureTypeList.MEGAVOLT_AMPERE_REACTIVE_HOURS,
                             UnitOfMeasureTypeList.MEGAVOLT_AMPERE_REACTIVE, EnergyProductTypeList.REACTIVE_POWER),
                Arguments.of(UnitOfMeasureTypeList.MEGAWATT_HOURS,
                             UnitOfMeasureTypeList.MEGAWATT,
                             EnergyProductTypeList.ACTIVE_POWER)
        );
    }

    public static Stream<Arguments> testIsTargetUnit_returns() {
        return Stream.of(
                Arguments.of(UnitOfMeasureTypeList.WATT, true),
                Arguments.of(UnitOfMeasureTypeList.KILOWATT_HOUR, false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testConverts_energyToPower(int energyInt, String hours) {
        // Given
        var energy = BigDecimal.valueOf(energyInt);
        var calculation = new EnergyToPowerCalculation();
        var expected = BigDecimal.valueOf(5).setScale(1, RoundingMode.HALF_UP);

        // When
        var res = calculation.convert(energy, hours, BigDecimal.ONE);


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
        assertThrows(UnsupportedUnitException.class,
                     () -> calculation.scaledUnit(UnitOfMeasureTypeList.WATT_PER_SQUARE_METER));
    }

    @ParameterizedTest
    @MethodSource
    void testIsTargetUnit_returns(UnitOfMeasureTypeList unit, boolean expected) {
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
        var res = calculation.convert(BigDecimal.ZERO, "PT1H", BigDecimal.ONE);

        // Then
        assertEquals(BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP), res);
    }

    @Test
    void testConvertUnit_with15MinuteGranularity() {
        // Given
        var calculation = new EnergyToPowerCalculation();

        // When
        var res = calculation.convert(BigDecimal.ONE, "PT15M", BigDecimal.ONE);

        // Then
        assertNotEquals(BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP), res);
    }
}