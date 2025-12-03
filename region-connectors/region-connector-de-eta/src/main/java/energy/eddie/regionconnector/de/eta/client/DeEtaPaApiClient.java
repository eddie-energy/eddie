package energy.eddie.regionconnector.de.eta.client;

import energy.eddie.regionconnector.de.eta.permission.requests.DeEtaPermissionRequest;
import reactor.core.publisher.Mono;

/**
 * Custom API client for sending DE-ETA permission requests to the Permission Administrator (PA).
 *
 * Implementations should perform the actual HTTP call and return a reactive Mono that
 * emits a {@link SendPermissionResponse} indicating success or failure.
 */
public interface DeEtaPaApiClient {

    Mono<SendPermissionResponse> sendPermissionRequest(DeEtaPermissionRequest request);

    /** Simple response DTO indicating whether the PA accepted the request. */
    record SendPermissionResponse(boolean success) { }
}
