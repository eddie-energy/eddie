package energy.eddie.regionconnector.dk.energinet.permission.request;

import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.shared.permission.requests.PermissionRequestProxy;
import energy.eddie.regionconnector.shared.permission.requests.extensions.Extension;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Component
public class PermissionRequestFactory {
    private final EnerginetCustomerApi customerApi;
    private final Set<Extension<DkEnerginetCustomerPermissionRequest>> extensions;
    private final StateBuilderFactory stateBuilderFactory;

    public PermissionRequestFactory(
            EnerginetCustomerApi customerApi,
            Set<Extension<DkEnerginetCustomerPermissionRequest>> extensions,
            StateBuilderFactory stateBuilderFactory
    ) {
        this.customerApi = customerApi;
        this.extensions = extensions;
        this.stateBuilderFactory = stateBuilderFactory;
    }

    public DkEnerginetCustomerPermissionRequest create(PermissionRequestForCreation request) {
        DkEnerginetCustomerPermissionRequest permissionRequest = new EnerginetCustomerPermissionRequest(
                UUID.randomUUID().toString(),
                request,
                customerApi,
                stateBuilderFactory);
        return PermissionRequestProxy.createProxy(
                permissionRequest,
                extensions,
                DkEnerginetCustomerPermissionRequest.class,
                PermissionRequestProxy.CreationInfo.NEWLY_CREATED
        );
    }

    public DkEnerginetCustomerPermissionRequest create(DkEnerginetCustomerPermissionRequest permissionRequest) {
        return PermissionRequestProxy.createProxy(
                permissionRequest.withApiClient(customerApi).withStateBuilderFactory(stateBuilderFactory),
                extensions,
                DkEnerginetCustomerPermissionRequest.class,
                PermissionRequestProxy.CreationInfo.RECREATED
        );
    }
}