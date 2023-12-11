package energy.eddie.regionconnector.es.datadis.permission.request;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationResponseHandler;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequestRepository;
import energy.eddie.regionconnector.shared.permission.requests.decorators.MessagingPermissionRequest;
import energy.eddie.regionconnector.shared.permission.requests.decorators.SavingPermissionRequest;
import reactor.core.publisher.Sinks;

import java.util.UUID;

public class PermissionRequestFactory {
    private final AuthorizationApi authorizationApi;
    private final Sinks.Many<ConnectionStatusMessage> connectionStatusMessageSink;
    private final EsPermissionRequestRepository permissionRequestRepository;

    public PermissionRequestFactory(
            AuthorizationApi authorizationApi,
            Sinks.Many<ConnectionStatusMessage> connectionStatusMessageSink,
            EsPermissionRequestRepository permissionRequestRepository) {
        this.authorizationApi = authorizationApi;
        this.connectionStatusMessageSink = connectionStatusMessageSink;
        this.permissionRequestRepository = permissionRequestRepository;
    }

    public EsPermissionRequest create(PermissionRequestForCreation requestForCreation,
                                      AuthorizationResponseHandler authorizationResponseHandler) {
        var permissionId = UUID.randomUUID().toString();
        var permissionRequest = new DatadisPermissionRequest(permissionId, requestForCreation.connectionId(),
                requestForCreation.dataNeedId(), requestForCreation.nif(),
                requestForCreation.meteringPointId(), requestForCreation.measurementType(),
                requestForCreation.requestDataFrom(), requestForCreation.requestDataTo(),
                authorizationApi, authorizationResponseHandler);

        PermissionRequest messagingPermissionRequest = new MessagingPermissionRequest(permissionRequest, connectionStatusMessageSink);
        PermissionRequest savingPermissionRequest = new SavingPermissionRequest<>(
                // TODO why is adapter used here?
                new DatadisPermissionRequestAdapter(permissionRequest, messagingPermissionRequest),
                permissionRequestRepository
        );
        return new DatadisPermissionRequestAdapter(
                permissionRequest,
                savingPermissionRequest
        );
    }
}
