package energy.eddie.regionconnector.at.eda.permission.request.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

import static energy.eddie.regionconnector.at.eda.requests.DsoIdAndMeteringPoint.DSO_ID_LENGTH;

public record PermissionRequestForCreation(
        @NotBlank(message = "ConnectionId must not be empty")
        String connectionId,
        @Size(
                min = 33,
                max = 33,
                message = "MeteringPoint needs to be exactly 33 characters long"
        )
        String meteringPointId,
        @NotBlank(message = "DataNeedId must not be empty")
        String dataNeedId,
        @Size(
                min = DSO_ID_LENGTH,
                max = DSO_ID_LENGTH,
                message = "dsoId must be " + DSO_ID_LENGTH + " characters long"
        )
        String dsoId,
        LocalDate start,
        LocalDate end
) {
}
