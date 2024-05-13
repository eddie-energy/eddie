package energy.eddie.core.services;

public class UnknownRegionConnectorException extends Exception {
    public UnknownRegionConnectorException(String regionConnectorId) {
        super("Unknown region connector: " + regionConnectorId);
    }
}
