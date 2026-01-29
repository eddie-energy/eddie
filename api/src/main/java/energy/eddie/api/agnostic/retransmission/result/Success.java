// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic.retransmission.result;

import java.time.ZonedDateTime;

public record Success(String permissionId, ZonedDateTime timestamp) implements RetransmissionResult {
}
