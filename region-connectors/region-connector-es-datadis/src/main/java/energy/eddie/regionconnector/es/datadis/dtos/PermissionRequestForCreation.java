package energy.eddie.regionconnector.es.datadis.dtos;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.shared.utils.StartOfDayZonedDateTimeDeserializer;
import energy.eddie.regionconnector.shared.validation.SupportedGranularities;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.ZonedDateTime;

public record PermissionRequestForCreation(
        @NotBlank(message = "must not be null or blank")
        String connectionId,
        @NotBlank(message = "must not be null or blank")
        String dataNeedId,
        @NotBlank(message = "must not be null or blank")
        String nif,
        @NotBlank(message = "must not be null or blank")
        String meteringPointId,
        @JsonDeserialize(using = StartOfDayZonedDateTimeDeserializer.class)
        @NotNull(message = "must not be null")
        ZonedDateTime requestDataFrom,
        @JsonDeserialize(using = StartOfDayZonedDateTimeDeserializer.class)
        @NotNull(message = "must not be null")
        ZonedDateTime requestDataTo,
        @NotNull(message = "must not be null")
        @SupportedGranularities({Granularity.PT1H, Granularity.PT15M})
        Granularity granularity
) {
}
