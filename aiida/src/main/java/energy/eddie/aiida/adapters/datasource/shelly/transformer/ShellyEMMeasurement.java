package energy.eddie.aiida.adapters.datasource.shelly.transformer;

import energy.eddie.aiida.adapters.datasource.SmartMeterAdapterMeasurement;
import energy.eddie.aiida.models.record.UnitOfMeasurement;
import energy.eddie.aiida.utils.ObisCode;

public class ShellyEMMeasurement extends SmartMeterAdapterMeasurement {
    private final ShellyEMPhase phase;
    private final ShellyEMEntry entry;

    public ShellyEMMeasurement(ShellyEMComponent component, String entryKey, String rawValue) {
        super(entryKey, rawValue);

        if(component.phase() == ShellyEMPhase.TOTAL) {
            this.phase = ShellyEMPhase.fromKey(entryKey);
        } else {
            this.phase = component.phase();
        }
        this.entry = ShellyEMEntry.fromKey(entryKey);
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
