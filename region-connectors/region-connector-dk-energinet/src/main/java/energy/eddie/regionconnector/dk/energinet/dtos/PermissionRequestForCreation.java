package energy.eddie.regionconnector.dk.energinet.dtos;

import jakarta.validation.constraints.NotBlank;

public record PermissionRequestForCreation(
        @NotBlank(message = "must not be blank")
        String connectionId,
        @NotBlank(message = "must not be blank")
        String refreshToken,
        @NotBlank(message = "must not be blank")
        String meteringPoint,
        @NotBlank(message = "must not be blank")
        String dataNeedId
) {}
