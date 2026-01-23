// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.dtos;

import jakarta.annotation.Nullable;

import java.net.URI;

public record CreatedPermissionRequest(String permissionId, @Nullable URI redirectUri) {
}
