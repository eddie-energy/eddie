package energy.eddie.aiida.adapters.datasource.shelly.transformer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShellyPhaseTest {
    @Test
    void fromKey_returnsPhaseL1() {
        var phase = ShellyPhase.fromKey("a_total_act_energy");
        assertEquals(ShellyPhase.PHASE_L1, phase);
    }

    @Test
    void fromKey_returnsUnknown() {
        var phase = ShellyPhase.fromKey("unknown_key");
        assertEquals(ShellyPhase.UNKNOWN, phase);
    }
}
