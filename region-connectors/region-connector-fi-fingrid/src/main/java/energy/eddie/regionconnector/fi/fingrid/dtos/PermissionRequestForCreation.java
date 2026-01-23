// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fi.fingrid.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PermissionRequestForCreation(
        @NotBlank(message = "must not be blank")
        String connectionId,
        @NotBlank
        @Size(
                min = 36,
                max = 36,
                message = "needs to be exactly 36 characters long"
        )
        String dataNeedId,
        @NotBlank(message = "must not be blank")
        @Size(max = 50)
        String customerIdentification
) {
}
