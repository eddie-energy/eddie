package energy.eddie.regionconnector.de.eta.dtos;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for creating a new permission request for the German (DE) ETA Plus region connector.
 */
public record PermissionRequestForCreation(
        @NotBlank String connectionId,
        @NotBlank String dataNeedId,
        @NotBlank String meteringPointId
) { }
