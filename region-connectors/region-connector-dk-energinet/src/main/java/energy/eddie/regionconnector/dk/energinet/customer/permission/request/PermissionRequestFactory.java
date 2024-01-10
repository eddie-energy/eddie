package energy.eddie.regionconnector.dk.energinet.customer.permission.request;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.Mvp1ConnectionStatusMessageProvider;
import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequestRepository;
import energy.eddie.regionconnector.dk.energinet.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.shared.permission.requests.decorators.MessagingPermissionRequest;
import energy.eddie.regionconnector.shared.permission.requests.decorators.SavingPermissionRequest;
import org.springframework.stereotype.Component;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Sinks;

import java.util.UUID;
import java.util.concurrent.Flow;

@Component
public class PermissionRequestFactory implements Mvp1ConnectionStatusMessageProvider {
    private final DkEnerginetCustomerPermissionRequestRepository permissionRequestRepository;
    private final Sinks.Many<ConnectionStatusMessage> connectionStatusSink;
    private final EnerginetCustomerApi customerApi;

    public PermissionRequestFactory(DkEnerginetCustomerPermissionRequestRepository permissionRequestRepository,
                                    Sinks.Many<ConnectionStatusMessage> connectionStatusSink,
                                    EnerginetCustomerApi customerApi) {
        this.permissionRequestRepository = permissionRequestRepository;
        this.connectionStatusSink = connectionStatusSink;
        this.customerApi = customerApi;
    }

    public DkEnerginetCustomerPermissionRequest create(PermissionRequestForCreation request) {
        var permissionId = UUID.randomUUID().toString();
        var permissionRequest = new EnerginetCustomerPermissionRequest(
                permissionId,
                request,
                customerApi
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

    @Override
    public Flow.Publisher<ConnectionStatusMessage> getConnectionStatusMessageStream() {
        return JdkFlowAdapter.publisherToFlowPublisher(connectionStatusSink.asFlux());
    }

    @Override
    public void close() {
        connectionStatusSink.tryEmitComplete();
    }
}