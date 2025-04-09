package energy.eddie.regionconnector.at.eda.requests;

public enum RequestDataType {
    MASTER_DATA("MasterData"),
    METERING_DATA("MeteringData"),;

    private final String value;

    RequestDataType(String value) {this.value = value;}

    @Override
    public String toString() {
        return value;
    }
}
