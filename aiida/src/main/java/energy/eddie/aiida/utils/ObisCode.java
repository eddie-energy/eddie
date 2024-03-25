package energy.eddie.aiida.utils;

public enum ObisCode {
    POSITIVE_ACTIVE_ENERGY("1-0:1.8.0"),
    NEGATIVE_ACTIVE_ENERGY("1-0:2.8.0"),
    POSITIVE_ACTIVE_INSTANTANEOUS_POWER("1-0:1.7.0"),
    NEGATIVE_ACTIVE_INSTANTANEOUS_POWER("1-0:2.7.0"),
    DEVICE_ID_1("0-0:96.1.0"),
    TIME("0-0:1.0.0"),
    UNKNOWN("0-0:2.0.0"),
    METER_SERIAL("0-0:C.1.0");


    private final String code;

    ObisCode(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
