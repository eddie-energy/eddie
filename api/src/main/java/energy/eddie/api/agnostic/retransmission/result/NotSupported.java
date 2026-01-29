// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic.retransmission.result;

import java.time.ZonedDateTime;

public record NotSupported(
        String permissionId,
        ZonedDateTime timestamp,
        String reason
) implements RetransmissionResult {
}
