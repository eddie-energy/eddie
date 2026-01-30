package energy.eddie.regionconnector.de.eta.dtos;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for creating a new permission request for the German (DE) ETA Plus region connector.
 */
public record PermissionRequestForCreation(
        @NotBlank(message = "must not be blank")
        String connectionId,
        @NotBlank(message = "must not be blank")
        String dataNeedId,
        @NotBlank(message = "must not be blank")
        String meteringPointId
) { }
