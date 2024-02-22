package energy.eddie.regionconnector.es.datadis.permission.request.state;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.AcceptedPermissionRequestState;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.permission.request.StateBuilderFactory;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;

/**
 * The user has accepted the permission request
 */
public class AcceptedState
        extends ContextualizedPermissionRequestState<EsPermissionRequest>
        implements AcceptedPermissionRequestState {
    private final StateBuilderFactory factory;

    public AcceptedState(
            EsPermissionRequest permissionRequest,
            StateBuilderFactory factory
    ) {
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
        permissionRequest.changeState(
                factory.create(permissionRequest, PermissionProcessStatus.FULFILLED)
                        .build()
        );
    }
}