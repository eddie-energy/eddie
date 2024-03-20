package energy.eddie.regionconnector.dk.energinet.permission.request.api;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.permission.request.StateBuilderFactory;
import energy.eddie.regionconnector.shared.permission.requests.annotations.InvokeExtensions;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface DkEnerginetCustomerPermissionRequest extends TimeframedPermissionRequest {
    DkEnerginetCustomerPermissionRequest withApiClient(EnerginetCustomerApi client);

    DkEnerginetCustomerPermissionRequest withStateBuilderFactory(StateBuilderFactory factory);

    Mono<String> accessToken();

    Granularity granularity();

    String meteringPoint();

    LocalDate lastPolled();

    @InvokeExtensions
    void updateLastPolled(LocalDate lastPolled);
}
