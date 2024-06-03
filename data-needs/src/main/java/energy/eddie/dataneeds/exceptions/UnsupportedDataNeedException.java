package energy.eddie.dataneeds.exceptions;

public class UnsupportedDataNeedException extends Exception {
    private final String errorReason;

    public UnsupportedDataNeedException(String regionConnectorName, String dataNeedId, String errorReason) {
        super("Region connector '%s' does not support data need with ID '%s': %s"
                      .formatted(regionConnectorName,
                                 dataNeedId,
                                 errorReason));
        this.errorReason = errorReason;
    }

    public UnsupportedDataNeedException(String dataNeedId, String errorReason) {
        super("Data need with ID '%s' is not supported: %s"
                      .formatted(dataNeedId, errorReason));
        this.errorReason = errorReason;
    }

    public String errorReason() {
        return errorReason;
    }
}
