package energy.eddie.regionconnector.dk.energinet.customer.permission.request;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.api.v0.process.model.PermissionRequestRepository;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.api.DkEnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.shared.permission.requests.decorators.MessagingPermissionRequest;
import energy.eddie.regionconnector.shared.permission.requests.decorators.SavingPermissionRequest;
import io.javalin.http.Context;
import reactor.core.publisher.Sinks;

public class PermissionRequestFactory {
    private final PermissionRequestRepository<DkEnerginetCustomerPermissionRequest> permissionRequestRepository;
    private final Sinks.Many<ConnectionStatusMessage> connectionStatusSink;
    private final EnerginetConfiguration configuration;

    public PermissionRequestFactory(PermissionRequestRepository<DkEnerginetCustomerPermissionRequest> permissionRequestRepository,
                                    Sinks.Many<ConnectionStatusMessage> connectionStatusSink,
                                    EnerginetConfiguration configuration) {
        this.permissionRequestRepository = permissionRequestRepository;
        this.connectionStatusSink = connectionStatusSink;
        this.configuration = configuration;
    }

    public DkEnerginetCustomerPermissionRequest create(Context ctx) {
        DkEnerginetCustomerPermissionRequest permissionRequest = new EnerginetCustomerPermissionRequest(ctx, configuration);
        PermissionRequest messagingPermissionRequest = new MessagingPermissionRequest<>(permissionRequest, connectionStatusSink);
        PermissionRequest savingPermissionRequest = new SavingPermissionRequest<>(
                new DkEnerginetCustomerPermissionRequestAdapter(permissionRequest, messagingPermissionRequest),
                permissionRequestRepository
        );
        return new DkEnerginetCustomerPermissionRequestAdapter(
                permissionRequest,
                savingPermissionRequest
        );
    }
}
