// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.dtos;

import jakarta.validation.constraints.NotBlank;

public record PermissionRequestForCreation(@NotBlank long cdsId, @NotBlank String dataNeedId, @NotBlank String connectionId) {
}
