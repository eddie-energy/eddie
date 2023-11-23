package energy.eddie.regionconnector.at.eda.permission.request.dtos;

import energy.eddie.regionconnector.at.api.AtPermissionRequest;

public record CreatedPermissionRequest(String permissionId, String cmRequestId) {
    public CreatedPermissionRequest(AtPermissionRequest request) {
        this(request.permissionId(), request.cmRequestId());
    }
}
