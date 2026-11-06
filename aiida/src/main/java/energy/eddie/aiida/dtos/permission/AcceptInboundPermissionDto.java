// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.dtos.permission;

import energy.eddie.aiida.models.permission.InboundMessageFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Accepts an inbound permission request from an AIIDA code. The data source of an inbound permission is created
 * automatically.
 *
 * @param eddieId              ID of the EDDIE framework that created the permission request, from the AIIDA code.
 * @param handshakeUrl         Handshake URL template from the AIIDA code.
 * @param accessToken          Access token from the AIIDA code.
 * @param inboundMessageFormat Optional format for inbound permission messages.
 * @param displayName          Optional display name, defaults to the name of the data need.
 */
public record AcceptInboundPermissionDto(
        @Schema(description = "ID of the EDDIE framework, from the AIIDA code.")
        @NotNull(message = "must not be null")
        UUID eddieId,

        @Schema(description = "Handshake URL template, from the AIIDA code.")
        @NotBlank(message = "must not be blank")
        String handshakeUrl,

        @Schema(description = "Access token, from the AIIDA code.")
        @NotBlank(message = "must not be blank")
        String accessToken,

        @Schema(description = "Selected format for inbound permission messages.", example = "OPENADR_3_1")
        @Nullable
        InboundMessageFormat inboundMessageFormat,

        @Schema(description = "Display name to assign to the permission.", example = "My smart meter")
        @Nullable
        String displayName
) implements AcceptPermissionDto {}
