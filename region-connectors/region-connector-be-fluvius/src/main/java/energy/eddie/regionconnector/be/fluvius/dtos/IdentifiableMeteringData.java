package energy.eddie.regionconnector.be.fluvius.dtos;

import energy.eddie.api.agnostic.IdentifiablePayload;
import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.regionconnector.be.fluvius.client.model.GetEnergyResponseModelApiDataResponse;

public record IdentifiableMeteringData(
        PermissionRequest permissionRequest,
        GetEnergyResponseModelApiDataResponse payload
) implements IdentifiablePayload<PermissionRequest, GetEnergyResponseModelApiDataResponse> {
}
