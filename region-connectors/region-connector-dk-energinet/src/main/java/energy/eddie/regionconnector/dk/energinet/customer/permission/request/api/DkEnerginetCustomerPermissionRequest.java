package energy.eddie.regionconnector.dk.energinet.customer.permission.request.api;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;
import reactor.core.publisher.Mono;

public interface DkEnerginetCustomerPermissionRequest extends TimeframedPermissionRequest {
    Mono<String> accessToken();

    Granularity granularity();

    String meteringPoint();
}