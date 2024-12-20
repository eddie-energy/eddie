package energy.eddie.api.agnostic.retransmission.result;

import java.time.ZonedDateTime;

public record Success(String permissionId, ZonedDateTime timestamp) implements RetransmissionResult {
}
