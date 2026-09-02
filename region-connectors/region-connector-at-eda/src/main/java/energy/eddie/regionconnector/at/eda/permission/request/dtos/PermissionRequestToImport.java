// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.permission.request.dtos;

import energy.eddie.regionconnector.at.eda.dto.validation.MeterReadingDataNeedConstraint;
import energy.eddie.regionconnector.at.eda.dto.validation.MeterReadingOrderConstraint;
import energy.eddie.regionconnector.at.eda.dto.validation.MeterReadingPairConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.at.eda.requests.DsoIdAndMeteringPoint.DSO_ID_LENGTH;

@MeterReadingPairConstraint
@MeterReadingOrderConstraint
@MeterReadingDataNeedConstraint
public record PermissionRequestToImport(
        @NotBlank
        String connectionId,
        @NotNull
        @Size(
                min = 33,
                max = 33,
                message = "needs to be exactly 33 characters long"
        )
        String meteringPointId,
        @NotBlank
        String dataNeedId,
        @NotNull
        @Size(
                min = DSO_ID_LENGTH,
                max = DSO_ID_LENGTH,
                message = "needs to be exactly " + DSO_ID_LENGTH + " characters long"
        )
        String dsoId,
        @NotBlank
        String consentId,
        @NotNull
        @Past
        ZonedDateTime creationDateTime,
        @Nullable
        ZonedDateTime meterReadingStart,
        @Nullable
        ZonedDateTime meterReadingEnd
) {
    public PermissionRequestToImport(
            String connectionId,
            String meteringPointId,
            String dataNeedId,
            String dsoId,
            String consentId,
            ZonedDateTime creationDateTime
    ) {
        this(connectionId, meteringPointId, dataNeedId, dsoId, consentId, creationDateTime, null, null);
    }
}
