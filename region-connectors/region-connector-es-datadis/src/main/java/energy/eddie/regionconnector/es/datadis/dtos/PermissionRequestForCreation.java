package energy.eddie.regionconnector.es.datadis.dtos;

import jakarta.validation.constraints.NotBlank;

public record PermissionRequestForCreation(
        @NotBlank(message = "must not be null or blank")
        String connectionId,
        @NotBlank(message = "must not be null or blank")
        String dataNeedId,
        @NotBlank(message = "must not be null or blank")
        String nif,
        @NotBlank(message = "must not be null or blank")
        String meteringPointId
) {
}
