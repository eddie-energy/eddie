package energy.eddie.regionconnector.es.datadis.filter;

import energy.eddie.regionconnector.es.datadis.dtos.Supply;
import energy.eddie.regionconnector.es.datadis.dtos.exceptions.NoSuppliesException;
import energy.eddie.regionconnector.es.datadis.dtos.exceptions.NoSupplyForMeteringPointException;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.List;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;

class SupplyMeteringPointFilterTest {

    @Test
    void filter_returnsFirstMatchingSupply() {
        // Arrange
        String expectedMeteringPointId = "expectedMeteringPointId";
        Supply supply1 = createSupply("notExpectedMeteringPointId");
        Supply supply2 = createSupply(expectedMeteringPointId);
        Supply supply3 = createSupply(expectedMeteringPointId);

        var filter = new SupplyMeteringPointFilter(List.of(supply1, supply2, supply3), expectedMeteringPointId);

        // Act & Assert
        StepVerifier.create(filter.filter())
                .expectNext(supply2)
                .verifyComplete();
    }

    @Test
    void filter_noMatch_returnsNoSupplyForMeteringPointException() {
        // Arrange
        String expectedMeteringPointId = "expectedMeteringPointId";
        Supply supply = createSupply("notExpectedMeteringPointId");

        var filter = new SupplyMeteringPointFilter(List.of(supply), expectedMeteringPointId);

        // Act & Assert
        StepVerifier.create(filter.filter())
                .expectError(NoSupplyForMeteringPointException.class)
                .verify();
    }

    @Test
    void filter_emptySupplies_returnsNoSuppliesException() {
        // Arrange
        var filter = new SupplyMeteringPointFilter(List.of(), "expectedMeteringPointId");

        // Act & Assert
        StepVerifier.create(filter.filter())
                .expectError(NoSuppliesException.class)
                .verify();
    }


    private Supply createSupply(String meteringPointId) {
        return new Supply("", meteringPointId, "", "", "", "", LocalDate.now(ZONE_ID_SPAIN), null, 1, "");
    }
}