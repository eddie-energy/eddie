package energy.eddie.aiida.models.record;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum UnitOfMeasurement {
    W("W"),
    KW("kW"),
    WH("Wh"),
    KWH("kWh"),
    VAR("var"),
    KVAR("kvar"),
    VARH("varh"),
    KVARH("kvarh"),
    AMPERE("A"),
    VOLT("V"),
    VOLTAMPERE("VA"),
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
