package energy.eddie.regionconnector.de.eta.dtos;

import jakarta.validation.constraints.NotBlank;

public record PermissionRequestForCreation(@NotBlank String dataNeedId, @NotBlank String connectionId) {
}
