package energy.eddie.aiida.models.record;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum UnitOfMeasurement {
    WATT("W"),
    KILO_WATT("kW"),
    WATT_HOUR("Wh"),
    KILO_WATT_HOUR("kWh"),
    VOLT_AMPERE_REACTIVE("VAr"),
    KILO_VOLT_AMPERE_REACTIVE("kVAr"),
    VOLT_AMPERE_REACTIVE_HOUR("VArh"),
    KILO_VOLT_AMPERE_REACTIVE_HOUR("kVArh"),
    AMPERE("A"),
    VOLT("V"),
    VOLT_AMPERE("VA"),
    KILO_VOLT_AMPERE("kVA"),
    NONE("none"),
    UNKNOWN("unknown");

    private final String unit;

    UnitOfMeasurement(String unit) {
        this.unit = unit;
    }

    @JsonCreator
    public static UnitOfMeasurement forValue(String value) {
        return Arrays.stream(UnitOfMeasurement.values())
                     .filter(unit -> unit.toString().equals(value))
                     .findFirst()
                     .orElse(UNKNOWN);
    }

    @Override
    @JsonValue
    public String toString() {
        return unit;
    }
}
