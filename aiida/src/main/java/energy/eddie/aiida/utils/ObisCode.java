package energy.eddie.aiida.utils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import energy.eddie.aiida.models.record.UnitOfMeasurement;

import java.util.Arrays;

public enum ObisCode {
    POSITIVE_ACTIVE_ENERGY("1-0:1.8.0", UnitOfMeasurement.KILO_WATT_HOUR),
    NEGATIVE_ACTIVE_ENERGY("1-0:2.8.0", UnitOfMeasurement.KILO_WATT_HOUR),
    POSITIVE_ACTIVE_INSTANTANEOUS_POWER("1-0:1.7.0", UnitOfMeasurement.KILO_WATT),
    NEGATIVE_ACTIVE_INSTANTANEOUS_POWER("1-0:2.7.0", UnitOfMeasurement.KILO_WATT),
    POSITIVE_REACTIVE_INSTANTANEOUS_POWER("1-0:3.7.0", UnitOfMeasurement.KILO_VOLT_AMPERE_REACTIVE),
    NEGATIVE_REACTIVE_INSTANTANEOUS_POWER("1-0:4.7.0", UnitOfMeasurement.KILO_VOLT_AMPERE_REACTIVE),
    POSITIVE_REACTIVE_ENERGY_IN_TARIFF("1-0:3.8.1", UnitOfMeasurement.KILO_VOLT_AMPERE_REACTIVE_HOUR),
    NEGATIVE_REACTIVE_ENERGY_IN_TARIFF("1-0:4.8.1", UnitOfMeasurement.KILO_VOLT_AMPERE_REACTIVE_HOUR),
    INSTANTANEOUS_POWER_FACTOR("1-0:13.7.0", UnitOfMeasurement.NONE),
    INSTANTANEOUS_CURRENT_IN_PHASE_L1("1-0:31.7.0", UnitOfMeasurement.AMPERE),
    INSTANTANEOUS_VOLTAGE_IN_PHASE_L1("1-0:32.7.0", UnitOfMeasurement.VOLT),
    INSTANTANEOUS_CURRENT_IN_PHASE_L2("1-0:51.7.0", UnitOfMeasurement.AMPERE),
    INSTANTANEOUS_VOLTAGE_IN_PHASE_L2("1-0:52.7.0", UnitOfMeasurement.VOLT),
    INSTANTANEOUS_CURRENT_IN_PHASE_L3("1-0:71.7.0", UnitOfMeasurement.AMPERE),
    INSTANTANEOUS_VOLTAGE_IN_PHASE_L3("1-0:72.7.0", UnitOfMeasurement.VOLT),
    DEVICE_ID_1("0-0:96.1.0", UnitOfMeasurement.NONE),
    TIME("0-0:1.0.0", UnitOfMeasurement.NONE),
    UPTIME("0-0:2.0.0", UnitOfMeasurement.NONE),
    UNKNOWN("0-0:0.0.0", UnitOfMeasurement.UNKNOWN),
    METER_SERIAL("0-0:C.1.0", UnitOfMeasurement.NONE);


    private final String code;
    private final UnitOfMeasurement unitOfMeasurement;

    ObisCode(String code, UnitOfMeasurement unitOfMeasurement) {
        this.code = code;
        this.unitOfMeasurement = unitOfMeasurement;
    }

    @JsonCreator
    public static ObisCode forCode(String code) {
        return Arrays.stream(ObisCode.values())
                     .filter(obisCode -> obisCode.toString().equals(code))
                     .findFirst()
                     .orElse(UNKNOWN);
    }

    public UnitOfMeasurement unitOfMeasurement() {
        return unitOfMeasurement;
    }

    @Override
    @JsonValue
    public String toString() {
        return code;
    }
}
