package energy.eddie.regionconnector.de.eta.providers.vhd;

import energy.eddie.api.agnostic.IdentifiablePayload;
import energy.eddie.regionconnector.de.eta.permission.requests.DeEtaPermissionRequest;

public record IdentifiableValidatedHistoricalData(
        DeEtaPermissionRequest permissionRequest,
        Object payload
) implements IdentifiablePayload<DeEtaPermissionRequest, Object> {
}
