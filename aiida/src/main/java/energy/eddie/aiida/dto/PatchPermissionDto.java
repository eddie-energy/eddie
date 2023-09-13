package energy.eddie.aiida.dto;

import jakarta.validation.constraints.NotNull;

public record PatchPermissionDto(
        @NotNull(message = "operation mustn't be null")
        PatchOperation operation
) {
}
