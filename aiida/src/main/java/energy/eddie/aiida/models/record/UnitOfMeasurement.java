package energy.eddie.aiida.models.record;

public enum UnitOfMeasurement {
    KW("kW"),
    KWH("kWh"),
    WH("Wh"),
    KVARH("kvarh"),
    AMPERE("A"),
    VOLTAMPERE("VA"),
    TEXT("text"),
    UNKNOWN("unknown");

    private final String unit;

    UnitOfMeasurement(String unit) {
        this.unit = unit;
    }

    public String unit() {
        return unit;
    }
}
