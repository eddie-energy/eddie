// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.dto;

import energy.eddie.regionconnector.at.api.AtPermissionRequest;

public record IdentifiableMasterData(
        EdaMasterData masterData,
        AtPermissionRequest permissionRequest
) {
}
