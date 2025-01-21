package energy.eddie.regionconnector.be.fluvius;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FluviusRegionConnectorMetadataTest {

    public static Stream<Arguments> testGranularitiesFor_returnsCorrectGranularities() {
        return Stream.of(
                Arguments.of(EnergyType.ELECTRICITY, List.of(Granularity.PT15M, Granularity.P1D), false),
                Arguments.of(EnergyType.NATURAL_GAS, List.of(Granularity.PT30M, Granularity.P1D), false),
                Arguments.of(EnergyType.HYDROGEN, List.of(), false),
                Arguments.of(EnergyType.ELECTRICITY, List.of(Granularity.PT15M, Granularity.P1D), true),
                Arguments.of(EnergyType.NATURAL_GAS, List.of(Granularity.PT15M, Granularity.P1D), true)
        );
    }

    @Test
    void testSupportedGranularities_returnsPT15MIN_and_P1D_forSandbox() {
        // Given
        var metadata = new FluviusRegionConnectorMetadata(true);

        // When
        var res = metadata.supportedGranularities();

        // Then
        assertEquals(List.of(Granularity.PT15M, Granularity.P1D), res);
    }

    @Test
    void testSupportedGranularities_returnsElectricityAndGasGranularities_forProduction() {
        // Given
        var metadata = new FluviusRegionConnectorMetadata(false);

        // When
        var res = metadata.supportedGranularities();

        // Then
        assertEquals(List.of(Granularity.PT15M, Granularity.PT30M, Granularity.P1D), res);
    }

    @ParameterizedTest
    @MethodSource
    void testGranularitiesFor_returnsCorrectGranularities(
            EnergyType energyType,
            List<Granularity> granularities,
            boolean sandboxEnabled
    ) {
        // Given
        var metadate = new FluviusRegionConnectorMetadata(sandboxEnabled);

        // When
        var res = metadate.granularitiesFor(energyType);

        // Then
        assertEquals(granularities, res);
    }
}