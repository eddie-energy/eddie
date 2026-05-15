package energy.eddie.regionconnector.de.eta.providers;

import energy.eddie.api.agnostic.IdentifiablePayload;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;

public record IdentifiableAccountingPointData(
        DePermissionRequest permissionRequest,
        EtaPlusAccountingPointData payload
) implements IdentifiablePayload<DePermissionRequest, EtaPlusAccountingPointData> {
}
