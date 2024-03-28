package energy.eddie.regionconnector.dk.energinet.permission.request.api;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.MeterReadingPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.permission.request.StateBuilderFactory;
import reactor.core.publisher.Mono;

public interface DkEnerginetCustomerPermissionRequest extends MeterReadingPermissionRequest {
    DkEnerginetCustomerPermissionRequest withApiClient(EnerginetCustomerApi client);

    DkEnerginetCustomerPermissionRequest withStateBuilderFactory(StateBuilderFactory factory);

    Mono<String> accessToken();

    Granularity granularity();

    String meteringPoint();
}
