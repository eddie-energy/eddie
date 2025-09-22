package energy.eddie.regionconnector.si.moj.elektro.dtos;

import jakarta.validation.constraints.NotBlank;

public record PermissionRequestForCreation(
        @NotBlank(message = "must not be blank")
        String connectionId,
        @NotBlank(message = "must not be blank")
        String apiToken,
        @NotBlank(message = "must not be blank")
        String meteringPoint,
        @NotBlank(message = "must not be blank")
        String dataNeedId
) { }
