package energy.eddie.api.v0;

import java.time.ZonedDateTime;

/**
 * Represents a generic status message for a permission
 *
 * @param connectionId id of the connection (a connectionId can be associated with multiple permissions)
 * @param permissionId unique id of the permission
 * @param timestamp    timestamp of the message
 * @param status       status of the message
 * @param message      contains additional information about the status
 */
public record ConnectionStatusMessage(String connectionId, String permissionId,
                                      ZonedDateTime timestamp, ConnectionStatusMessage.Status status,
                                      String message) {


    public enum Status {
        REQUESTED,
        GRANTED,
        REJECTED,
        ERROR,
        REVOKED
    }
}
