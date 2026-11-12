// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.providers;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import energy.eddie.cim.v1_12.StandardUnitOfMeasureTypeList;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.naesb.espi.ReadingType;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReadingTypeValueConverterTest {
    public static Stream<Arguments> testScale_throwsOnUnknownUnit() {
        return IntStream.range(0, 170)
                        .filter(o -> !allowedUnits().contains(o))
                        .mapToObj(Arguments::of);
    }

    public static Stream<Arguments> testScale_onKnownUnit_doesNotThrow() {
        return allowedUnits().stream()
                             .map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource
    @NullSource
    void testScale_throwsOnUnknownUnit(Integer unit) {
        // Given
        var readingType = new ReadingType()
                .withUom(String.valueOf(unit))
                .withPowerOfTenMultiplier("0");
        var converterV082 = ReadingTypeValueConverter.v082UnitOfMeasureTypeList(readingType);
        var converterV104 = ReadingTypeValueConverter.v104UnitOfMeasureTypeList(readingType);
        var converterV112 = ReadingTypeValueConverter.v112UnitOfMeasureTypeList(readingType);

        // When & Then
        assertThrows(UnsupportedUnitException.class, converterV082::scale);
        assertThrows(UnsupportedUnitException.class, converterV104::scale);
        assertThrows(UnsupportedUnitException.class, converterV112::scale);
    }

    @ParameterizedTest
    @MethodSource
    void testScale_onKnownUnit_doesNotThrow(int unit) {
        // Given
        var v082Converter = ReadingTypeValueConverter.v082UnitOfMeasureTypeList(0, String.valueOf(unit));
        var v104Converter = ReadingTypeValueConverter.v104UnitOfMeasureTypeList(0, String.valueOf(unit));

        // When & Then
        assertDoesNotThrow(v082Converter::scale);
        assertDoesNotThrow(v104Converter::scale);
    }

    @ParameterizedTest
    @CsvSource({
            "61,6,MEGAVOLTAMPERE,0",
            "38,0,WATT,0",
            "63,3,KILOVOLT_AMPERE_REACTIVE,-2",
            "71,6,MEGAVOLT_AMPERE_REACTIVE_HOURS,0",
            "72,0,KILOWATT_HOUR,-5",
            "73,6,MEGAVOLT_AMPERE_REACTIVE_HOURS,0",
            "29,3,KILOVOLT,-2",
            "5,0,AMPERE,0",
            "23,0,CELSIUS,0",
            "9,0,DEGREE_UNIT_OF_ANGLE,0",
            "39,2,HECTOPASCAL,0",
            "2,0,METRE,0",
            "42,0,CUBIC_METRE,0",
            "6,0,K_KELVIN,0",
            "45,0,CUBIC_METRES_PER_SECOND,0"
    })
    void directV112UnitsRetainExistingProviderFactorsAndPowerOfTenMultiplier(
            String unit, int multiplier, StandardUnitOfMeasureTypeList expectedUnit, int expectedScale
    ) throws UnsupportedUnitException {
        var reading = new ReadingType().withUom(unit).withPowerOfTenMultiplier(Integer.toString(multiplier));

        var scale = ReadingTypeValueConverter.v112UnitOfMeasureTypeList(reading).scale();

        assertEquals(expectedUnit, scale.unit());
        assertEquals(expectedScale, scale.scale());
        assertEquals(BigDecimal.valueOf(123).scaleByPowerOfTen(expectedScale), scale.scaled(123));
    }

    private static List<Integer> allowedUnits() {
        return List.of(
                2, 5, 6, 9, 23, 29, 38, 39, 42, 45, 61, 63, 71, 72, 73
        );
    }
}