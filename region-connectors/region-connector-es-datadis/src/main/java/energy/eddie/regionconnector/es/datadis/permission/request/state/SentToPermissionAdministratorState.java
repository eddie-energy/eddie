package energy.eddie.regionconnector.es.datadis.permission.request.state;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.PastStateException;
import energy.eddie.api.agnostic.process.model.states.SentToPermissionAdministratorPermissionRequestState;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.permission.request.StateBuilderFactory;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;

public class SentToPermissionAdministratorState extends ContextualizedPermissionRequestState<EsPermissionRequest> implements SentToPermissionAdministratorPermissionRequestState {
    private final StateBuilderFactory factory;

    public SentToPermissionAdministratorState(EsPermissionRequest permissionRequest, StateBuilderFactory factory) {
        super(permissionRequest);
        this.factory = factory;
    }

    @Override
    public void accept() {
        permissionRequest.changeState(
                factory.create(permissionRequest, PermissionProcessStatus.ACCEPTED)
                        .build()
        );
    }

    @Override
    public void invalid() throws PastStateException {
        // in datadis, invalid is handled by the PendingAcknowledgementState
        throw new PastStateException(this);
    }

    @Override
    public void reject() {
        permissionRequest.changeState(
                factory.create(permissionRequest, PermissionProcessStatus.REJECTED)
                        .build()
        );
    }

    @Override
    public void timeOut() {
        permissionRequest.changeState(
                factory.create(permissionRequest, PermissionProcessStatus.TIMED_OUT)
                        .build()
        );
    }
}