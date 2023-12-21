package energy.eddie.regionconnector.es.datadis.permission.request;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.Mvp1ConnectionStatusMessageProvider;
import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequestRepository;
import energy.eddie.regionconnector.shared.permission.requests.decorators.MessagingPermissionRequest;
import energy.eddie.regionconnector.shared.permission.requests.decorators.SavingPermissionRequest;
import org.springframework.stereotype.Component;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Sinks;

import java.util.UUID;
import java.util.concurrent.Flow;

@Component
public class PermissionRequestFactory implements Mvp1ConnectionStatusMessageProvider {
    private final AuthorizationApi authorizationApi;
    private final Sinks.Many<ConnectionStatusMessage> connectionStatusMessageSink;
    private final EsPermissionRequestRepository repository;

    public PermissionRequestFactory(
            AuthorizationApi authorizationApi,
            Sinks.Many<ConnectionStatusMessage> connectionStatusMessageSink,
            EsPermissionRequestRepository repository) {
        this.authorizationApi = authorizationApi;
        this.connectionStatusMessageSink = connectionStatusMessageSink;
        this.repository = repository;
    }

    public EsPermissionRequest create(PermissionRequestForCreation requestForCreation) {
        var permissionId = UUID.randomUUID().toString();
        var permissionRequest = new DatadisPermissionRequest(permissionId, requestForCreation, authorizationApi, repository);

        PermissionRequest messagingPermissionRequest = new MessagingPermissionRequest(permissionRequest, connectionStatusMessageSink);
        PermissionRequest savingPermissionRequest = new SavingPermissionRequest<>(
                new DatadisPermissionRequestAdapter(permissionRequest, messagingPermissionRequest),
                repository
        );
        return new DatadisPermissionRequestAdapter(
                permissionRequest,
                savingPermissionRequest
        );
    }

    @Override
    public Flow.Publisher<ConnectionStatusMessage> getConnectionStatusMessageStream() {
        return JdkFlowAdapter.publisherToFlowPublisher(connectionStatusMessageSink.asFlux());
    }

    @Override
    public void close() {
        connectionStatusMessageSink.tryEmitComplete();
    }
}
