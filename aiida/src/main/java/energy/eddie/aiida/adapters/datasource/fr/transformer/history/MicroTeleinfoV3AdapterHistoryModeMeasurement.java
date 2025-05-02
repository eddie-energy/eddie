package energy.eddie.aiida.adapters.datasource.fr.transformer.history;

import energy.eddie.aiida.adapters.datasource.SmartMeterAdapterMeasurement;
import energy.eddie.aiida.models.record.UnitOfMeasurement;
import energy.eddie.aiida.utils.ObisCode;

public class MicroTeleinfoV3AdapterHistoryModeMeasurement extends SmartMeterAdapterMeasurement {
    private final HistoryModeEntry historyModeEntry;

    public MicroTeleinfoV3AdapterHistoryModeMeasurement(String entryKey, String rawValue) {
        super(entryKey, rawValue);
        this.historyModeEntry = HistoryModeEntry.fromEntryKey(entryKey);
    }

    @Override
    public ObisCode obisCode() {
        return historyModeEntry.obisCode();
    }

    @Override
    public UnitOfMeasurement rawUnitOfMeasurement() {
        return historyModeEntry.rawUnitOfMeasurement();
    }
}
