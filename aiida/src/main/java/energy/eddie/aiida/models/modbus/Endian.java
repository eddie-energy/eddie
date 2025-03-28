package energy.eddie.aiida.models.modbus;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Endian {
    BIG,
    LITTLE,
    UNKNOWN;

    @JsonCreator
    public static Endian fromString(String value) {
        try {
            return Endian.valueOf(value.toUpperCase());
        } catch (Exception e) {
            return UNKNOWN;
        }
    }
}
