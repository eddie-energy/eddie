package energy.eddie.regionconnector.us.green.button.xml.helper;

public enum Status {
    UNAVAILABLE("0"),
    NORMAL("1");

    private final String statusValue;

    Status(String statusValue) {
        this.statusValue = statusValue;
    }

    public static Status fromValue(String value) {
        return switch (value) {
            case "0" -> UNAVAILABLE;
            case "1" -> NORMAL;
            default -> throw new IllegalArgumentException("Unknown status value: " + value);
        };
    }

    public String status() {
        return statusValue;
    }
}
