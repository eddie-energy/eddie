package energy.eddie.regionconnector.es.datadis.permission.request;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.Mvp1ConnectionStatusMessageProvider;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.shared.permission.requests.PermissionRequestProxy;
import energy.eddie.regionconnector.shared.permission.requests.extensions.Extension;
import org.springframework.stereotype.Component;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Sinks;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Flow;

@Component
public class PermissionRequestFactory implements Mvp1ConnectionStatusMessageProvider {
    private final AuthorizationApi authorizationApi;
    private final Sinks.Many<ConnectionStatusMessage> connectionStatusMessageSink;
    private final Set<Extension<EsPermissionRequest>> extensions;

    public PermissionRequestFactory(
            AuthorizationApi authorizationApi,
            Sinks.Many<ConnectionStatusMessage> connectionStatusMessageSink,
            Set<Extension<EsPermissionRequest>> extensions) {
        this.authorizationApi = authorizationApi;
        this.connectionStatusMessageSink = connectionStatusMessageSink;
        this.extensions = extensions;
    }

    public EsPermissionRequest create(PermissionRequestForCreation requestForCreation) {
        var permissionId = UUID.randomUUID().toString();
        var permissionRequest = new DatadisPermissionRequest(permissionId, requestForCreation, authorizationApi);

        return PermissionRequestProxy.createProxy(
                permissionRequest,
                extensions,
                EsPermissionRequest.class,
                PermissionRequestProxy.CreationInfo.NEWLY_CREATED
        );
    }

    public EsPermissionRequest create(EsPermissionRequest permissionRequest) {
        return PermissionRequestProxy.createProxy(
                permissionRequest,
                extensions,
                EsPermissionRequest.class,
                PermissionRequestProxy.CreationInfo.RECREATED
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