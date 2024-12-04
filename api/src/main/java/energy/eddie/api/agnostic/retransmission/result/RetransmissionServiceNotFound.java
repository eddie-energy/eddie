package energy.eddie.api.agnostic.retransmission.result;

import java.time.ZonedDateTime;

public final class RetransmissionServiceNotFound extends RuntimeException implements RetransmissionResult {
    private final String permissionId;
    private final String regionConnectorId;
    private final ZonedDateTime timestamp;

    public RetransmissionServiceNotFound(String permissionId, String regionConnectorId, ZonedDateTime timestamp) {
        super("Cant request retransmission for permissionId: '" + permissionId + "': No retransmission service found for regionConnectorId: '" + regionConnectorId + "'");
        this.permissionId = permissionId;
        this.regionConnectorId = regionConnectorId;
        this.timestamp = timestamp;
    }

    @Override
    public String permissionId() {
        return permissionId;
    }


    @Override
    public ZonedDateTime timestamp() {
        return timestamp;
    }

    public String regionConnectorId() {
        return regionConnectorId;
    }
}
