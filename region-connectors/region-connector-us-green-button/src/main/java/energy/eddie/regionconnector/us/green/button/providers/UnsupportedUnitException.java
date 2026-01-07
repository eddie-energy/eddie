package energy.eddie.regionconnector.us.green.button.providers;

public class UnsupportedUnitException extends Exception {
    public UnsupportedUnitException(String unitOfMeasurement) {
        super("Unsupported unit: " + unitOfMeasurement);
    }
}
