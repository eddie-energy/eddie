package energy.eddie.api.agnostic.retransmission.result;

public record Failure(
        String reason
) implements RetransmissionResult {
}
