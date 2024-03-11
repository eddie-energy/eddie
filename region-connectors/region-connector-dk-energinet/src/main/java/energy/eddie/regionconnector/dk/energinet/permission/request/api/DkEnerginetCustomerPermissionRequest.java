package energy.eddie.regionconnector.dk.energinet.permission.request.api;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.permission.request.StateBuilderFactory;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;

public interface DkEnerginetCustomerPermissionRequest extends TimeframedPermissionRequest {
    DkEnerginetCustomerPermissionRequest withApiClient(EnerginetCustomerApi client);

    DkEnerginetCustomerPermissionRequest withStateBuilderFactory(StateBuilderFactory factory);

    Mono<String> accessToken();

    Granularity granularity();

    String meteringPoint();

    ZonedDateTime lastPolled();

    PermissionProcessStatus status();

    void updateLastPolled(ZonedDateTime lastPolled);
}