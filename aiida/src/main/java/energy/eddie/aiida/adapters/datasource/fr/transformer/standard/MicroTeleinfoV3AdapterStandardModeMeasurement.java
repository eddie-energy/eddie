package energy.eddie.aiida.adapters.datasource.fr.transformer.standard;

import energy.eddie.aiida.adapters.datasource.AbstractAdapterMeasurement;
import energy.eddie.aiida.models.record.UnitOfMeasurement;
import energy.eddie.aiida.utils.ObisCode;

public class MicroTeleinfoV3AdapterStandardModeMeasurement extends AbstractAdapterMeasurement {
    public MicroTeleinfoV3AdapterStandardModeMeasurement(String key, String rawValue) {
        super(key, rawValue);
    }

    @Override
    public UnitOfMeasurement rawUnitOfMeasurement() {
        return switch (obisCode.unitOfMeasurement()) {
            case WATT, KILO_WATT -> UnitOfMeasurement.WATT;
            case WATT_HOUR, KILO_WATT_HOUR -> UnitOfMeasurement.WATT_HOUR;
            case VOLT_AMPERE_REACTIVE -> UnitOfMeasurement.VOLT_AMPERE_REACTIVE;
            case KILO_VOLT_AMPERE_REACTIVE -> UnitOfMeasurement.KILO_VOLT_AMPERE_REACTIVE;
            case VOLT_AMPERE_REACTIVE_HOUR, KILO_VOLT_AMPERE_REACTIVE_HOUR ->
                    UnitOfMeasurement.VOLT_AMPERE_REACTIVE_HOUR;
            case AMPERE -> UnitOfMeasurement.AMPERE;
            case VOLT -> UnitOfMeasurement.VOLT;
            case VOLT_AMPERE -> UnitOfMeasurement.VOLT_AMPERE;
            case KILO_VOLT_AMPERE -> UnitOfMeasurement.KILO_VOLT_AMPERE;
            case NONE -> UnitOfMeasurement.NONE;
            case UNKNOWN -> UnitOfMeasurement.UNKNOWN;
        };
    }

    @Override
    public String value() {
        return switch (obisCode.unitOfMeasurement()) {
            case AMPERE, VOLT, VOLT_AMPERE, KILO_VOLT_AMPERE, VOLT_AMPERE_REACTIVE, KILO_VOLT_AMPERE_REACTIVE,
                 VOLT_AMPERE_REACTIVE_HOUR, WATT, WATT_HOUR, NONE, UNKNOWN -> rawValue;
            case KILO_VOLT_AMPERE_REACTIVE_HOUR, KILO_WATT, KILO_WATT_HOUR ->
                    String.valueOf(Double.parseDouble(rawValue) / 1000);
        };
    }

    @Override
    protected ObisCode obisCodeForEntryKey(String entryKey) {
        return StandardModeEntry.fromEntryKey(entryKey).obisCode();
    }
}
