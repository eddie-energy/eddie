package energy.eddie.regionconnector.shared.permission.requests.extensions;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.agnostic.process.model.PermissionRequestRepository;

public class SavingExtension<T extends PermissionRequest> implements Extension<T> {
    private final PermissionRequestRepository<T> permissionRequestRepository;

    public SavingExtension(PermissionRequestRepository<T> permissionRequestRepository) {
        this.permissionRequestRepository = permissionRequestRepository;
    }

    @Override
    public void accept(T permissionRequest) {
        permissionRequestRepository.save(permissionRequest);
    }
}