package energy.eddie.regionconnector.at.eda.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.SentToPermissionAdministratorPermissionRequestState;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;

public class AtSentToPermissionAdministratorPermissionRequestState
        extends ContextualizedPermissionRequestState<AtPermissionRequest>
        implements SentToPermissionAdministratorPermissionRequestState {
    private final EdaAdapter edaAdapter;
    private final AtConfiguration atConfiguration;

    public AtSentToPermissionAdministratorPermissionRequestState(AtPermissionRequest permissionRequest,
                                                                 EdaAdapter edaAdapter,
                                                                 AtConfiguration atConfiguration) {
        super(permissionRequest);
        this.edaAdapter = edaAdapter;
        this.atConfiguration = atConfiguration;
    }

    @Override
    public void accept() {
        permissionRequest.changeState(new AtAcceptedPermissionRequestState(permissionRequest, edaAdapter, atConfiguration));
    }

    @Override
    public void invalid() {
        permissionRequest.changeState(new AtInvalidPermissionRequestState(permissionRequest));
    }

    @Override
    public void reject() {
        permissionRequest.changeState(new AtRejectedPermissionRequestState(permissionRequest));
    }

    @Override
    public void timeOut() {
        throw new IllegalStateException("Not implemented yet");
    }
}