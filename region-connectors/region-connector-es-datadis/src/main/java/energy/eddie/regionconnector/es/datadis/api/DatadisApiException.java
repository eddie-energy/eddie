package energy.eddie.regionconnector.es.datadis.api;

public class DatadisApiException extends Exception {
    public DatadisApiException(Throwable cause) {
        super(cause);
    }

    public DatadisApiException(String message) {
        super(message);
    }
}
