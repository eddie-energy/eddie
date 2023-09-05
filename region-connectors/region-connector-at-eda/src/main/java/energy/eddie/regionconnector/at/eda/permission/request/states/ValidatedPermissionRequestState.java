package energy.eddie.regionconnector.at.eda.permission.request.states;

import at.ebutilities.schemata.customerconsent.cmrequest._01p10.CMRequest;
import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.FutureStateException;
import energy.eddie.api.v0.process.model.PastStateException;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.TransmissionException;
import jakarta.xml.bind.JAXBException;

public class ValidatedPermissionRequestState extends ContextualizedPermissionRequestState<AtPermissionRequest> {
    private final EdaAdapter edaAdapter;
    private final CMRequest cmRequest;


    protected ValidatedPermissionRequestState(AtPermissionRequest permissionRequest, CMRequest cmRequest, EdaAdapter edaAdapter) {
        super(permissionRequest);
        this.edaAdapter = edaAdapter;
        this.cmRequest = cmRequest;
    }

    @Override
    public void validate() throws PastStateException {
        throw new PastStateException(this);
    }

    @Override
    public void sendToPermissionAdministrator() {
        try {
            edaAdapter.sendCMRequest(cmRequest);
            permissionRequest.changeState(new PendingAcknowledgmentPermissionRequestState(permissionRequest));
        } catch (TransmissionException | JAXBException e) {
            permissionRequest.changeState(new UnableToSendPermissionRequestState(permissionRequest, e));
        }
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
