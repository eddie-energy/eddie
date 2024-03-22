package energy.eddie.regionconnector.dk.energinet.filter;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnerginetResolutionTest {

    @ParameterizedTest
    @CsvSource({
            "PT15M, PT15M",
            "PT1H, PT1H",
            "PT1D, P1D",
            "P1M, P1M",
            "PT1Y, P1Y"
    })
    void toISO8601Duration(String resolution, String expected) {
        var filter = new EnerginetResolution(resolution);
        assertEquals(expected, filter.toISO8601Duration());
    }
}
