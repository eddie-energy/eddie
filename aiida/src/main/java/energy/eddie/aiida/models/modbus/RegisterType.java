package energy.eddie.aiida.models.modbus;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum RegisterType {
    HOLDING,
    INPUT,
    COIL,
    DISCRETE,
    UNKNOWN;

    @JsonCreator
    public static RegisterType fromString(String value) {
        try {
            return RegisterType.valueOf(value.toUpperCase());
        } catch (Exception e) {
            return UNKNOWN;
        }
    }
}
