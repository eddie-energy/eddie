package energy.eddie.dataneeds.exceptions;

public class DataNeedDisabledException extends Exception {
    public DataNeedDisabledException(String dataNeedId) {
        super("Data need with ID '%s' is disabled.".formatted(dataNeedId));
    }
}
