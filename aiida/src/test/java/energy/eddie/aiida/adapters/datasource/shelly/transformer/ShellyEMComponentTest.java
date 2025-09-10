package energy.eddie.aiida.adapters.datasource.shelly.transformer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShellyEMComponentTest {
    @Test
    void fromKey_returnsEM() {
        var component = ShellyEMComponent.fromKey("em:0");
        assertEquals(ShellyEMComponent.EM, component);
        assertEquals(ShellyEMPhase.TOTAL, component.phase());
    }

    @Test
    void fromKey_returnsEM1_DATA_2() {
        var component = ShellyEMComponent.fromKey("em1data:2");
        assertEquals(ShellyEMComponent.EM1_DATA_2, component);
        assertEquals(ShellyEMPhase.PHASE_L3, component.phase());
    }

    @Test
    void fromKey_returnsUNKNOWN_forUnknownKey() {
        var component = ShellyEMComponent.fromKey("nonexistent:key");
        assertEquals(ShellyEMComponent.UNKNOWN, component);
        assertEquals(ShellyEMPhase.UNKNOWN, component.phase());
    }
}
