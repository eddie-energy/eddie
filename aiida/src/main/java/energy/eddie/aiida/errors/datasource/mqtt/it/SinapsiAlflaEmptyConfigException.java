package energy.eddie.aiida.errors.datasource.mqtt.it;

public class SinapsiAlflaEmptyConfigException extends Exception {
    public SinapsiAlflaEmptyConfigException() {
        super("Sinapsi Alfa credentials are not configured - cannot proceed without them.”");
    }
}
