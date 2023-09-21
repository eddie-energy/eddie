package energy.eddie.regionconnector.es.datadis.permission.request.state;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.states.PendingAcknowledgmentPermissionRequestState;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;

public class PendingAcknowledgementState extends ContextualizedPermissionRequestState<EsPermissionRequest> implements PendingAcknowledgmentPermissionRequestState {

    protected PendingAcknowledgementState(EsPermissionRequest permissionRequest) {
        super(permissionRequest);
    }

    @Override
    public void receivedPermissionAdministratorResponse() {
        permissionRequest.changeState(
                new SentToPermissionAdministratorState(
                        permissionRequest
                )
        );
    }

}
