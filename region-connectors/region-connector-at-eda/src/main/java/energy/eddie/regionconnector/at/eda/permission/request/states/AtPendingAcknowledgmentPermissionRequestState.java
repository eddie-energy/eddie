package energy.eddie.regionconnector.at.eda.permission.request.states;


import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.PendingAcknowledgmentPermissionRequestState;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;

public class AtPendingAcknowledgmentPermissionRequestState
        extends ContextualizedPermissionRequestState<AtPermissionRequest>
        implements PendingAcknowledgmentPermissionRequestState {

    public AtPendingAcknowledgmentPermissionRequestState(AtPermissionRequest permissionRequest) {
        super(permissionRequest);
    }

    @Override
    public void receivedPermissionAdministratorResponse() {
        permissionRequest.changeState(
                new AtSentToPermissionAdministratorPermissionRequestState(
                        permissionRequest
                )
        );
    }

}