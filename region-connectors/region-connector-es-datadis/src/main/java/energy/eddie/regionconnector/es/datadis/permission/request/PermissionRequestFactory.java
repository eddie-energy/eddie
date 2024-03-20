package energy.eddie.regionconnector.es.datadis.permission.request;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.Mvp1ConnectionStatusMessageProvider;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.shared.permission.requests.PermissionRequestProxy;
import energy.eddie.regionconnector.shared.permission.requests.extensions.Extension;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Component
public class PermissionRequestFactory implements Mvp1ConnectionStatusMessageProvider {
    private final Sinks.Many<ConnectionStatusMessage> connectionStatusMessageSink;
    private final Set<Extension<EsPermissionRequest>> extensions;
    private final StateBuilderFactory stateBuilderFactory;

    public PermissionRequestFactory(
            Sinks.Many<ConnectionStatusMessage> connectionStatusMessageSink,
            Set<Extension<EsPermissionRequest>> extensions,
            StateBuilderFactory stateBuilderFactory
    ) {
        this.connectionStatusMessageSink = connectionStatusMessageSink;
        this.extensions = extensions;
        this.stateBuilderFactory = stateBuilderFactory;
    }

    public EsPermissionRequest create(PermissionRequestForCreation requestForCreation,
                                      LocalDate start,
                                      LocalDate end,
                                      Granularity granularity) {
        var permissionRequest = new DatadisPermissionRequest(
                UUID.randomUUID().toString(),
                requestForCreation,
                start,
                end,
                granularity,
                stateBuilderFactory
        );

        return PermissionRequestProxy.createProxy(
                permissionRequest,
                extensions,
                EsPermissionRequest.class,
                PermissionRequestProxy.CreationInfo.NEWLY_CREATED
        );
    }

    public EsPermissionRequest create(EsPermissionRequest permissionRequest) {
        return PermissionRequestProxy.createProxy(
                permissionRequest.withStateBuilderFactory(stateBuilderFactory),
                extensions,
                EsPermissionRequest.class,
                PermissionRequestProxy.CreationInfo.RECREATED
        );
    }

    @Override
    public Flux<ConnectionStatusMessage> getConnectionStatusMessageStream() {
        return connectionStatusMessageSink.asFlux();
    }

    @Override
    public void close() {
        connectionStatusMessageSink.tryEmitComplete();
    }
}