package energy.eddie.regionconnector.at.eda.utils;

import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.MeteringIntervall;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.at.eda.InvalidMappingException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MeteringIntervalUtilTest {

    private static Stream<Arguments> validMappings() {
        return Stream.of(
                Arguments.of(MeteringIntervall.D, Granularity.P1D),
                Arguments.of(MeteringIntervall.H, Granularity.PT1H),
                Arguments.of(MeteringIntervall.QH, Granularity.PT15M)
        );
    }

    @ParameterizedTest
    @MethodSource("validMappings")
    void toGranularity_returnsExpected(MeteringIntervall meteringInterval, Granularity expected) throws InvalidMappingException {
        var actual = MeteringIntervalUtil.toGranularity(meteringInterval);
        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @EnumSource(value = MeteringIntervall.class, names = {"D", "H", "QH"}, mode = EnumSource.Mode.EXCLUDE)
    void toGranularity_throwsInvalidMappingException(MeteringIntervall meteringInterval) {
        var exception = org.junit.jupiter.api.Assertions.assertThrows(InvalidMappingException.class, () -> MeteringIntervalUtil.toGranularity(meteringInterval));
        assertEquals("Unexpected MeteringInterval value: '" + meteringInterval + "'", exception.getMessage());
    }
}