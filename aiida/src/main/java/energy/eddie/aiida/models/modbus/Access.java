package energy.eddie.aiida.models.modbus;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

public enum Access {
    READ,
    WRITE,
    READWRITE,
    UNKNOWN;

    @JsonCreator
    public static Access fromString(String value) {
        try {
            return Access.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return UNKNOWN;
        }
    }

    public boolean canRead() {
        return this == READ || this == READWRITE;
    }

    public boolean canWrite() {
        return this == WRITE || this == READWRITE;
    }

}
