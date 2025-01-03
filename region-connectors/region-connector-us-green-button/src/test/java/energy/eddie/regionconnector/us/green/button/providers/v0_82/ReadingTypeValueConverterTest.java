package energy.eddie.regionconnector.us.green.button.providers.v0_82;

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
        var converter = new ReadingTypeValueConverter(readingType);

        // When & Then
        assertThrows(UnsupportedUnitException.class, converter::scale);
    }

    @ParameterizedTest
    @MethodSource
    void testScale_onKnownUnit_doesNotThrow(int unit) {
        // Given
        var converter = new ReadingTypeValueConverter(0, String.valueOf(unit));

        // When & Then
        assertDoesNotThrow(converter::scale);
    }

    private static List<Integer> allowedUnits() {
        return List.of(
                2, 5, 6, 9, 23, 29, 38, 39, 42, 45, 61, 63, 71, 72, 73
        );
    }
}