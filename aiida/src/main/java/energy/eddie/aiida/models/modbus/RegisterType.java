package energy.eddie.aiida.models.modbus;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

public enum RegisterType {
    HOLDING,
    INPUT,
    COIL,
    DISCRETE,
    UNKNOWN;

    @JsonCreator
    public static RegisterType fromString(String value) {
        try {
            return RegisterType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return UNKNOWN;
        }
    }
}
