package energy.eddie.outbound.shared.serde;

public class DeserializationException extends Exception {
    public DeserializationException(Exception e) {
        super(e);
    }
}
