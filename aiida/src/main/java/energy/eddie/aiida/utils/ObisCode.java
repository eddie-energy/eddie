package energy.eddie.aiida.utils;

import energy.eddie.aiida.models.record.UnitOfMeasurement;

public enum ObisCode {
    POSITIVE_ACTIVE_ENERGY("1-0:1.8.0", UnitOfMeasurement.kWh),
    NEGATIVE_ACTIVE_ENERGY("1-0:2.8.0", UnitOfMeasurement.kWh),
    POSITIVE_ACTIVE_INSTANTANEOUS_POWER("1-0:1.7.0", UnitOfMeasurement.kW),
    NEGATIVE_ACTIVE_INSTANTANEOUS_POWER("1-0:2.7.0", UnitOfMeasurement.kW),
    POSITIVE_REACTIVE_INSTANTANEOUS_POWER("1-0:3.7.0", UnitOfMeasurement.kW),
    NEGATIVE_REACTIVE_INSTANTANEOUS_POWER("1-0:4.7.0", UnitOfMeasurement.kW),
    POSITIVE_REACTIVE_ENERGY_IN_TARIFF("1-0:3.8.1", UnitOfMeasurement.kvarh),
    NEGATIVE_REACTIVE_ENERGY_IN_TARIFF("1-0:4.8.1", UnitOfMeasurement.kvarh),
    DEVICE_ID_1("0-0:96.1.0", UnitOfMeasurement.text),
    TIME("0-0:1.0.0", UnitOfMeasurement.text),
    UPTIME("0-0:2.0.0", UnitOfMeasurement.text),
    UNKNOWN("0-0:0.0.0", UnitOfMeasurement.unkown),
    METER_SERIAL("0-0:C.1.0", UnitOfMeasurement.text);


    private final String code;
    private final UnitOfMeasurement unitOfMeasurement;

    ObisCode(String code, UnitOfMeasurement unitOfMeasurement) {
        this.code = code;
        this.unitOfMeasurement = unitOfMeasurement;
    }

    public String code() {
        return code;
    }

    public String unitOfMeasurement() {
        return unitOfMeasurement.unit();
    }

    public static ObisCode from(String code) {
        for (ObisCode obisCode : ObisCode.values()) {
            if (obisCode.code.equals(code)) {
                return obisCode;
            }
        }
        return UNKNOWN;
    }
}
