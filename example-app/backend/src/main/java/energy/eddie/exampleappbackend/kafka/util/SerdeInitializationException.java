package energy.eddie.exampleappbackend.kafka.util;

public class SerdeInitializationException extends Exception {

    public SerdeInitializationException(Throwable throwable) {
        super(throwable);
    }

    public SerdeInitializationException(String message) {
        super(message);
    }
}
