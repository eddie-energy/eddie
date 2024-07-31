package energy.eddie.regionconnector.us.green.button.permission.request.api;

import energy.eddie.api.agnostic.process.model.PermissionRequest;

import java.util.Optional;

public interface UsGreenButtonPermissionRequest extends PermissionRequest {
    Optional<String> scope();

    Optional<String> jumpOffUrl();
}
