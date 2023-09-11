package energy.eddie.regionconnector.at.eda.permission.request.states;

import at.ebutilities.schemata.customerconsent.cmrequest._01p10.CMRequest;
import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.states.CreatedPermissionRequestState;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import energy.eddie.regionconnector.at.eda.requests.InvalidDsoIdException;

/**
 * The first state a PermissionRequest is in.
 * After it is constructed the PermissionRequest is in a created state.
 */
public class AtCreatedPermissionRequestState
        extends ContextualizedPermissionRequestState<AtPermissionRequest>
        implements CreatedPermissionRequestState {
    private final CCMORequest ccmoRequest;
    private final EdaAdapter edaAdapter;

    public AtCreatedPermissionRequestState(AtPermissionRequest permissionRequest, CCMORequest ccmoRequest, EdaAdapter edaAdapter) {
        super(permissionRequest);
        this.ccmoRequest = ccmoRequest;
        this.edaAdapter = edaAdapter;
    }

    @Override
    public void validate() {
        try {
            CMRequest cmRequest = ccmoRequest.toCMRequest();
            permissionRequest.changeState(new AtValidatedPermissionRequestState(permissionRequest, cmRequest, edaAdapter));
        } catch (InvalidDsoIdException e) {
            permissionRequest.changeState(new AtMalformedPermissionRequestState(permissionRequest, e));
        }
    }

}
