package energy.eddie.aiida.adapters.datasource.shelly.transformer;

import energy.eddie.aiida.adapters.datasource.SmartMeterAdapterMeasurement;
import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.api.agnostic.aiida.UnitOfMeasurement;

public class ShellyMeasurement extends SmartMeterAdapterMeasurement {
    private static final String COMPONENT_KEY_SEPARATOR = ":";
    private final ShellyPhase phase;
    private final ShellyEntry entry;

    public ShellyMeasurement(ShellyComponent component, String entryKey, String rawValue) {
        super(component.key() + COMPONENT_KEY_SEPARATOR + entryKey, rawValue);

        if(component.phase() == ShellyPhase.TOTAL) {
            this.phase = ShellyPhase.fromKey(entryKey);
        } else {
            this.phase = component.phase();
        }
        this.entry = ShellyEntry.fromKey(entryKey);
    }

    @Override
    public ObisCode obisCode() {
        return entry.obisCodeForPhase(phase);
    }

    @Override
    public UnitOfMeasurement rawUnitOfMeasurement() {
        return entry.rawUnitOfMeasurement();
    }
}
