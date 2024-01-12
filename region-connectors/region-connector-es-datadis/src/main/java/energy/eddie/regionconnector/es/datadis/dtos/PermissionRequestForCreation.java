package energy.eddie.regionconnector.es.datadis.dtos;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.shared.utils.StartOfDayZonedDateTimeDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.ZonedDateTime;

public record PermissionRequestForCreation(
        @NotBlank(message = "connectionId must not be null or blank")
        String connectionId,
        @NotBlank(message = "dataNeedId must not be null or blank")
        String dataNeedId,
        @NotBlank(message = "nif must not be null or blank")
        String nif,
        @NotBlank(message = "meteringPointId must not be null or blank")
        String meteringPointId,
        @JsonDeserialize(using = StartOfDayZonedDateTimeDeserializer.class)
        @NotNull(message = "requestDataFrom must not be null")
        ZonedDateTime requestDataFrom,
        @JsonDeserialize(using = StartOfDayZonedDateTimeDeserializer.class)
        @NotNull(message = "requestDataTo must not be null")
        ZonedDateTime requestDataTo,
        @NotNull(message = "measurementType must not be null")
        MeasurementType measurementType
) {
}
