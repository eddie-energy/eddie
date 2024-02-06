package energy.eddie.regionconnector.dk.energinet.permission.request.api;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;

public interface DkEnerginetCustomerPermissionRequest extends TimeframedPermissionRequest {
    Mono<String> accessToken();

    Granularity granularity();

    String meteringPoint();

    ZonedDateTime lastPolled();

    void updateLastPolled(ZonedDateTime lastPolled);
}