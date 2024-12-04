package energy.eddie.api.agnostic.retransmission.result;

import java.time.ZonedDateTime;

public record PermissionRequestNotFound(String permissionId, ZonedDateTime timestamp) implements RetransmissionResult {
}
