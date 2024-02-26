package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.ValidatedPermissionRequestState;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fr.enedis.permission.request.StateBuilderFactory;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;

public class FrEnedisValidatedState
        extends ContextualizedPermissionRequestState<FrEnedisPermissionRequest>
        implements ValidatedPermissionRequestState {

    private final StateBuilderFactory factory;

    public FrEnedisValidatedState(FrEnedisPermissionRequest permissionRequest, StateBuilderFactory factory) {
        super(permissionRequest);
        this.factory = factory;
    }

    @Override
    public void sendToPermissionAdministrator() {
        permissionRequest.changeState(
                factory.create(permissionRequest, PermissionProcessStatus.PENDING_PERMISSION_ADMINISTRATOR_ACKNOWLEDGEMENT)
                        .build()
        );
    }
}