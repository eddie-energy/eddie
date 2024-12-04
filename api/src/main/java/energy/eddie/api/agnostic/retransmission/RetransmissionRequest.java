package energy.eddie.api.agnostic.retransmission;

import java.time.LocalDate;

public record RetransmissionRequest(
        String regionConnectorId,
        String permissionId,
        LocalDate from,
        LocalDate to
) {
}
