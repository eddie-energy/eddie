package energy.eddie.aiida.adapters.datasource.it.transformer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SinapsiAlfaEntryTest {
    @Test
    void fromKey_returnsPositiveActiveEnergy_withPrefix() {
        var entry = SinapsiAlfaEntry.fromKey("1-0:1.8.0.255_3,0_2");
        assertEquals(SinapsiAlfaEntry.POSITIVE_ACTIVE_ENERGY, entry);
    }

    @Test
    void fromKey_returnsUnknown() {
        var entry = SinapsiAlfaEntry.fromKey("unknown_key");
        assertEquals(SinapsiAlfaEntry.UNKNOWN, entry);
    }
}
