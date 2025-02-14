package energy.eddie.aiida.models.record;

public enum UnitOfMeasurement {
    KW("kW"),
    KWH("kWh"),
    WH("Wh"),
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

    public String unit() {
        return unit;
    }
}
