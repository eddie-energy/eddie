// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic.retransmission;

import java.time.LocalDate;

public record RetransmissionRequest(
        String regionConnectorId,
        String permissionId,
        LocalDate from,
        LocalDate to
) {
}
