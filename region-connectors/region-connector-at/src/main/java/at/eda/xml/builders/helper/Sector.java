package at.eda.xml.builders.helper;

public enum Sector {
    ELECTRICITY("01"),
    GAS("02");

    private final String value;

    Sector(String value) {
        this.value = value;
    }

    public static Sector fromValue(String v) {
        return valueOf(v);
    }

    public String value() {
        return value;
    }
}
