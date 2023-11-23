package energy.eddie.regionconnector.at.eda.permission.request.extensions;

import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;

public class SavingExtension implements Extension<AtPermissionRequest> {
    private final AtPermissionRequestRepository permissionRequestRepository;

    public SavingExtension(AtPermissionRequestRepository permissionRequestRepository) {
        this.permissionRequestRepository = permissionRequestRepository;
    }

    @Override
    public void accept(AtPermissionRequest permissionRequest) {
        permissionRequestRepository.save(permissionRequest);
    }

}
