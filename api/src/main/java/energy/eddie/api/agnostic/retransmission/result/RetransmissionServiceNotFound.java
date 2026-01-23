// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic.retransmission.result;

import java.time.ZonedDateTime;

public final class RetransmissionServiceNotFound implements RetransmissionResult {
    private final String permissionId;
    private final String regionConnectorId;
    private final ZonedDateTime timestamp;
    private final String reason;

    public RetransmissionServiceNotFound(String permissionId, String regionConnectorId, ZonedDateTime timestamp) {
        this.reason = "Cant request retransmission for permissionId: '" + permissionId + "': No retransmission service found for regionConnectorId: '" + regionConnectorId + "'";
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

    public String reason() {
        return reason;
    }
}
