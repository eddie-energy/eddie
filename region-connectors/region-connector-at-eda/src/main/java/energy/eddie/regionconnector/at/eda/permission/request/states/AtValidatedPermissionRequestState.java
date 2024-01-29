package energy.eddie.regionconnector.at.eda.permission.request.states;

import at.ebutilities.schemata.customerconsent.cmrequest._01p10.CMRequest;
import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.ValidatedPermissionRequestState;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.TransmissionException;
import energy.eddie.regionconnector.at.eda.permission.request.StateBuilderFactory;
import jakarta.xml.bind.JAXBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AtValidatedPermissionRequestState extends ContextualizedPermissionRequestState<AtPermissionRequest> implements ValidatedPermissionRequestState {
    private static final Logger LOGGER = LoggerFactory.getLogger(AtValidatedPermissionRequestState.class);
    private final EdaAdapter edaAdapter;
    private final CMRequest cmRequest;
    private final StateBuilderFactory stateBuilderFactory;

    public AtValidatedPermissionRequestState(AtPermissionRequest permissionRequest,
                                             CMRequest cmRequest,
                                             EdaAdapter edaAdapter,
                                             StateBuilderFactory stateBuilderFactory) {
        super(permissionRequest);
        this.edaAdapter = edaAdapter;
        this.cmRequest = cmRequest;
        this.stateBuilderFactory = stateBuilderFactory;
    }

    @Override
    public void sendToPermissionAdministrator() {
        try {
            edaAdapter.sendCMRequest(cmRequest);
            permissionRequest.changeState(new AtPendingAcknowledgmentPermissionRequestState(permissionRequest, stateBuilderFactory));
        } catch (TransmissionException | JAXBException e) {
            LOGGER.error("Error sending CCMO request to DSO '{}'", permissionRequest.cmRequestId(), e);
            permissionRequest.changeState(new AtUnableToSendPermissionRequestState(permissionRequest, e));
        }
    }
}