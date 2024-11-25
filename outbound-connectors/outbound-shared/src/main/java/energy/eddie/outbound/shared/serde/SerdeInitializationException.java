package energy.eddie.outbound.shared.serde;

public class SerdeInitializationException extends Exception {

    public SerdeInitializationException(Throwable throwable) {
        super(throwable);
    }

    public SerdeInitializationException(String message) {
        super(message);
    }
}
