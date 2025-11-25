package energy.eddie.aiida.adapters.datasource.fr.transformer.standard;

import energy.eddie.aiida.adapters.datasource.SmartMeterAdapterMeasurement;
import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.api.agnostic.aiida.UnitOfMeasurement;

public class MicroTeleinfoV3AdapterStandardModeMeasurement extends SmartMeterAdapterMeasurement {
    private final StandardModeEntry standardModeEntry;

    public MicroTeleinfoV3AdapterStandardModeMeasurement(String entryKey, String rawValue) {
        super(entryKey, rawValue);
        this.standardModeEntry = StandardModeEntry.fromEntryKey(entryKey);
    }

    @Override
    public ObisCode obisCode() {
        return standardModeEntry.obisCode();
    }

    @Override
    public UnitOfMeasurement rawUnitOfMeasurement() {
        return standardModeEntry.rawUnitOfMeasurement();
    }
}
