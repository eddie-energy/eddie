package energy.eddie.regionconnector.at.eda.permission.request.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static energy.eddie.regionconnector.at.eda.requests.DsoIdAndMeteringPoint.DSO_ID_LENGTH;

public record PermissionRequestForCreation(
        @NotBlank(message = "must not be blank")
        String connectionId,
        @Size(
                min = 33,
                max = 33,
                message = "needs to be exactly 33 characters long"
        )
        String meteringPointId,
        @NotBlank(message = "must not be blank")
        String dataNeedId,
        @Size(
                min = DSO_ID_LENGTH,
                max = DSO_ID_LENGTH,
                message = "needs to be exactly " + DSO_ID_LENGTH + " characters long"
        )
        @NotBlank(message = "must not be blank")
        String dsoId
) {}
