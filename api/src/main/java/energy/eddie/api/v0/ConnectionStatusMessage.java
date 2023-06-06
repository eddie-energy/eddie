package energy.eddie.api.v0;

import java.time.ZonedDateTime;

/**
 * Message from the region connector describing the current consent status.
 *
 * @param connectionId connection id as given from the EP application
 * @param permissionId associated with this connection
 * @param timestamp    current timestamp
 * @param status       current status of the consent process
 */
public record ConnectionStatusMessage(
        String connectionId,
        String permissionId,
        ZonedDateTime timestamp,
        ConsentStatus status
) {
}
