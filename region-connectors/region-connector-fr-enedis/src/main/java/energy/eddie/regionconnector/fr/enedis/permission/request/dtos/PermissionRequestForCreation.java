package energy.eddie.regionconnector.fr.enedis.permission.request.dtos;

import jakarta.validation.constraints.NotBlank;

public record PermissionRequestForCreation(
        @NotBlank(message = "must not be blank")
        String connectionId,
        @NotBlank(message = "must not be blank")
        String dataNeedId
) {
}
