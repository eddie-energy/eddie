package energy.eddie.regionconnector.dk.energinet.customer.permission.request;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequestRepository;
import energy.eddie.regionconnector.dk.energinet.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.shared.permission.requests.decorators.MessagingPermissionRequest;
import energy.eddie.regionconnector.shared.permission.requests.decorators.SavingPermissionRequest;
import reactor.core.publisher.Sinks;

import java.util.UUID;

public class PermissionRequestFactory {
    private final DkEnerginetCustomerPermissionRequestRepository permissionRequestRepository;
    private final Sinks.Many<ConnectionStatusMessage> connectionStatusSink;
    private final EnerginetConfiguration configuration;

    public PermissionRequestFactory(DkEnerginetCustomerPermissionRequestRepository permissionRequestRepository,
                                    Sinks.Many<ConnectionStatusMessage> connectionStatusSink,
                                    EnerginetConfiguration configuration) {
        this.permissionRequestRepository = permissionRequestRepository;
        this.connectionStatusSink = connectionStatusSink;
        this.configuration = configuration;
    }

    public DkEnerginetCustomerPermissionRequest create(PermissionRequestForCreation request) {
        var permissionId = UUID.randomUUID().toString();
        var permissionRequest = new EnerginetCustomerPermissionRequest(
                permissionId,
                request.connectionId(),
                request.start(),
                request.end(),
                request.refreshToken(),
                request.meteringPoint(),
                request.dataNeedId(),
                request.periodResolution(),
                configuration
        );

        PermissionRequest messagingPermissionRequest = new MessagingPermissionRequest(permissionRequest, connectionStatusSink);
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