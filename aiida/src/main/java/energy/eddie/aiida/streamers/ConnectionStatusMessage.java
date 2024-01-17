package energy.eddie.aiida.streamers;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.models.permission.PermissionStatus;

import java.time.Instant;

/**
 * Represents a generic status message for a permission
 *
 * @param connectionId unique id of the connection
 * @param dataNeedId   unique id of the dataNeed for the permission
 * @param timestamp    timestamp of the message
 * @param status       status of the message
 * @param permissionId unique id of the permission
 */
public record ConnectionStatusMessage(
        @JsonProperty String connectionId,
        @JsonProperty String dataNeedId,
        @JsonProperty Instant timestamp,
        @JsonProperty PermissionStatus status,
        @JsonProperty String permissionId) {
}
