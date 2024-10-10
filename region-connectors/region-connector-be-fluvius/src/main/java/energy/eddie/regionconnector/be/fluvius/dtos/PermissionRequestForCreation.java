package energy.eddie.regionconnector.be.fluvius.dtos;

import energy.eddie.regionconnector.be.fluvius.permission.request.Flow;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PermissionRequestForCreation(
        @NotBlank(message = "must not be blank")
        String connectionId,
        @NotBlank(message = "must not be blank")
        String dataNeedId,
        @NotNull
        Flow flow
) {
}