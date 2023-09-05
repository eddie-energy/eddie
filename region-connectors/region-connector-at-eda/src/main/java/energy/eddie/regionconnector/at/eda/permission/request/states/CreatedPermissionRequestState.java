package energy.eddie.regionconnector.at.eda.permission.request.states;

import at.ebutilities.schemata.customerconsent.cmrequest._01p10.CMRequest;
import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.FutureStateException;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import energy.eddie.regionconnector.at.eda.requests.InvalidDsoIdException;

/**
 * The first state a PermissionRequest is in.
 * After it is constructed the PermissionRequest is in a created state.
 */
public class CreatedPermissionRequestState extends ContextualizedPermissionRequestState<AtPermissionRequest> {
    private final CCMORequest ccmoRequest;
    private final EdaAdapter edaAdapter;

    public CreatedPermissionRequestState(AtPermissionRequest permissionRequest, CCMORequest ccmoRequest, EdaAdapter edaAdapter) {
        super(permissionRequest);
        this.ccmoRequest = ccmoRequest;
        this.edaAdapter = edaAdapter;
    }

    @Override
    public void validate() {
        try {
            CMRequest cmRequest = ccmoRequest.toCMRequest();
            permissionRequest.changeState(new ValidatedPermissionRequestState(permissionRequest, cmRequest, edaAdapter));
        } catch (InvalidDsoIdException e) {
            permissionRequest.changeState(new MalformedPermissionRequestState(permissionRequest, e));
        }
    }

    @Override
    public void sendToPermissionAdministrator() throws FutureStateException {
        throw new FutureStateException(this);
    }

    @Override
    public void receivedPermissionAdministratorResponse() throws FutureStateException {
        throw new FutureStateException(this);
    }

    @Override
    public void accept() throws FutureStateException {
        throw new FutureStateException(this);
    }

    @Override
    public void invalid() throws FutureStateException {
        throw new FutureStateException(this);
    }

    @Override
    public void reject() throws FutureStateException {
        throw new FutureStateException(this);
    }

    @Override
    public void terminate() throws FutureStateException {
        throw new FutureStateException(this);
    }
}
