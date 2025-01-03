package energy.eddie.regionconnector.us.green.button.providers.v0_82;

class UnsupportedUnitException extends Exception {
    public UnsupportedUnitException(String unitOfMeasurement) {
        super("Unsupported unit: " + unitOfMeasurement);
    }
}
