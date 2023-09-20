package energy.eddie.aiida.dtos;

import jakarta.validation.constraints.NotNull;

/**
 * This data transfer object is expected by the revoke permission API endpoint.
 *
 * @param operation The operation that should be applied to the permission. Currently only REVOKE_PERMISSION is applicable.
 */
public record PatchPermissionDto(
        @NotNull(message = "operation must not be null")
        PatchOperation operation
) {
}
