package energy.eddie.regionconnector.es.datadis.dtos;

import energy.eddie.regionconnector.es.datadis.MeteringDataProvider;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.ZONE_ID_SPAIN;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class IntermediateMeteringDataTest {
    @Test
    void fromMeteringData_setsStartAndEndAsExpected() throws IOException {
        ZonedDateTime start = ZonedDateTime.of(2023, 12, 1, 1, 0, 0, 0, ZONE_ID_SPAIN);
        ZonedDateTime end = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZONE_ID_SPAIN);

        IntermediateMeteringData intermediateMeteringData = IntermediateMeteringData.fromMeteringData(MeteringDataProvider.loadMeteringData());

        assertAll(
                () -> assertEquals(start, intermediateMeteringData.start()),
                () -> assertEquals(end, intermediateMeteringData.end())
        );
    }
}
