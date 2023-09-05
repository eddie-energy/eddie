package energy.eddie.regionconnector.at.eda.permission.request.dtos;

import energy.eddie.regionconnector.at.api.AtPermissionRequest;

public record PermissionRequest(String permissionId, String cmRequestId) {
    public PermissionRequest(AtPermissionRequest request) {
        this(request.permissionId(), request.cmRequestId());
    }
}
