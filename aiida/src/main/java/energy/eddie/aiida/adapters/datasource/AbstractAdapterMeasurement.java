package energy.eddie.aiida.adapters.datasource;

import energy.eddie.aiida.models.record.UnitOfMeasurement;
import energy.eddie.aiida.utils.ObisCode;

public abstract class AbstractAdapterMeasurement implements AdapterMeasurement {
    protected final String entryKey;
    protected final String rawValue;
    protected ObisCode obisCode;

    protected AbstractAdapterMeasurement(String key, String rawValue) {
        this.obisCode = obisCodeForEntryKey(key);
        this.entryKey = key;
        this.rawValue = rawValue;
    }

    @Override
    public String entryKey() {
        return entryKey;
    }

    @Override
    public ObisCode obisCode() {
        return obisCode;
    }

    @Override
    public UnitOfMeasurement unitOfMeasurement() {
        return obisCode.unitOfMeasurement();
    }

    @Override
    public String rawValue() {
        return rawValue;
    }

    protected abstract ObisCode obisCodeForEntryKey(String entryKey);
}
