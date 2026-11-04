// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.dtos.permission;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;

/**
 * Rejects a permission request from an AIIDA code. Nothing is persisted, the rejection is only sent to the EDDIE
 * framework.
 *
 * @param handshakeUrl Handshake URL template from the AIIDA code.
 * @param accessToken  Access token from the AIIDA code.
 * @param reason       Optional reason, which is forwarded to the eligible party.
 */
public record RejectPermissionDto(
        @Schema(description = "Handshake URL template, from the AIIDA code.")
        @NotBlank(message = "must not be blank")
        String handshakeUrl,

        @Schema(description = "Access token, from the AIIDA code.")
        @NotBlank(message = "must not be blank")
        String accessToken,

        @Schema(description = "Optional reason for the rejection, forwarded to the eligible party.")
        @Nullable
        String reason
) {}
