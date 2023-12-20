package energy.eddie.regionconnector.fr.enedis.permission.request.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.ZonedDateTime;

public record PermissionRequestForCreation(
        @NotBlank(message = "connectionId must not be blank")
        String connectionId,
        @NotBlank(message = "dataNeedId must not be blank")
        String dataNeedId,
        @NotNull
        ZonedDateTime start,
        @NotNull
        ZonedDateTime end
) {
}
