// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.requests;

import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata;

import java.time.LocalDate;
import java.time.ZonedDateTime;

public record CCMORevoke(AtPermissionRequest permissionRequest, String eligiblePartyId, String reason,
                         ZonedDateTime consentEnd) {
    public CCMORevoke(AtPermissionRequest permissionRequest, String eligiblePartyId, String reason) {
        this(
                permissionRequest,
                eligiblePartyId,
                reason,
                LocalDate.now(EdaRegionConnectorMetadata.AT_ZONE_ID).atStartOfDay(EdaRegionConnectorMetadata.AT_ZONE_ID)
        );
    }
}
