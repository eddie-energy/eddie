package energy.eddie.regionconnector.shared.services;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.v0.PermissionProcessStatus;

import java.util.List;

public interface CommonPermissionRequestRepository  {

    List<? extends PermissionRequest> findByStatus(PermissionProcessStatus status);

}
