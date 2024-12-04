package energy.eddie.api.agnostic.retransmission.result;

import java.time.ZonedDateTime;

public record DataNotAvailable(String permissionId, ZonedDateTime timestamp) implements RetransmissionResult {
}
