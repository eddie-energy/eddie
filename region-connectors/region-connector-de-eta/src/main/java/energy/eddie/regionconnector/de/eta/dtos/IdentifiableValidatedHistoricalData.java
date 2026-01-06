package energy.eddie.regionconnector.de.eta.dtos;

import energy.eddie.api.agnostic.IdentifiablePayload;
import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;

/**
 * Record that associates validated historical data from the ETA Plus API with a permission request.
 * This allows the same API response to be converted to both raw data messages and CIM documents.
 *
 * @param permissionRequest The permission request that triggered this data retrieval
 * @param payload The metered data response from the ETA Plus API
 */
public record IdentifiableValidatedHistoricalData(
        DePermissionRequest permissionRequest,
        EtaMeteringDataPayload payload
) implements IdentifiablePayload<PermissionRequest, EtaMeteringDataPayload> {
}

