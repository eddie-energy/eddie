// SPDX-FileCopyrightText: 2023-2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PermissionRequestForCreation(@NotBlank(message = "must not be blank")
                                           String connectionId,
                                           @NotEmpty(message = "must not be empty")
                                           List<String> dataNeedIds
) {
}
