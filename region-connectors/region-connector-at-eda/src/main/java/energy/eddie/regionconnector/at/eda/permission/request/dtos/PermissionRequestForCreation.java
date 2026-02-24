// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.permission.request.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

import static energy.eddie.regionconnector.at.eda.requests.DsoIdAndMeteringPoint.DSO_ID_LENGTH;

public record PermissionRequestForCreation(
        @NotBlank
        String connectionId,
        @Size(
                min = 33,
                max = 33,
                message = "needs to be exactly 33 characters long"
        )
        String meteringPointId,
        @NotEmpty
        List<String> dataNeedIds,
        @Size(
                min = DSO_ID_LENGTH,
                max = DSO_ID_LENGTH,
                message = "needs to be exactly " + DSO_ID_LENGTH + " characters long"
        )
        @NotBlank
        String dsoId
) {}
