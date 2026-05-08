// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.dtos;

import energy.eddie.api.agnostic.IdentifiablePayload;
import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.regionconnector.be.fluvius.client.model.v3.energy.GetEnergyResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.permission.request.FluviusPermissionRequest;

public record IdentifiableMeteringData(
        FluviusPermissionRequest permissionRequest,
        GetEnergyResponseModelApiDataResponse payload
) implements IdentifiablePayload<PermissionRequest, GetEnergyResponseModelApiDataResponse> {
}
