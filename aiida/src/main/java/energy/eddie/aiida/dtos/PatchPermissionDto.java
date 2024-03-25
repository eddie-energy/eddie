package energy.eddie.aiida.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * This data transfer object is expected by the revoke permission API endpoint.
 *
 * @param operation The operation that should be applied to the permission. Currently only REVOKE_PERMISSION is applicable.
 */
public record PatchPermissionDto(
        @Schema(description = "Operation to apply to the permission.", example = "REVOKE_PERMISSION")
        @NotNull(message = "operation must not be null")
        PatchOperation operation
) {
}
