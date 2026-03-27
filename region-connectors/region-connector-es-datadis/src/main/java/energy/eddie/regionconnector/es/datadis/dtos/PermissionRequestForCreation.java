// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.dtos;

import energy.eddie.regionconnector.es.datadis.dtos.validation.DataNeedCombinationConstraint;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.jspecify.annotations.Nullable;

import java.util.Set;

@DataNeedCombinationConstraint
public record PermissionRequestForCreation(
        @NotBlank
        String connectionId,
        @NotEmpty
        @Valid
        Set<@NotBlank String> dataNeedIds,
        @NotBlank
        String nif,
        @NotBlank
        String meteringPointId,
        @Nullable
        String firstname,
        @Nullable
        String surname
) {
        public PermissionRequestForCreation(
                String connectionId,
                Set<String> dataNeedIds,
                String nif,
                String meteringPointId
        ) {
                this(connectionId, dataNeedIds, nif, meteringPointId, null, null);
        }
}
