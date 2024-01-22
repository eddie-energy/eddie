package energy.eddie.api.agnostic;

import energy.eddie.api.v0.DataSourceInformation;

import java.time.ZonedDateTime;

/**
 * A record that holds the raw data as directly received from the MDA and some metadata information.
 *
 * @param permissionId          ID of the permission to which this record belongs to.
 * @param connectionId          ConnectionId of the permission this record belongs to.
 * @param dataNeedId            DataNeedId of the permission this record belongs to.
 * @param dataSourceInformation Metadata about the region connector that produced this message.
 * @param timestamp             Timestamp when this message was created.
 * @param rawPayload            Raw data as received from the MDA.
 */
public record RawDataMessage(
        String permissionId,
        String connectionId,
        String dataNeedId,
        DataSourceInformation dataSourceInformation,
        ZonedDateTime timestamp,
        String rawPayload) {
}
