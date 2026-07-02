// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors.permission;

import energy.eddie.dataneeds.needs.aiida.InboundAiidaDataNeed;
import energy.eddie.dataneeds.needs.aiida.OutboundAiidaDataNeed;
import jakarta.annotation.Nullable;

import java.util.UUID;

public class ActiveFcaPermissionAlreadyExistsException extends Exception {
    public ActiveFcaPermissionAlreadyExistsException(
            @Nullable UUID permissionId,
            String meterId,
            String dataNeedType
    ) {
        super("Permission %s cannot be fulfilled: there is already an active %s FCA permission for meter ID '%s'."
                      .formatted(permissionId, toDirection(dataNeedType), meterId));
    }

    private static String toDirection(String dataNeedType) {
        return switch (dataNeedType) {
            case InboundAiidaDataNeed.DISCRIMINATOR_VALUE -> "inbound";
            case OutboundAiidaDataNeed.DISCRIMINATOR_VALUE -> "outbound";
            default -> dataNeedType;
        };
    }
}
