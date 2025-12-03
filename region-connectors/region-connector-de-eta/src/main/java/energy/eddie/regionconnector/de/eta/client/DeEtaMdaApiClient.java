package energy.eddie.regionconnector.de.eta.client;

import energy.eddie.regionconnector.de.eta.permission.requests.DeEtaPermissionRequest;
import reactor.core.publisher.Mono;

/**
 * Minimal ETA+/MDA API client abstraction for DE-ETA.
 * Implementations should request master data and historical data required by OTA/MDA spec.
 */
public interface DeEtaMdaApiClient {

    Mono<ValidatedHistoricalDataResponse> fetchValidatedHistoricalData(DeEtaPermissionRequest permissionRequest);

    Mono<AccountingPointDataResponse> fetchAccountingPointData(DeEtaPermissionRequest permissionRequest);

    /** Placeholder payloads to carry raw data. Replace with generated/openapi models when available. */
    record ValidatedHistoricalDataResponse(Object rawPayload) { }

    record AccountingPointDataResponse(Object rawPayload) { }
}
