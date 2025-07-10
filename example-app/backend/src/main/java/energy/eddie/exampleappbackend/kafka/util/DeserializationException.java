package energy.eddie.exampleappbackend.kafka.util;

public class DeserializationException extends Exception {
    public DeserializationException(Exception e) {
        super(e);
    }
}
