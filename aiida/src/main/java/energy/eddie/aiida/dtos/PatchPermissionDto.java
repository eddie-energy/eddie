// SPDX-FileCopyrightText: 2023-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * This data transfer object is expected by the updatePermission API endpoint.
 *
 * @param operation The operation that should be applied to the permission.
 * @param dataSourceId The data source assigned to the permission.
 */
public record PatchPermissionDto(
        @Schema(description = "Operation to apply to the permission.", example = "REVOKE")
        @NotNull(message = "must not be null")
        PatchOperation operation,

        @Schema(description = "Data source assigned to the permission.")
        @Nullable
        UUID dataSourceId
) {
}
