package energy.eddie.regionconnector.at.eda.permission.request.states;

import at.ebutilities.schemata.customerconsent.cmrequest._01p10.CMRequest;
import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.states.ValidatedPermissionRequestState;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.TransmissionException;
import jakarta.xml.bind.JAXBException;

public class AtValidatedPermissionRequestState extends ContextualizedPermissionRequestState<AtPermissionRequest> implements ValidatedPermissionRequestState {
    private final EdaAdapter edaAdapter;
    private final CMRequest cmRequest;


    protected AtValidatedPermissionRequestState(AtPermissionRequest permissionRequest, CMRequest cmRequest, EdaAdapter edaAdapter) {
        super(permissionRequest);
        this.edaAdapter = edaAdapter;
        this.cmRequest = cmRequest;
    }

    @Override
    public void sendToPermissionAdministrator() {
        try {
            edaAdapter.sendCMRequest(cmRequest);
            permissionRequest.changeState(new AtPendingAcknowledgmentPermissionRequestState(permissionRequest));
        } catch (TransmissionException | JAXBException e) {
            permissionRequest.changeState(new AtUnableToSendPermissionRequestState(permissionRequest, e));
        }
    }

}
