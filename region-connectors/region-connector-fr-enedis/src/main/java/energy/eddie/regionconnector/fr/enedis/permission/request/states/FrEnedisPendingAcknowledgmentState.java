package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.PendingAcknowledgmentPermissionRequestState;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fr.enedis.permission.request.StateBuilderFactory;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;

public class FrEnedisPendingAcknowledgmentState extends ContextualizedPermissionRequestState<FrEnedisPermissionRequest> implements PendingAcknowledgmentPermissionRequestState {
    private final StateBuilderFactory factory;

    public FrEnedisPendingAcknowledgmentState(FrEnedisPermissionRequest permissionRequest, StateBuilderFactory factory) {
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