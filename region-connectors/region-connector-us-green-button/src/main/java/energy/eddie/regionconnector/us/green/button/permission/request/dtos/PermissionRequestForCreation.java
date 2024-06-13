package energy.eddie.regionconnector.us.green.button.permission.request.dtos;

import jakarta.validation.constraints.NotBlank;

public record PermissionRequestForCreation(
        @NotBlank(message = "must not be blank")
        String connectionId,
        @NotBlank(message = "must not be blank")
        String dataNeedId,
        @NotBlank(message = "must not be blank")
        String jumpOffUrl,
        @NotBlank(message = "must not be blank")
        String companyId,
        @NotBlank(message = "must not be blank")
        String countryCode
) {
}
