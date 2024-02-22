package energy.eddie.regionconnector.es.datadis.permission.request.state;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.PendingAcknowledgmentPermissionRequestState;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequestResponse;
import energy.eddie.regionconnector.es.datadis.permission.request.StateBuilderFactory;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;

public class PendingAcknowledgementState extends ContextualizedPermissionRequestState<EsPermissionRequest> implements PendingAcknowledgmentPermissionRequestState {
    private final AuthorizationRequestResponse response;
    private final StateBuilderFactory factory;

    public PendingAcknowledgementState(
            EsPermissionRequest permissionRequest,
            AuthorizationRequestResponse response,
            StateBuilderFactory factory
    ) {
        super(permissionRequest);
        this.response = response;
        this.factory = factory;
    }

    @Override
    public void receivedPermissionAdministratorResponse() {
        // For datadis, it makes sense to transition to the InvalidState here if the response is NO_NIF or NO_SUPPLIES
        // otherwise we would need to throw an exception and handle it in a more complex way
        permissionRequest.changeState(
                switch (response) {
                    case AuthorizationRequestResponse.NoNif ignored ->
                            factory.create(permissionRequest, PermissionProcessStatus.INVALID)
                                    .withCause(new Throwable("Given NIF does not exist"))
                                    .build();
                    case AuthorizationRequestResponse.NoPermission ignored ->
                            factory.create(permissionRequest, PermissionProcessStatus.INVALID)
                                    .withCause(new Throwable("The given NIF has no permissions"))
                                    .build();
                    case AuthorizationRequestResponse.Unknown ignored ->
                            factory.create(permissionRequest, PermissionProcessStatus.INVALID)
                                    .withCause(new Throwable("Unknown response from datadis: " + response.originalResponse()))
                                    .build();
                    case AuthorizationRequestResponse.Ok ignored ->
                            factory.create(permissionRequest, PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR)
                                    .build();
                }
        );
    }
}