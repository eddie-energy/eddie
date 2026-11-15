// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.dtos.connectionstatus;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.models.permission.ConnectionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Server-sent event, that the connection status of a permission has changed.
 */
public record ConnectionStatusEventDto(
        @JsonProperty
        @Schema(description = "ID of the permission whose connection status changed.")
        UUID permissionId,
        @JsonProperty
        @Schema(description = "New connection status of the permission.")
        ConnectionStatus connectionStatus
) {}
