package at.eda;
public enum EdaSchemaVersion {
    CM_REQUEST_01_10("01.10");

    private final String value;

    EdaSchemaVersion(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
