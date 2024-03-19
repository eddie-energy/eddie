package energy.eddie.dataneeds.exceptions;

public class UnsupportedDataNeedException extends Exception {
    public UnsupportedDataNeedException(String regionConnectorName, String dataNeedId, String errorReason) {
        super("Region connector '%s' does not support data need with ID '%s': %s"
                      .formatted(regionConnectorName,
                                 dataNeedId,
                                 errorReason));
    }
}
