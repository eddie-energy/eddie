package energy.eddie.regionconnector.es.datadis.permission.request;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationResponseHandler;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequestRepository;
import energy.eddie.regionconnector.shared.permission.requests.decorators.MessagingPermissionRequest;
import energy.eddie.regionconnector.shared.permission.requests.decorators.SavingPermissionRequest;
import io.javalin.http.Context;
import reactor.core.publisher.Sinks;

import static java.util.Objects.requireNonNull;

public class PermissionRequestFactory {

    private final AuthorizationApi authorizationApi;
    private final Sinks.Many<ConnectionStatusMessage> permissionStateMessages;
    private final EsPermissionRequestRepository permissionRequestRepository;

    private final AuthorizationResponseHandler authorizationResponseHandler;


    public PermissionRequestFactory(AuthorizationApi authorizationApi, Sinks.Many<ConnectionStatusMessage> permissionStateMessages, EsPermissionRequestRepository permissionRequestRepository, AuthorizationResponseHandler authorizationResponseHandler) {
        requireNonNull(authorizationApi);
        requireNonNull(permissionStateMessages);
        requireNonNull(permissionRequestRepository);
        requireNonNull(authorizationResponseHandler);

        this.authorizationApi = authorizationApi;
        this.permissionStateMessages = permissionStateMessages;
        this.permissionRequestRepository = permissionRequestRepository;
        this.authorizationResponseHandler = authorizationResponseHandler;
    }

    public PermissionRequest create(Context ctx) {
        EsPermissionRequest permissionRequest = new DatadisPermissionRequest(
                ctx,
                authorizationApi,
                authorizationResponseHandler);
        PermissionRequest messagingPermissionRequest = new MessagingPermissionRequest(permissionRequest, permissionStateMessages);
        PermissionRequest savingPermissionRequest = new SavingPermissionRequest<>(
                new DatadisPermissionRequestAdapter(permissionRequest, messagingPermissionRequest),
                permissionRequestRepository
        );
        return new DatadisPermissionRequestAdapter(
                permissionRequest,
                savingPermissionRequest
        );
    }


}
