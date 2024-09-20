package energy.eddie.regionconnector.us.green.button.api;

public enum Pages {
    SLURP(true), NO_SLURP(false);
    private final boolean value;

    Pages(boolean value) {
        this.value = value;
    }

    public boolean value() {
        return value;
    }
}
