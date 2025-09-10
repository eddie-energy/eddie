package energy.eddie.aiida.adapters.datasource.shelly.transformer;

import energy.eddie.aiida.models.record.UnitOfMeasurement;
import energy.eddie.aiida.utils.ObisCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShellyEMEntryTest {
    @Test
    void fromKey_returnsTotalActiveEnergy_withPrefix() {
        var entry = ShellyEMEntry.fromKey("a_total_act_energy");
        assertEquals(ShellyEMEntry.TOTAL_ACTIVE_ENERGY, entry);
    }

    @Test
    void fromKey_returnsTotalActiveEnergy_withoutPrefix() {
        var entry = ShellyEMEntry.fromKey("total_act_energy");
        assertEquals(ShellyEMEntry.TOTAL_ACTIVE_ENERGY, entry);
    }

    @Test
    void fromKey_returnsUnknown() {
        var entry = ShellyEMEntry.fromKey("unknown_key");
        assertEquals(ShellyEMEntry.UNKNOWN, entry);
    }

    @Test
    void obisCodeForPhase_returnsCorrectCode() {
        var entry = ShellyEMEntry.CURRENT;

        assertEquals(ObisCode.INSTANTANEOUS_CURRENT, entry.obisCodeForPhase(ShellyEMPhase.TOTAL));
        assertEquals(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_NEUTRAL, entry.obisCodeForPhase(ShellyEMPhase.NEUTRAL));
        assertEquals(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L1, entry.obisCodeForPhase(ShellyEMPhase.PHASE_L1));
        assertEquals(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L2, entry.obisCodeForPhase(ShellyEMPhase.PHASE_L2));
        assertEquals(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L3, entry.obisCodeForPhase(ShellyEMPhase.PHASE_L3));
        assertEquals(ObisCode.UNKNOWN, entry.obisCodeForPhase(ShellyEMPhase.UNKNOWN));
    }

    @Test
    void rawUnitOfMeasurement_returnsNotDefaultUnit() {
        var entry = ShellyEMEntry.ACTIVE_POWER;
        assertEquals(UnitOfMeasurement.WATT, entry.rawUnitOfMeasurement());
    }
}
