package energy.eddie.regionconnector.at.eda.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.AcceptedPermissionRequestState;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.requests.CCMORevoke;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AtAcceptedPermissionRequestState
        extends ContextualizedPermissionRequestState<AtPermissionRequest>
        implements AcceptedPermissionRequestState {
    private static final Logger LOGGER = LoggerFactory.getLogger(AtAcceptedPermissionRequestState.class);
    private final EdaAdapter edaAdapter;
    private final AtConfiguration atConfiguration;

    public AtAcceptedPermissionRequestState(
            AtPermissionRequest permissionRequest,
            EdaAdapter edaAdapter,
            AtConfiguration atConfiguration
    ) {
        super(permissionRequest);
        this.edaAdapter = edaAdapter;
        this.atConfiguration = atConfiguration;
    }

    @Override
    public void terminate() {
        var revoke = new CCMORevoke(permissionRequest, atConfiguration.eligiblePartyId()).toCMRevoke();
        try {
            edaAdapter.sendCMRevoke(revoke);
            permissionRequest.changeState(new AtTerminatedPermissionRequestState(permissionRequest));
            permissionRequest.setStateTransitionMessage(revoke.getProcessDirectory().getReason());
        } catch (Exception e) {
            LOGGER.warn("Error trying to terminate permission request.", e);
        }
    }

    @Override
    public void revoke() {
        permissionRequest.changeState(new AtRevokedPermissionRequestState(permissionRequest));
    }

    @Override
    public void fulfill() {
        permissionRequest.changeState(new AtFulfilledPermissionRequestState(permissionRequest));
    }
}