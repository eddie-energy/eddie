package energy.eddie.aiida.adapters.datasource;

import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.aiida.models.record.UnitOfMeasurement;
import energy.eddie.aiida.utils.ObisCode;

public abstract class SmartMeterAdapterMeasurement implements AdapterMeasurement {
    protected final String entryKey;
    protected final String rawValue;

    protected SmartMeterAdapterMeasurement(String entryKey, String rawValue) {
        this.entryKey = entryKey;
        this.rawValue = rawValue;
    }

    @Override
    public String entryKey() {
        return entryKey;
    }

    @Override
    public String rawValue() {
        return rawValue;
    }

    public abstract ObisCode obisCode();

    public final AiidaRecordValue toAiidaRecordValue() {
        return new AiidaRecordValue(entryKey(),
                                    obisCode(),
                                    rawValue(),
                                    rawUnitOfMeasurement(),
                                    value(),
                                    unitOfMeasurement());
    }

    public final String value() {
        if (unitOfMeasurement().equals(UnitOfMeasurement.UNKNOWN)) {
            return rawValue;
        }

        return switch (rawUnitOfMeasurement()) {
            case WATT, WATT_HOUR, VOLT_AMPERE_REACTIVE, VOLT_AMPERE_REACTIVE_HOUR, VOLT_AMPERE ->
                    String.valueOf(Double.parseDouble(rawValue) / 1000);
            case KILO_WATT, KILO_WATT_HOUR, KILO_VOLT_AMPERE_REACTIVE, KILO_VOLT_AMPERE_REACTIVE_HOUR, AMPERE, VOLT,
                 KILO_VOLT_AMPERE, HERTZ, NONE, UNKNOWN -> rawValue;
        };
    }

    public final UnitOfMeasurement unitOfMeasurement() {
        return obisCode().unitOfMeasurement();
    }
}
