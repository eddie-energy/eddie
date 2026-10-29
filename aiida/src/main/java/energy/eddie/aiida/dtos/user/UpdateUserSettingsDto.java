// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.dtos.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Email;

/**
 * Request body to update the settings of the current user.
 */
public record UpdateUserSettingsDto(
        @Nullable
        @Email(message = "must be a well-formed email address")
        @JsonProperty
        @Schema(description = "Contact email for connection limit notifications. An empty value disables notifications.", example = "user@example.com")
        String contactEmail
) {
}
