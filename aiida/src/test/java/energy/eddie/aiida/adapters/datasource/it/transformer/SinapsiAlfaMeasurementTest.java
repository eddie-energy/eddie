package energy.eddie.aiida.adapters.datasource.it.transformer;

import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.api.agnostic.aiida.UnitOfMeasurement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SinapsiAlfaMeasurementTest {
    @Test
    void constructor_setsFieldsCorrectly() {
        // Given
        var entryKey = "1-0:1.7.0.255_3,0_2";
        var rawValue = "123";

        // When
        var measurement = new SinapsiAlfaMeasurement(entryKey, rawValue);

        // Then
        assertEquals(ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER, measurement.obisCode());
        assertEquals(UnitOfMeasurement.WATT, measurement.rawUnitOfMeasurement());
        assertEquals(rawValue, measurement.rawValue());
        assertEquals(entryKey, measurement.entryKey());
    }
}
