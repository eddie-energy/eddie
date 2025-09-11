package energy.eddie.aiida.adapters.datasource.shelly.transformer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShellyComponentTest {
    @Test
    void fromKey_returnsEM() {
        var component = ShellyComponent.fromKey("em:0");
        assertEquals(ShellyComponent.EM, component);
        assertEquals(ShellyPhase.TOTAL, component.phase());
    }

    @Test
    void fromKey_returnsEM1_DATA_2() {
        var component = ShellyComponent.fromKey("em1data:2");
        assertEquals(ShellyComponent.EM1_DATA_2, component);
        assertEquals(ShellyPhase.PHASE_L3, component.phase());
    }

    @Test
    void fromKey_returnsUNKNOWN_forUnknownKey() {
        var component = ShellyComponent.fromKey("nonexistent:key");
        assertEquals(ShellyComponent.UNKNOWN, component);
        assertEquals(ShellyPhase.UNKNOWN, component.phase());
    }
}
