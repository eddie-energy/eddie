package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.AcceptedPermissionRequestState;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fr.enedis.permission.request.StateBuilderFactory;

public class FrEnedisAcceptedState
        extends ContextualizedPermissionRequestState<FrEnedisPermissionRequest>
        implements AcceptedPermissionRequestState {

    public static final String NOT_IMPLEMENTED_YET = "Not implemented yet";
    private final StateBuilderFactory factory;

    public FrEnedisAcceptedState(FrEnedisPermissionRequest permissionRequest, StateBuilderFactory factory) {
        super(permissionRequest);
        this.factory = factory;
    }

    @Override
    public void terminate() {
        permissionRequest.changeState(
                factory.create(permissionRequest, PermissionProcessStatus.TERMINATED)
                        .build()
        );
    }

    @Override
    public void revoke() {
        permissionRequest.changeState(
                factory.create(permissionRequest, PermissionProcessStatus.REVOKED)
                        .build()
        );
    }

    @Override
    public void fulfill() {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_YET);
    }
}