package energy.eddie.regionconnector.at.eda.permission.request.dtos;

public record PermissionRequest(String permissionId, String cmRequestId) {
    public PermissionRequest(energy.eddie.regionconnector.at.api.PermissionRequest request) {
        this(request.permissionId(), request.cmRequestId());
    }
}
