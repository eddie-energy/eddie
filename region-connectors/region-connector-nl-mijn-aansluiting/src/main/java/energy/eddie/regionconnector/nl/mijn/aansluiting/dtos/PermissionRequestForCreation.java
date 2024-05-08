package energy.eddie.regionconnector.nl.mijn.aansluiting.dtos;

import jakarta.validation.constraints.NotBlank;

public record PermissionRequestForCreation(
        @NotBlank(message = "must not be blank")
        String connectionId,
        @NotBlank(message = "must not be blank")
        String dataNeedId,
        @NotBlank(message = "must not be blank")
        String verificationCode

) {

}
