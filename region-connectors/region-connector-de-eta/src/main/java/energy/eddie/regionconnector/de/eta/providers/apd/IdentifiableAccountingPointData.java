package energy.eddie.regionconnector.de.eta.providers.apd;

import energy.eddie.api.agnostic.IdentifiablePayload;
import energy.eddie.regionconnector.de.eta.permission.requests.DeEtaPermissionRequest;

/**
 * Wrapper class to associate the raw Accounting Point Data response with the
 * corresponding Permission Request.
 */
public record IdentifiableAccountingPointData(
        DeEtaPermissionRequest permissionRequest,
        Object payload
) implements IdentifiablePayload<DeEtaPermissionRequest, Object> {
}