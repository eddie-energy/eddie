package energy.eddie.cim.serde;

public class SerdeInitializationException extends Exception {

    public SerdeInitializationException(Throwable throwable) {
        super(throwable);
    }

    public SerdeInitializationException(String message) {
        super(message);
    }
}
