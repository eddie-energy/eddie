package energy.eddie.aiida.adapters.datasource.shelly.transformer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShellyEMPhaseTest {
    @Test
    void fromKey_returnsPhaseL1() {
        var phase = ShellyEMPhase.fromKey("a_total_act_energy");
        assertEquals(ShellyEMPhase.PHASE_L1, phase);
    }

    @Test
    void fromKey_returnsUnknown() {
        var phase = ShellyEMPhase.fromKey("unknown_key");
        assertEquals(ShellyEMPhase.UNKNOWN, phase);
    }
}
