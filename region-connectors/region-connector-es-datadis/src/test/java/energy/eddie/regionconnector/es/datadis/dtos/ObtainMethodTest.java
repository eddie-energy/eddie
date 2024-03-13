package energy.eddie.regionconnector.es.datadis.dtos;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ObtainMethodTest {


    public static Stream<Arguments> obtainMethodProvider() {
        return Stream.of(
                Arguments.of(ObtainMethod.REAL, "Real"),
                Arguments.of(ObtainMethod.REAL, "REAL"),
                Arguments.of(ObtainMethod.ESTIMATED, "Estimada"),
                Arguments.of(ObtainMethod.ESTIMATED, "ESTIMADA"),
                Arguments.of(ObtainMethod.UNKNOWN, ""),
                Arguments.of(ObtainMethod.UNKNOWN, null),
                Arguments.of(ObtainMethod.UNKNOWN, "xxx")
        );
    }

    @ParameterizedTest
    @MethodSource("obtainMethodProvider")
    void fromString(ObtainMethod expectedMethod, String text) {
        assertEquals(expectedMethod, ObtainMethod.fromString(text));
    }
}
