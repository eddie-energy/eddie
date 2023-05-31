package energy.eddie.regionconnector.at.eda;

/**
 * This exception indicates that a connection or transmission error occurred.
 */
public class TransmissionException extends Exception {
    public TransmissionException(Throwable cause) {
        super(cause);
    }
}
