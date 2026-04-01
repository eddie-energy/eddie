// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record PermissionRequestForCreation(
        @NotBlank
        String connectionId,
        @NotEmpty
        @Valid
        Set<@NotBlank String> dataNeedIds,
        @NotBlank
        String nif,
        @NotBlank
        String meteringPointId
) {
}
