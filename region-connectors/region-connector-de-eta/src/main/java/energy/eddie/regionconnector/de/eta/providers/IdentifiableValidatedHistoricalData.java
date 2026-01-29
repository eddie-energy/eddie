package energy.eddie.regionconnector.de.eta.providers;

import energy.eddie.api.agnostic.IdentifiablePayload;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;

public record IdentifiableValidatedHistoricalData(
        DePermissionRequest permissionRequest,
        EtaPlusMeteredData payload
) implements IdentifiablePayload<DePermissionRequest, EtaPlusMeteredData> {
}
