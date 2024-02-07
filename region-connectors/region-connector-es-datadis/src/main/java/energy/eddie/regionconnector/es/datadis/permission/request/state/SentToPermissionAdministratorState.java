package energy.eddie.regionconnector.es.datadis.permission.request.state;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.PastStateException;
import energy.eddie.api.agnostic.process.model.states.SentToPermissionAdministratorPermissionRequestState;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;

public class SentToPermissionAdministratorState extends ContextualizedPermissionRequestState<EsPermissionRequest> implements SentToPermissionAdministratorPermissionRequestState {


    protected SentToPermissionAdministratorState(EsPermissionRequest permissionRequest) {
        super(permissionRequest);
    }

    @Override
    public void accept() {
        permissionRequest.changeState(new AcceptedState(permissionRequest));
    }

    @Override
    public void invalid() throws PastStateException {
        // in datadis, invalid is handled by the PendingAcknowledgementState
        throw new PastStateException(this);
    }

    @Override
    public void reject() {
        permissionRequest.changeState(new RejectedState(permissionRequest));
    }

    @Override
    public void timeOut() {
        permissionRequest.changeState(new TimedOutState(permissionRequest));
    }
}