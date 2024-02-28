package energy.eddie.regionconnector.fr.enedis.permission.request.dtos;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.shared.validation.SupportedGranularities;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.ZonedDateTime;

public record PermissionRequestForCreation(
        @NotBlank(message = "must not be blank")
        String connectionId,
        @NotBlank(message = "must not be blank")
        String dataNeedId,
        @NotNull(message = "must not be null")
        ZonedDateTime start,
        @NotNull(message = "must not be null")
        ZonedDateTime end,
        @SupportedGranularities({Granularity.P1D, Granularity.PT30M})
        Granularity granularity
) {
}