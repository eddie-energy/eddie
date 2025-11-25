package energy.eddie.aiida.adapters.datasource.it.transformer;

import energy.eddie.aiida.adapters.datasource.SmartMeterAdapterMeasurement;
import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.api.agnostic.aiida.UnitOfMeasurement;

public class SinapsiAlfaMeasurement extends SmartMeterAdapterMeasurement {
    private final SinapsiAlfaEntry entry;

    public SinapsiAlfaMeasurement(String entryKey, String rawValue) {
        super(entryKey, rawValue);
        this.entry = SinapsiAlfaEntry.fromKey(entryKey);
    }

    @Override
    public ObisCode obisCode() {
        return entry.obisCode();
    }

    @Override
    public UnitOfMeasurement rawUnitOfMeasurement() {
        return entry.rawUnitOfMeasurement();
    }
}
