package energy.eddie.regionconnector.be.fluvius;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FluviusRegionConnectorMetadataTest {

    public static Stream<Arguments> testGranularitiesFor_returnsCorrectGranularities() {
        return Stream.of(
                Arguments.of(EnergyType.ELECTRICITY, List.of(Granularity.PT15M)),
                Arguments.of(EnergyType.NATURAL_GAS, List.of(Granularity.PT30M)),
                Arguments.of(EnergyType.HYDROGEN, List.of())
        );
    }

    @ParameterizedTest
    @MethodSource
    void testGranularitiesFor_returnsCorrectGranularities(EnergyType energyType, List<Granularity> granularities) {
        // Given
        var metadate = FluviusRegionConnectorMetadata.getInstance();

        // When
        var res = metadate.granularitiesFor(energyType);

        // Then
        assertEquals(granularities, res);
    }
}