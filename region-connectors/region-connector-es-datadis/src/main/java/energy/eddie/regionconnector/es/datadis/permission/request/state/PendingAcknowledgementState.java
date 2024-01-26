package energy.eddie.regionconnector.es.datadis.permission.request.state;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.PendingAcknowledgmentPermissionRequestState;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequestResponse;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;

public class PendingAcknowledgementState extends ContextualizedPermissionRequestState<EsPermissionRequest> implements PendingAcknowledgmentPermissionRequestState {

    private final AuthorizationRequestResponse response;

    protected PendingAcknowledgementState(EsPermissionRequest permissionRequest, AuthorizationRequestResponse response) {
        super(permissionRequest);
        this.response = response;
    }

    @Override
    public void receivedPermissionAdministratorResponse() {
        // For datadis, it makes sense to transition to the InvalidState here if the response is NO_NIF or NO_SUPPLIES
        // otherwise we would need to throw an exception and handle it in a more complex way
        permissionRequest.changeState(
                switch (response) {
                    case AuthorizationRequestResponse.NoNif ignored ->
                            new InvalidState(permissionRequest, new Throwable("Given NIF does not exist"));
                    case AuthorizationRequestResponse.NoPermission ignored ->
                            new InvalidState(permissionRequest, new Throwable("The given NIF has no permissions"));
                    case AuthorizationRequestResponse.Unknown ignored ->
                            new InvalidState(permissionRequest, new Throwable("Unknown response from datadis: " + response.originalResponse()));
                    case AuthorizationRequestResponse.Ok ignored ->
                            new SentToPermissionAdministratorState(permissionRequest);
                }
        );
    }
}