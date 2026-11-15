// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic.aiida;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Sent by AIIDA to the EDDIE framework when the user accepted a permission request.
 *
 * @param aiidaId Unique ID of the AIIDA instance accepting the permission request.
 */
public record AiidaPermissionAcceptDto(@JsonProperty @NotNull UUID aiidaId) {}
