package energy.eddie.regionconnector.dk.energinet.dtos;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.shared.utils.StartOfDayZonedDateTimeDeserializer;
import energy.eddie.regionconnector.shared.validation.SupportedGranularities;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.ZonedDateTime;

public record PermissionRequestForCreation(
        @NotBlank(message = "connectionId must not be blank")
        String connectionId,
        @JsonDeserialize(using = StartOfDayZonedDateTimeDeserializer.class)
        @NotNull(message = "start must not be null")
        ZonedDateTime start,
        @JsonDeserialize(using = StartOfDayZonedDateTimeDeserializer.class)
        @NotNull(message = "end must not be null")
        ZonedDateTime end,
        @NotBlank(message = "refreshToken must not be blank")
        String refreshToken,
        @SupportedGranularities({
                Granularity.PT15M,
                Granularity.PT1H,
                Granularity.P1D,
                Granularity.P1M,
                Granularity.P1Y
        })
        @NotNull(message = "granularity must not be null")
        Granularity granularity,
        @NotBlank(message = "meteringPoint must not be blank")
        String meteringPoint,
        @NotBlank(message = "dataNeedId must not be blank")
        String dataNeedId
) {
}