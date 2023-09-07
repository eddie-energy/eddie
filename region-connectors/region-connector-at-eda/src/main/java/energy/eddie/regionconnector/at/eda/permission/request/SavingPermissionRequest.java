package energy.eddie.regionconnector.at.eda.permission.request;

import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;

/**
 * This class will save a permission request everytime a potential state change has happened.
 */
public class SavingPermissionRequest
        extends energy.eddie.regionconnector.shared.permission.requests.decorators.SavingPermissionRequest<AtPermissionRequest>
        implements AtPermissionRequest {

    public SavingPermissionRequest(AtPermissionRequest permissionRequest, AtPermissionRequestRepository permissionRequestRepository) {
        super(permissionRequest, permissionRequestRepository);
    }

    @Override
    public String cmRequestId() {
        return permissionRequest.cmRequestId();
    }

    @Override
    public String conversationId() {
        return permissionRequest.conversationId();
    }
}
