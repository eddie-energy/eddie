package energy.eddie.api.agnostic.retransmission;

public class RetransmissionServiceNotFound extends RuntimeException {
    public RetransmissionServiceNotFound(String message) {
        super(message);
    }
}
