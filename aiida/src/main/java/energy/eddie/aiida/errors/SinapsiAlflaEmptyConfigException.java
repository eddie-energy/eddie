package energy.eddie.aiida.errors;

public class SinapsiAlflaEmptyConfigException extends Exception {
    public SinapsiAlflaEmptyConfigException() {
        super("Sinapsi Alfa credentials are not configured - cannot proceed without them.”");
    }
}
