package energy.eddie.regionconnector.es.datadis.dtos;

import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
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
        ZonedDateTime requestDataFrom,
        ZonedDateTime requestDataTo,
        @NotNull(message = "measurementType must not be null")
        MeasurementType measurementType
) {
}
