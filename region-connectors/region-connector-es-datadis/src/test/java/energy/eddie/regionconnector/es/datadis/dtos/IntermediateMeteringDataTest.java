package energy.eddie.regionconnector.es.datadis.dtos;

import energy.eddie.regionconnector.es.datadis.MeteringDataProvider;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class IntermediateMeteringDataTest {
    @Test
    void fromMeteringData_setsStartAndEndAsExpected() throws IOException {
        LocalDate start = LocalDate.of(2023, 12, 1);
        LocalDate end = LocalDate.of(2024, 1, 1);

        IntermediateMeteringData intermediateMeteringData = IntermediateMeteringData.fromMeteringData(
                MeteringDataProvider.loadMeteringData()).block(Duration.ofMillis(10));

        assertAll(
                () -> assertEquals(start, intermediateMeteringData.start()),
                () -> assertEquals(end, intermediateMeteringData.end())
        );
    }
}
