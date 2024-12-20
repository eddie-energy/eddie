package energy.eddie.api.agnostic.retransmission.result;

import java.time.ZonedDateTime;

public record NotSupported(
        String permissionId,
        ZonedDateTime timestamp,
        String reason
) implements RetransmissionResult {
}
