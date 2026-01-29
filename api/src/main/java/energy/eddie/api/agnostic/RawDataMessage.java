// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic;

import energy.eddie.api.agnostic.process.model.PermissionRequest;

import java.time.ZoneOffset;
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
        String rawPayload
) implements MessageWithHeaders {
    /**
     * Utility constructor to create a RawDataMessage from a permission request and payload. Will always use the current
     * ZonedDateTime for {@code timestamp}.
     *
     * @param permissionRequest used to populate all fields except timestamp and rawPayload
     * @param rawPayload        the payload
     */
    public RawDataMessage(PermissionRequest permissionRequest, String rawPayload) {
        this(
                permissionRequest.permissionId(),
                permissionRequest.connectionId(),
                permissionRequest.dataNeedId(),
                permissionRequest.dataSourceInformation(),
                ZonedDateTime.now(ZoneOffset.UTC),
                rawPayload
        );
    }
}
