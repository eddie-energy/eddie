// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.shelly.transformer;

import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.api.agnostic.aiida.UnitOfMeasurement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShellyEntryTest {
    @Test
    void fromKey_returnsTotalActiveEnergy_withPrefix() {
        var entry = ShellyEntry.fromKey("a_total_act_energy");
        assertEquals(ShellyEntry.TOTAL_ACTIVE_ENERGY, entry);
    }

    @Test
    void fromKey_returnsTotalActiveEnergy_withoutPrefix() {
        var entry = ShellyEntry.fromKey("total_act_energy");
        assertEquals(ShellyEntry.TOTAL_ACTIVE_ENERGY, entry);
    }

    @Test
    void fromKey_returnsUnknown() {
        var entry = ShellyEntry.fromKey("unknown_key");
        assertEquals(ShellyEntry.UNKNOWN, entry);
    }

    @Test
    void obisCodeForPhase_returnsCorrectCode() {
        var entry = ShellyEntry.CURRENT;

        assertEquals(ObisCode.INSTANTANEOUS_CURRENT, entry.obisCodeForPhase(ShellyPhase.TOTAL));
        assertEquals(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_NEUTRAL, entry.obisCodeForPhase(ShellyPhase.NEUTRAL));
        assertEquals(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L1, entry.obisCodeForPhase(ShellyPhase.PHASE_L1));
        assertEquals(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L2, entry.obisCodeForPhase(ShellyPhase.PHASE_L2));
        assertEquals(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L3, entry.obisCodeForPhase(ShellyPhase.PHASE_L3));
        assertEquals(ObisCode.UNKNOWN, entry.obisCodeForPhase(ShellyPhase.UNKNOWN));
    }

    @Test
    void rawUnitOfMeasurement_returnsNotDefaultUnit() {
        var entry = ShellyEntry.ACTIVE_POWER_EM;
        assertEquals(UnitOfMeasurement.WATT, entry.rawUnitOfMeasurement());
    }
}
