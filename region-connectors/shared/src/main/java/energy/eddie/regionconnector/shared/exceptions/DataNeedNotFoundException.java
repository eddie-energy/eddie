package energy.eddie.regionconnector.shared.exceptions;

public class DataNeedNotFoundException extends Exception {
    public DataNeedNotFoundException(String dataNeedId) {
        super("No dataNeed with ID %s found".formatted(dataNeedId));
    }
}
