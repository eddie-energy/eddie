package energy.eddie.regionconnector.es.datadis.filter;

import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.dtos.Supply;
import energy.eddie.regionconnector.es.datadis.dtos.exceptions.InvalidPointAndMeasurementTypeCombinationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.stream.Stream;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;

class SupplyPointTypeAndMeasurementTypeCombinationFilterTest {

    private static Stream<Arguments> validCombinations() {
        return Stream.of(
                Arguments.of(1, MeasurementType.QUARTER_HOURLY),
                Arguments.of(2, MeasurementType.QUARTER_HOURLY),
                Arguments.of(3, MeasurementType.HOURLY),
                Arguments.of(4, MeasurementType.HOURLY)
        );
    }

    private static Stream<Arguments> invalidCombinations() {
        return Stream.of(
                Arguments.of(3, MeasurementType.QUARTER_HOURLY),
                Arguments.of(4, MeasurementType.QUARTER_HOURLY),
                Arguments.of(5, MeasurementType.QUARTER_HOURLY),
                Arguments.of(6, MeasurementType.QUARTER_HOURLY),
                Arguments.of(7, MeasurementType.QUARTER_HOURLY)
        );
    }


    @ParameterizedTest
    @MethodSource("validCombinations")
    void filter_returnsSupplyWithValidCombination(Integer pointType, MeasurementType measurementType) {
        // Arrange
        Supply supply = createSupply(pointType);

        SupplyPointTypeAndMeasurementTypeCombinationFilter filter = new SupplyPointTypeAndMeasurementTypeCombinationFilter(supply, measurementType);

        // Act & Assert
        StepVerifier.create(filter.filter())
                .expectNext(supply)
                .verifyComplete();
    }

    @ParameterizedTest
    @MethodSource("invalidCombinations")
    void filter_throwsInvalidPointAndMeasurementTypeCombinationException(Integer pointType, MeasurementType measurementType) {
        // Arrange
        Supply supply = createSupply(pointType);

        SupplyPointTypeAndMeasurementTypeCombinationFilter filter = new SupplyPointTypeAndMeasurementTypeCombinationFilter(supply, measurementType);

        // Act & Assert
        filter.filter().as(StepVerifier::create)
                .expectError(InvalidPointAndMeasurementTypeCombinationException.class)
                .verify();
    }

    private Supply createSupply(Integer pointType) {
        return new Supply("", "", "", "", "", "", LocalDate.now(ZONE_ID_SPAIN), null, pointType, "");
    }
}