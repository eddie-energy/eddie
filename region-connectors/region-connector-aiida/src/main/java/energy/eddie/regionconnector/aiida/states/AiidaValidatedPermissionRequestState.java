package energy.eddie.regionconnector.aiida.states;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.states.ValidatedPermissionRequestState;
import energy.eddie.regionconnector.aiida.api.AiidaPermissionRequest;
import energy.eddie.regionconnector.aiida.services.AiidaRegionConnectorService;

public class AiidaValidatedPermissionRequestState
        extends ContextualizedPermissionRequestState<AiidaPermissionRequest>
        implements ValidatedPermissionRequestState {
    private final AiidaRegionConnectorService service;

    protected AiidaValidatedPermissionRequestState(AiidaPermissionRequest permissionRequest, AiidaRegionConnectorService service) {
        super(permissionRequest);
        this.service = service;
    }

    @Override
    public void sendToPermissionAdministrator() {
        service.sendToPermissionAdministrator(permissionRequest);
        // AIIDA region connector skips the PendingAcknowledgmentPermissionRequestState, as there is no acknowledgement from AIIDA
        permissionRequest.changeState(new AiidaSentToPermissionAdministratorPermissionRequestState(permissionRequest));
    }
}