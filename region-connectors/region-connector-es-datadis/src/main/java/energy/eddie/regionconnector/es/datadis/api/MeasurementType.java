package energy.eddie.regionconnector.es.datadis.api;

public enum MeasurementType {
    HOURLY(0),
    QUARTER_HOURLY(1);

    private final int value;

    MeasurementType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
