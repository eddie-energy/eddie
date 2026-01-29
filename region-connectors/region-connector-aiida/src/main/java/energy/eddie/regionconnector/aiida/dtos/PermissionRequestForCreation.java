// SPDX-FileCopyrightText: 2023-2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.dtos;

import jakarta.validation.constraints.NotBlank;

public record PermissionRequestForCreation(@NotBlank(message = "must not be blank")
                                           String connectionId,
                                           @NotBlank(message = "must not be blank")
                                           String dataNeedId
) {
}
