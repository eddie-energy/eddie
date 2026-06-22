// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors.permission;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class InboundDataSourceInUseException extends Exception {
    public InboundDataSourceInUseException(UUID inboundPermissionId, List<UUID> outboundPermissionIds) {
        super("Cannot revoke inbound permission %s because it is still used by outbound permissions: %s".formatted(
                inboundPermissionId,
                formatPermissionIds(outboundPermissionIds)));
    }

    private static String formatPermissionIds(List<UUID> permissionIds) {
        return permissionIds.stream().map(UUID::toString).collect(Collectors.joining(", "));
    }
}
