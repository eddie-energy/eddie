package energy.eddie.regionconnector.dk.energinet.dtos;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.shared.utils.StartOfDayZonedDateTimeDeserializer;
import energy.eddie.regionconnector.shared.validation.SupportedGranularities;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.ZonedDateTime;

public record PermissionRequestForCreation(
        @NotBlank(message = "must not be blank")
        String connectionId,
        @JsonDeserialize(using = StartOfDayZonedDateTimeDeserializer.class)
        @NotNull(message = "must not be null")
        ZonedDateTime start,
        @JsonDeserialize(using = StartOfDayZonedDateTimeDeserializer.class)
        @NotNull(message = "must not be null")
        ZonedDateTime end,
        @NotBlank(message = "must not be blank")
        String refreshToken,
        @SupportedGranularities({
                Granularity.PT15M,
                Granularity.PT1H,
                Granularity.P1D,
                Granularity.P1M,
                Granularity.P1Y
        })
        @NotNull(message = "must not be null")
        Granularity granularity,
        @NotBlank(message = "must not be blank")
        String meteringPoint,
        @NotBlank(message = "must not be blank")
        String dataNeedId
) {
}