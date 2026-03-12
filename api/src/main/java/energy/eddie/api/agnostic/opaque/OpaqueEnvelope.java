// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic.opaque;

import energy.eddie.api.agnostic.MessageWithHeaders;

import java.time.ZonedDateTime;

public record OpaqueEnvelope(
        String regionConnectorId,
        String permissionId,
        String connectionId,
        String dataNeedId,
        String messageId,
        ZonedDateTime timestamp,
        String payload
) implements MessageWithHeaders {
}
