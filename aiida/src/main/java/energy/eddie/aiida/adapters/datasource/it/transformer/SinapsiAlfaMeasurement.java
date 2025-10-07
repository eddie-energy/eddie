package energy.eddie.aiida.adapters.datasource.it.transformer;

import energy.eddie.aiida.adapters.datasource.SmartMeterAdapterMeasurement;
import energy.eddie.aiida.models.record.UnitOfMeasurement;
import energy.eddie.aiida.utils.ObisCode;

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
