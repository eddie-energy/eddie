package energy.eddie.exampleappbackend.serialization;

public class SerdeInitializationException extends Exception {

    public SerdeInitializationException(Throwable throwable) {
        super(throwable);
    }

    public SerdeInitializationException(String message) {
        super(message);
    }
}
