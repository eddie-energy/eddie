package energy.eddie.aiida.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.models.permission.PermissionStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a generic status message for a permission
 *
 * @param connectionId unique id of the connection
 * @param dataNeedId   unique id of the dataNeed for the permission
 * @param timestamp    timestamp of the message
 * @param status       status of the message
 * @param permissionId unique id of the permission
 * @param eddieId      unique id of the eddie application
 */
public record ConnectionStatusMessage(
        @JsonProperty String connectionId,
        @JsonProperty UUID dataNeedId,
        @JsonProperty Instant timestamp,
        @JsonProperty PermissionStatus status,
        @JsonProperty UUID permissionId,
        @JsonProperty UUID eddieId) {
}
