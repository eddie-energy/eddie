package energy.eddie.aiida.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * This data transfer object is expected by the updatePermission API endpoint.
 *
 * @param operation The operation that should be applied to the permission.
 */
public record PatchPermissionDto(
        @Schema(description = "Operation to apply to the permission.", example = "REVOKE")
        @NotNull(message = "must not be null")
        PatchOperation operation
) {
}
