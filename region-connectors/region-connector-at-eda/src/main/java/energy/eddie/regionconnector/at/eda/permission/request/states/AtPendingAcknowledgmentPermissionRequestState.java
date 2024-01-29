package energy.eddie.regionconnector.at.eda.permission.request.states;


import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.PendingAcknowledgmentPermissionRequestState;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;

public class AtPendingAcknowledgmentPermissionRequestState
        extends ContextualizedPermissionRequestState<AtPermissionRequest>
        implements PendingAcknowledgmentPermissionRequestState {

    private final EdaAdapter edaAdapter;
    private final AtConfiguration atConfiguration;

    public AtPendingAcknowledgmentPermissionRequestState(AtPermissionRequest permissionRequest,
                                                         EdaAdapter edaAdapter,
                                                         AtConfiguration atConfiguration) {
        super(permissionRequest);
        this.edaAdapter = edaAdapter;
        this.atConfiguration = atConfiguration;
    }

    @Override
    public void receivedPermissionAdministratorResponse() {
        permissionRequest.changeState(
                new AtSentToPermissionAdministratorPermissionRequestState(
                        permissionRequest,
                        edaAdapter,
                        atConfiguration)
        );
    }

}