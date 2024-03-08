package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.SentToPermissionAdministratorPermissionRequestState;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fr.enedis.permission.request.StateBuilderFactory;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;

public class FrEnedisSentToPermissionAdministratorState
        extends ContextualizedPermissionRequestState<FrEnedisPermissionRequest>
        implements SentToPermissionAdministratorPermissionRequestState {
    private final StateBuilderFactory factory;

    public FrEnedisSentToPermissionAdministratorState(
            FrEnedisPermissionRequest permissionRequest,
            StateBuilderFactory factory
    ) {
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
    public void invalid() {
        permissionRequest.changeState(
                factory.create(permissionRequest, PermissionProcessStatus.INVALID)
                        .build()
        );
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