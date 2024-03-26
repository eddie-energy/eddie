package energy.eddie.regionconnector.es.datadis.providers.agnostic;

import energy.eddie.regionconnector.es.datadis.dtos.IntermediateMeteringData;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;
import static org.junit.jupiter.api.Assertions.assertEquals;

class IdentifiableMeteringDataTest {

    @Test
    void extractMeterReadingEndDate_returnsExpected() {
        // Arrange
        LocalDate start = LocalDate.now(ZONE_ID_SPAIN);
        LocalDate end = start.plusDays(1);
        var intermediateMeteringData = new IntermediateMeteringData(List.of(), start, end);
        var identifiableMeteringData = new IdentifiableMeteringData(null, intermediateMeteringData);

        // Act
        var result = identifiableMeteringData.meterReadingEndDate();

        // Assert
        assertEquals(end, result);
    }
}
