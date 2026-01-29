// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.providers;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
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

        // When & Then
        assertThrows(UnsupportedUnitException.class, converterV082::scale);
        assertThrows(UnsupportedUnitException.class, converterV104::scale);
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

    private static List<Integer> allowedUnits() {
        return List.of(
                2, 5, 6, 9, 23, 29, 38, 39, 42, 45, 61, 63, 71, 72, 73
        );
    }
}