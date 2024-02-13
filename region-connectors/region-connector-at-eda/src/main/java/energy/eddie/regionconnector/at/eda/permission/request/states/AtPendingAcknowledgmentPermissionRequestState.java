package energy.eddie.regionconnector.at.eda.permission.request.states;


import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.PendingAcknowledgmentPermissionRequestState;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.StateBuilderFactory;

public class AtPendingAcknowledgmentPermissionRequestState
        extends ContextualizedPermissionRequestState<AtPermissionRequest>
        implements PendingAcknowledgmentPermissionRequestState {

    private final StateBuilderFactory factory;

    public AtPendingAcknowledgmentPermissionRequestState(AtPermissionRequest permissionRequest,
                                                         StateBuilderFactory factory) {
        super(permissionRequest);
        this.factory = factory;
    }

    @Override
    public void receivedPermissionAdministratorResponse() {
        permissionRequest.changeState(
                factory.create(permissionRequest, PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR)
                        .build()
        );
    }

}