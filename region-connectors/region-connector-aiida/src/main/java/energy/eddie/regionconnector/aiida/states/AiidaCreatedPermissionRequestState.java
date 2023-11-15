package energy.eddie.regionconnector.aiida.states;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.states.CreatedPermissionRequestState;
import energy.eddie.regionconnector.aiida.api.AiidaPermissionRequest;
import energy.eddie.regionconnector.aiida.services.AiidaRegionConnectorService;

public class AiidaCreatedPermissionRequestState
        extends ContextualizedPermissionRequestState<AiidaPermissionRequest>
        implements CreatedPermissionRequestState {
    private final AiidaRegionConnectorService service;

    public AiidaCreatedPermissionRequestState(AiidaPermissionRequest permissionRequest, AiidaRegionConnectorService service) {
        super(permissionRequest);
        this.service = service;
    }

    @Override
    public void validate() {
        // request already contains only valid values because controller validates input
        permissionRequest.changeState(new AiidaValidatedPermissionRequestState(permissionRequest, service));
    }
}