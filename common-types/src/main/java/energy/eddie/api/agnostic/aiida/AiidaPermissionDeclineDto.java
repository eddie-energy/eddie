// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic.aiida;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Sent by AIIDA to the EDDIE framework when a permission request is not accepted, i.e. it was rejected by the user or
 * cannot be fulfilled by the AIIDA instance.
 *
 * @param aiidaId Unique ID of the AIIDA instance sending the update.
 * @param reason  Optional human-readable reason, which is forwarded to the eligible party.
 */
public record AiidaPermissionDeclineDto(
        @JsonProperty @NotNull UUID aiidaId,
        @JsonProperty @Nullable String reason
) {}
