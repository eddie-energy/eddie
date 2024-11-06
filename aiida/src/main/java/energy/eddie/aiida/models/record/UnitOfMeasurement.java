package energy.eddie.aiida.models.record;

public enum UnitOfMeasurement {
    kW("kW"),
    kWh("kWh"),
    wh("Wh"),
    kvarh("kvarh"),
    ampere("A"),
    voltAmpera("VA"),
    text("text"),
    unkown("unknown");

    private final String unit;

    UnitOfMeasurement(String unit) {
        this.unit = unit;
    }

    public String unit() {
        return unit;
    }
}
